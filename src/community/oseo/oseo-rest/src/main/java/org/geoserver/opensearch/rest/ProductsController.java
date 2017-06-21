/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.opensearch.eo.DefaultOpenSearchEoService;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the OpenSearch products
 *
 * @author Andrea Aime - GeoSolutions
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/oseo/collections/{collection}/products")
public class ProductsController extends AbstractOpenSearchController {

    static final Name PRODUCT_ID = new NameImpl(
            OpenSearchAccess.ProductClass.EOP_GENERIC.getNamespace(), "identifier");

    static final Name PARENT_ID = new NameImpl(OpenSearchAccess.EO_NAMESPACE, "parentIdentifier");

    public ProductsController(

            OpenSearchAccessProvider accessProvider) {
        super(accessProvider);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public ProductReferences getProducts(HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "limit", required = false) Integer limit) throws IOException {
        // query products for their identifiers
        Query query = new Query();
        PropertyIsEqualTo parentIdFilter = FF.equal(FF.property(PARENT_ID), FF.literal(collection),
                true);
        query.setFilter(parentIdFilter);
        setupQueryPaging(query, offset, limit);
        query.setSortBy(new SortBy[] { FF.sort(PRODUCT_ID.getLocalPart(), SortOrder.ASCENDING) });
        query.setProperties(Collections.singletonList(FF.property(PRODUCT_ID)));
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureSource<FeatureType, Feature> fs = access.getProductSource();
        FeatureCollection<FeatureType, Feature> features = fs.getFeatures(query);

        // map to java beans for JSON encoding
        String baseURL = ResponseUtils.baseURL(request);
        List<ProductReference> list = new ArrayList<>();
        features.accepts(f -> {
            String id = (String) f.getProperty("identifier").getValue();
            String productHref = ResponseUtils.buildURL(baseURL,
                    "/rest/oseo/collections/" + collection + "/products/" + id, null,
                    URLType.RESOURCE);
            String oseoHref = ResponseUtils.buildURL(baseURL, "/oseo/search",
                    Collections.singletonMap("uid", id), URLType.RESOURCE);
            ProductReference pr = new ProductReference(id, productHref, oseoHref);
            list.add(pr);
        }, null);

        if (list.isEmpty()) {
            // suspicious, did the collection actually exist?
            queryCollection(collection, q -> {
            });
        }

        return new ProductReferences(list);
    }

    /*
     * Note, the .+ regular expression allows the product id to contain dots instead of having them interpreted as format extension
     */
    @GetMapping(path = "{product:.+}", produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public SimpleFeature getProduct(HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product) throws IOException {
        Feature feature = queryProduct(collection, product, q -> {
        });

        // map to the output schema for GeoJSON encoding
        SimpleFeatureType targetSchema = mapFeatureTypeToSimple(feature.getType(), ftb -> {
            ftb.add("ogcLinksHref", String.class);
            ftb.add("metadataHref", String.class);
            ftb.add("descriptionHref", String.class);
            ftb.add("thumbnailHref", String.class);
            ftb.add("granulesHref", String.class);
        });
        return mapFeatureToSimple(feature, targetSchema, fb -> {
            String baseURL = ResponseUtils.baseURL(request);
            String pathBase = "/rest/oseo/collections/" + collection + "/products/" + product + "/";
            String ogcLinks = ResponseUtils.buildURL(baseURL, pathBase + "ogcLinks", null,
                    URLType.RESOURCE);
            String metadata = ResponseUtils.buildURL(baseURL, pathBase + "metadata", null,
                    URLType.RESOURCE);
            String description = ResponseUtils.buildURL(baseURL, pathBase + "description", null,
                    URLType.RESOURCE);
            String thumb = ResponseUtils.buildURL(baseURL, pathBase + "thumbnail", null,
                    URLType.RESOURCE);
            String granules = ResponseUtils.buildURL(baseURL, pathBase + "granules", null,
                    URLType.RESOURCE);

            fb.set("ogcLinksHref", ogcLinks);
            fb.set("metadataHref", metadata);
            fb.set("descriptionHref", description);
            fb.set("thumbnailHref", thumb);
            fb.set("granulesHref", granules);
        });
    }

    /*
     * Note, the .+ regular expression allows the product id to contain dots instead of having them interpreted as format extension
     */
    @GetMapping(path = "{product:.+}/ogcLinks", produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public OgcLinks getProductOgcLinks(HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product) throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryProduct(collection, product, q -> {
            q.setProperties(Collections
                    .singletonList(FF.property(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME)));
        });

        OgcLinks links = buildOgcLinksFromFeature(feature);
        return links;
    }

    /*
     * Note, the .+ regular expression allows the product id to contain dots instead of having them interpreted as format extension
     */
    @GetMapping(path = "{product:.+}/metadata", produces = { MediaType.APPLICATION_JSON_VALUE })
    public void getCollectionMetadata(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            HttpServletResponse response) throws IOException {
        // query one product and grab its metadata
        Feature feature = queryProduct(collection, product, q -> {
            q.setProperties(Collections
                    .singletonList(FF.property(OpenSearchAccess.METADATA_PROPERTY_NAME)));
        });

        // grab the metadata
        Property metadataProperty = feature.getProperty(OpenSearchAccess.METADATA_PROPERTY_NAME);
        if (metadataProperty != null && metadataProperty.getValue() instanceof String) {
            String value = (String) metadataProperty.getValue();
            response.setContentType("text/xml");
            StreamUtils.copy(value, Charset.forName("UTF-8"), response.getOutputStream());
        } else {
            throwProductNotFound(collection, product, "Metadata for product");
        }
    }

    private void throwProductNotFound(String collection, String product, String item) {
        throw new ResourceNotFoundException(item + " '" + product
                + "' in collection '" + collection + "' could not be found");
    }

    /*
     * Note, the .+ regular expression allows the product id to contain dots instead of having them interpreted as format extension
     */
    @GetMapping(path = "{product:.+}/description", produces = { MediaType.TEXT_HTML_VALUE })
    public void getProductDescription(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            HttpServletResponse response) throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryProduct(collection, product, q -> {
            q.setPropertyNames(new String[] { "htmlDescription" });
        });

        // grab the description
        Property descriptionProperty = feature.getProperty("htmlDescription");
        if (descriptionProperty != null && descriptionProperty.getValue() instanceof String) {
            String value = (String) descriptionProperty.getValue();
            response.setContentType("text/html");
            StreamUtils.copy(value, Charset.forName("UTF-8"), response.getOutputStream());
        } else {
            throwProductNotFound(collection, product, "Product");
        }
    }
    

    /*
     * Note, the .+ regular expression allows the product id to contain dots instead of having them interpreted as format extension
     */
    @GetMapping(path = "{product:.+}/thumbnail", produces = { MediaType.ALL_VALUE })
    public void getProductThumbnail(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            HttpServletResponse response) throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryProduct(collection, product, q -> {
            q.setProperties(Collections.singletonList(FF.property(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME))); 
        });

        // grab the thumbnail
        Property thumbnailProperty = feature.getProperty(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME);
        if (thumbnailProperty != null && thumbnailProperty.getValue() instanceof byte[]) {
            byte[] value = (byte[]) thumbnailProperty.getValue();
            response.setContentType(DefaultOpenSearchEoService.guessImageMimeType(value));
            StreamUtils.copy(value, response.getOutputStream());
        } else {
            throwProductNotFound(collection, product, "Thumbnail for product");
        }
    }

    private Feature queryProduct(String collection, String product, Consumer<Query> queryDecorator)
            throws IOException {
        // query products
        Query query = new Query();
        PropertyIsEqualTo parentIdFilter = FF.equal(FF.property(PARENT_ID), FF.literal(collection),
                true);
        PropertyIsEqualTo idFilter = FF.equal(FF.property(PRODUCT_ID), FF.literal(product), true);
        query.setFilter(FF.and(parentIdFilter, idFilter));
        queryDecorator.accept(query);
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureSource<FeatureType, Feature> fs = access.getProductSource();
        FeatureCollection<FeatureType, Feature> features = fs.getFeatures(query);
        Feature feature = DataUtilities.first(features);

        if (feature == null) {
            throwProductNotFound(collection, product, "Product");
        }

        return feature;
    }

}

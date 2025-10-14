/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.geoserver.opensearch.eo.DefaultOpenSearchEoService;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.opensearch.rest.CollectionsController.IOConsumer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.And;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private static final String EOP_IDENTIFIER = "eop:identifier";

    /** List of parts making up a zipfile for a collection */
    enum ProductPart implements ZipPart {
        Product("product.json"),
        Thumbnail("thumbnail\\.(png|jpeg|jpg)"),
        OwsLinks("owsLinks.json"),
        Granules("granules.json");

        Pattern pattern;

        ProductPart(String pattern) {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean matches(String name) {
            return pattern.matcher(name).matches();
        }
    }

    static final Name PRODUCT_ID = new NameImpl(ProductClass.GENERIC.getNamespace(), "identifier");

    static final Name PARENT_ID = new NameImpl(OpenSearchAccess.EO_NAMESPACE, "parentIdentifier");

    static final List<String> PRODUCT_HREFS =
            Arrays.asList("ogcLinksHref", "metadataHref", "descriptionHref", "thumbnailHref", "granulesHref");

    public ProductsController(OpenSearchAccessProvider accessProvider, OseoJSONConverter jsonConverter) {
        super(accessProvider, jsonConverter);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public ProductReferences getProducts(
            HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "limit", required = false) Integer limit)
            throws IOException {
        // query products for their identifiers
        Query query = new Query();
        PropertyIsEqualTo parentIdFilter = FF.equal(FF.property(PARENT_ID), FF.literal(collection), true);
        query.setFilter(parentIdFilter);
        setupQueryPaging(query, offset, limit);
        query.setSortBy(new SortBy[] {FF.sort(PRODUCT_ID.getLocalPart(), SortOrder.ASCENDING)});
        query.setProperties(Collections.singletonList(FF.property(PRODUCT_ID)));
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureSource<FeatureType, Feature> fs = access.getProductSource();
        FeatureCollection<FeatureType, Feature> features = fs.getFeatures(query);

        // map to java beans for JSON encoding
        String baseURL = ResponseUtils.baseURL(request);
        List<ProductReference> list = new ArrayList<>();
        features.accepts(
                f -> {
                    String id = (String) f.getProperty("identifier").getValue();
                    String productHref = ResponseUtils.buildURL(
                            baseURL,
                            "/rest/oseo/collections/" + collection + "/products/" + id,
                            null,
                            URLType.RESOURCE);
                    String oseoHref = ResponseUtils.buildURL(
                            baseURL, "/oseo/search", Collections.singletonMap("uid", id), URLType.RESOURCE);
                    ProductReference pr = new ProductReference(id, productHref, oseoHref);
                    list.add(pr);
                },
                null);

        if (list.isEmpty()) {
            // suspicious, did the collection actually exist?
            queryCollection(collection, q -> {});
        }

        return new ProductReferences(list);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postProductJson(
            @PathVariable(name = "collection", required = true) String collection,
            HttpServletRequest request,
            @RequestBody(required = true) SimpleFeature feature)
            throws IOException, URISyntaxException {
        String productId = (String) feature.getAttribute("eop:identifier");
        Feature productFeature = simpleToComplex(feature, getProductSchema(), PRODUCT_HREFS);

        // insert the new feature
        runTransactionOnProductStore(fs -> fs.addFeatures(singleton(productFeature)));

        return returnCreatedProductReference(collection, request, productId);
    }

    @PostMapping(consumes = MediaTypeExtensions.APPLICATION_ZIP_VALUE)
    public ResponseEntity<String> postProductZip(
            @PathVariable(name = "collection") String collection, HttpServletRequest request, InputStream body)
            throws IOException, URISyntaxException {

        return createProductFromZip(collection, request, body, null);
    }

    private ResponseEntity<String> createProductFromZip(
            String collection, HttpServletRequest request, InputStream body, String urlProductId)
            throws IOException, URISyntaxException {
        Map<ProductPart, byte[]> parts = parsePartsFromZip(body, ProductPart.values());

        // process the product part
        final byte[] productPayload = parts.get(ProductPart.Product);
        if (productPayload == null) {
            throw new RestException("product.json file is missing from the zip", HttpStatus.BAD_REQUEST);
        }
        SimpleFeature jsonFeature = parseGeoJSONFeature("product.json", productPayload);
        if (urlProductId != null) jsonFeature = updateOrVerifyId(jsonFeature, urlProductId);
        String productId = (String) jsonFeature.getAttribute("eop:identifier");
        String parentId = (String) jsonFeature.getAttribute("eop:parentIdentifier");
        queryCollection(parentId, q -> {});

        Feature productFeature = simpleToComplex(jsonFeature, getProductSchema(), PRODUCT_HREFS);

        // grab the other parts
        byte[] thumbnail = parts.get(ProductPart.Thumbnail);
        byte[] rawLinks = parts.get(ProductPart.OwsLinks);
        SimpleFeatureCollection linksCollection;
        if (rawLinks != null) {
            OgcLinks links = parseJSON(OgcLinks.class, rawLinks);
            linksCollection = beansToLinksCollection(links);
        } else {
            linksCollection = null;
        }
        byte[] rawGranules = parts.get(ProductPart.Granules);
        SimpleFeatureCollection granulesCollection;
        if (rawGranules != null) {
            granulesCollection = parseGeoJSONFeatureCollection("granules.json", rawGranules);
        } else {
            granulesCollection = null;
        }

        // insert the new feature and accessory bits
        runTransactionOnProductStore(fs -> {
            fs.addFeatures(singleton(productFeature));

            final String nsURI = fs.getSchema().getName().getNamespaceURI();
            Filter filter = getProductFilter(collection, productId);

            if (linksCollection != null) {
                fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, linksCollection, filter);
            }

            if (thumbnail != null) {
                fs.modifyFeatures(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME, thumbnail, filter);
            }

            if (granulesCollection != null) {
                fs.modifyFeatures(new NameImpl(nsURI, OpenSearchAccess.GRANULES), granulesCollection, filter);
            }
        });

        // if got here, all is fine
        return returnCreatedProductReference(collection, request, productId);
    }

    private ResponseEntity<String> returnCreatedProductReference(
            String collection, HttpServletRequest request, String productId) throws URISyntaxException {
        String baseURL = ResponseUtils.baseURL(request);
        String newCollectionLocation = ResponseUtils.buildURL(
                baseURL, "/rest/oseo/collections/" + collection + "/products/" + productId, null, URLType.RESOURCE);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(newCollectionLocation));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(productId, headers, HttpStatus.CREATED);
    }

    /*
     * Note, the .+ regular expression allows the product id to contain dots instead of having them interpreted as format extension
     */
    @GetMapping(
            path = "{product:.+}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public SimpleFeature getProduct(
            HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product)
            throws IOException {
        Feature feature = queryProduct(collection, product, q -> {}, true);

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
            String ogcLinks = ResponseUtils.buildURL(baseURL, pathBase + "ogcLinks", null, URLType.RESOURCE);
            String metadata = ResponseUtils.buildURL(baseURL, pathBase + "metadata", null, URLType.RESOURCE);
            String description = ResponseUtils.buildURL(baseURL, pathBase + "description", null, URLType.RESOURCE);
            String thumb = ResponseUtils.buildURL(baseURL, pathBase + "thumbnail", null, URLType.RESOURCE);
            String granules = ResponseUtils.buildURL(baseURL, pathBase + "granules", null, URLType.RESOURCE);

            fb.set("ogcLinksHref", ogcLinks);
            fb.set("metadataHref", metadata);
            fb.set("descriptionHref", description);
            fb.set("thumbnailHref", thumb);
            fb.set("granulesHref", granules);
        });
    }

    @PutMapping(path = "{product:.+}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> putProductJson(
            HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            @RequestBody(required = true) SimpleFeature feature)
            throws IOException, URISyntaxException {
        // check the product exists
        Feature f = queryProduct(collection, product, q -> {}, false);

        // if does not exist, be lenient and create
        if (f == null) return checkProductAndCreate(request, collection, product, feature);

        // otherwise update
        runTransactionOnProductStore(fs -> updateProductInternal(collection, product, feature, fs));
        return ResponseEntity.status(200).build();
    }

    private ResponseEntity<String> checkProductAndCreate(
            HttpServletRequest request, String collection, String productId, SimpleFeature feature)
            throws IOException, URISyntaxException {
        feature = updateOrVerifyId(feature, productId);
        return postProductJson(collection, request, feature);
    }

    private SimpleFeature updateOrVerifyId(SimpleFeature feature, String externalProductId) {
        String productId = (String) feature.getAttribute(EOP_IDENTIFIER);
        if (productId == null) {
            feature = addIdentifier(feature, externalProductId);
        } else if (!externalProductId.equals(productId)) {
            throw new RestException(
                    "Product id in URL not consistent with eop:identifier in JSON, refusing creation",
                    HttpStatus.BAD_REQUEST);
        }

        return feature;
    }

    private SimpleFeature addIdentifier(SimpleFeature feature, String productId) {
        // available but null? just set it
        if (feature.getType().getDescriptor(EOP_IDENTIFIER) != null) {
            feature.setAttribute(EOP_IDENTIFIER, productId);
            return feature;
        }

        // otherwise expand type and add
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.init(feature.getType());
        ftb.add(EOP_IDENTIFIER, String.class);
        SimpleFeatureType schema = ftb.buildFeatureType();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        fb.init(feature);
        fb.set(EOP_IDENTIFIER, productId);
        return fb.buildFeature(feature.getID());
    }

    private void updateProductInternal(String collection, String product, SimpleFeature feature, FeatureStore fs)
            throws IOException {
        // prepare the update, need to convert each field into a Name/Value couple
        Feature productFeature = simpleToComplex(feature, getProductSchema(), PRODUCT_HREFS);
        List<Name> names = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Property p : productFeature.getProperties()) {
            // skip over the large/complex attributes that are being modified via
            // separate calls
            final Name propertyName = p.getName();
            if (OpenSearchAccess.OGC_LINKS_PROPERTY_NAME.equals(propertyName)
                    || OpenSearchAccess.DESCRIPTION.equals(propertyName.getLocalPart())) {
                continue;
            }
            names.add(propertyName);
            values.add(p.getValue());
        }
        Name[] attributeNames = (Name[]) names.toArray(new Name[names.size()]);
        Object[] attributeValues = (Object[]) values.toArray();
        Filter filter = getProductFilter(collection, product);

        fs.modifyFeatures(attributeNames, attributeValues, filter);
    }

    @DeleteMapping(path = "{product:.+}")
    public void deleteProduct(
            @PathVariable(required = true, name = "collection") String collection,
            @PathVariable(name = "product", required = true) String product)
            throws IOException {
        // check the product exists
        queryProduct(collection, product, q -> {}, true);

        // TODO: handle removing the publishing side without removing the metadata
        Filter filter = getProductFilter(collection, product);
        runTransactionOnProductStore(fs -> fs.removeFeatures(filter));
    }

    /*
     * Note, the .+ regular expression allows the product id to contain dots instead of having them interpreted as format extension
     */
    @GetMapping(
            path = "{product:.+}/ogcLinks",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public OgcLinks getProductOgcLinks(
            HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product)
            throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryProduct(
                collection,
                product,
                q -> {
                    q.setProperties(Collections.singletonList(FF.property(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME)));
                },
                true);

        OgcLinks links = buildOgcLinksFromFeature(feature, true);
        return links;
    }

    @PutMapping(path = "{product:.+}/ogcLinks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void putProductLinks(
            HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            @RequestBody OgcLinks links)
            throws IOException {
        // check the product exists
        queryProduct(collection, product, q -> {}, true);

        ListFeatureCollection linksCollection = beansToLinksCollection(links);

        Filter filter = getProductFilter(collection, product);
        runTransactionOnProductStore(
                fs -> fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, linksCollection, filter));
    }

    @DeleteMapping(path = "{product:.+}/ogcLinks")
    public void deleteProductLinks(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product)
            throws IOException {
        // check the product exists
        queryProduct(collection, product, q -> {}, true);

        // prepare the update
        Filter filter = getProductFilter(collection, product);
        runTransactionOnProductStore(fs -> fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, null, filter));
    }

    private void throwProductNotFound(String collection, String product, String item) {
        throw new ResourceNotFoundException(
                item + " '" + product + "' in collection '" + collection + "' could not be found");
    }

    /*
     * Note, the .+ regular expression allows the product id to contain dots instead of having them interpreted as format extension
     */
    @GetMapping(
            path = "{product:.+}/description",
            produces = {MediaType.TEXT_HTML_VALUE})
    public void getProductDescription(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            HttpServletResponse response)
            throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryProduct(
                collection,
                product,
                q -> {
                    q.setPropertyNames(new String[] {"htmlDescription"});
                },
                true);

        // grab the description
        Property descriptionProperty = feature.getProperty("htmlDescription");
        if (descriptionProperty != null && descriptionProperty.getValue() instanceof String) {
            String value = (String) descriptionProperty.getValue();
            response.setContentType("text/html");
            StreamUtils.copy(value, UTF_8, response.getOutputStream());
        } else {
            throwProductNotFound(collection, product, "Product");
        }
    }

    @PutMapping(path = "{product:.+}/description", consumes = MediaType.TEXT_HTML_VALUE)
    public void putProductDescription(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            HttpServletRequest request)
            throws IOException {
        // check the product exists
        queryProduct(collection, product, q -> {}, true);

        String description = IOUtils.toString(request.getReader());

        updateDescription(collection, product, description);
    }

    @DeleteMapping(path = "{product:.+}/description")
    public void deleteProductDescription(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product)
            throws IOException {
        // check the product exists
        queryProduct(collection, product, q -> {}, true);

        // set it to null
        updateDescription(collection, product, null);
    }

    private void updateDescription(String collection, String product, String description) throws IOException {
        // prepare the update
        Filter filter = getProductFilter(collection, product);
        runTransactionOnProductStore(fs -> {
            // set the description to the specified value
            final FeatureType schema = fs.getSchema();
            final String nsURI = schema.getName().getNamespaceURI();
            fs.modifyFeatures(new NameImpl(nsURI, OpenSearchAccess.DESCRIPTION), description, filter);
        });
    }

    /*
     * Note, the .+ regular expression allows the product id to contain dots instead of having them interpreted as format extension
     */
    @GetMapping(
            path = "{product:.+}/thumbnail",
            produces = {MediaType.ALL_VALUE})
    public void getProductThumbnail(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            HttpServletResponse response)
            throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryProduct(
                collection,
                product,
                q -> {
                    q.setProperties(Collections.singletonList(FF.property(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME)));
                },
                true);

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

    @PutMapping(
            path = "{product:.+}/thumbnail",
            consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public void putProductThumbnail(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            HttpServletRequest request)
            throws IOException {
        // check the product exists
        queryProduct(collection, product, q -> {}, true);

        byte[] thumbnail = IOUtils.toByteArray(request.getInputStream());
        updateThumbnail(collection, product, thumbnail);
    }

    @DeleteMapping(path = "{product:.+}/thumbnail")
    public void deleteProductThumbnail(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product)
            throws IOException {
        // check the product exists
        queryProduct(collection, product, q -> {}, true);

        // set it to null
        updateThumbnail(collection, product, null);
    }

    @GetMapping(
            path = "{product:.+}/granules",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public SimpleFeatureCollection getProductGranules(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product)
            throws IOException {
        SimpleFeatureSource granules = getOpenSearchAccess().getGranules(collection, product);
        return granules.getFeatures();
    }

    @PutMapping(
            path = "{product:.+}/granules",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public void putProductGranules(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            @RequestBody SimpleFeatureCollection granules)
            throws IOException {
        SimpleFeatureStore store = (SimpleFeatureStore) getOpenSearchAccess().getGranules(collection, product);
        runTransactionOnStore(store, s -> {
            s.removeFeatures(Filter.INCLUDE);
            s.addFeatures(granules);
        });
    }

    @DeleteMapping(path = "{product:.+}/granules")
    public void putProductGranules(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product)
            throws IOException {
        SimpleFeatureStore store = (SimpleFeatureStore) getOpenSearchAccess().getGranules(collection, product);
        runTransactionOnStore(store, s -> {
            s.removeFeatures(Filter.INCLUDE);
        });
    }

    private void updateThumbnail(String collection, String product, byte[] thumbnail) throws IOException {
        // prepare the update
        Filter filter = getProductFilter(collection, product);
        runTransactionOnProductStore(fs -> {
            // set the description to the specified value
            fs.modifyFeatures(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME, thumbnail, filter);
        });
    }

    private Feature queryProduct(
            String collection, String product, Consumer<Query> queryDecorator, boolean ensureExists)
            throws IOException {
        // query products
        Query query = new Query();
        Filter filter = getProductFilter(collection, product);
        query.setFilter(filter);
        queryDecorator.accept(query);
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureSource<FeatureType, Feature> fs = access.getProductSource();
        FeatureCollection<FeatureType, Feature> features = fs.getFeatures(query);
        Feature feature = DataUtilities.first(features);

        if (feature == null && ensureExists) {
            throwProductNotFound(collection, product, "Product");
        }

        return feature;
    }

    private And getProductFilter(String collection, String product) {
        PropertyIsEqualTo parentIdFilter = FF.equal(FF.property(PARENT_ID), FF.literal(collection), true);
        PropertyIsEqualTo idFilter = FF.equal(FF.property(PRODUCT_ID), FF.literal(product), true);
        return FF.and(parentIdFilter, idFilter);
    }

    private void runTransactionOnProductStore(IOConsumer<FeatureStore> featureStoreConsumer) throws IOException {
        FeatureStore store = (FeatureStore) getOpenSearchAccess().getProductSource();
        super.runTransactionOnStore(store, featureStoreConsumer);
    }

    FeatureType getProductSchema() throws IOException {
        final OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        final FeatureSource<FeatureType, Feature> productSource = access.getProductSource();
        final FeatureType schema = productSource.getSchema();
        return schema;
    }

    @PutMapping(path = "{product:.+}", consumes = MediaTypeExtensions.APPLICATION_ZIP_VALUE)
    public ResponseEntity<String> putProductZip(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "product", required = true) String product,
            HttpServletRequest request,
            InputStream body)
            throws IOException, URISyntaxException {
        // check the collection actually exists
        queryCollection(collection, q -> {});
        // if the product is missing, be lenient and create it
        Feature f = queryProduct(collection, product, q -> {}, false);
        if (f == null) {
            return createProductFromZip(collection, request, body, product);
        }

        // grab and process the parts
        Map<ProductPart, byte[]> parts = parsePartsFromZip(body, ProductPart.values());

        // process the product part
        final byte[] productPayload = parts.get(ProductPart.Product);
        SimpleFeature jsonFeature;
        if (productPayload != null) {
            jsonFeature = parseGeoJSONFeature("product.json", productPayload);
            // get the JSON and check consistency
            String jsonProductId = (String) jsonFeature.getAttribute("eop:identifier");
            String jsonParentId = (String) jsonFeature.getAttribute("eop:parentIdentifier");
            if (!collection.equals(jsonParentId)) {
                throw new RestException(
                        "product.json file refers to parentId "
                                + jsonParentId
                                + " but the HTTP resource refers to "
                                + collection,
                        HttpStatus.BAD_REQUEST);
            }
            if (!product.equals(jsonProductId)) {
                throw new RestException(
                        "product.json file refers to product "
                                + jsonProductId
                                + " but the HTTP resource refers to "
                                + product,
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            jsonFeature = null;
        }

        // grab the other parts
        byte[] thumbnail = parts.get(ProductPart.Thumbnail);
        byte[] rawLinks = parts.get(ProductPart.OwsLinks);
        SimpleFeatureCollection linksCollection;
        if (rawLinks != null) {
            OgcLinks links = parseJSON(OgcLinks.class, rawLinks);
            linksCollection = beansToLinksCollection(links);
        } else {
            linksCollection = null;
        }
        byte[] rawGranules = parts.get(ProductPart.Granules);
        SimpleFeatureCollection granulesCollection;
        if (rawGranules != null) {
            granulesCollection = parseGeoJSONFeatureCollection("granules.json", rawGranules);
        } else {
            granulesCollection = null;
        }

        // update the feature and accessory bits
        runTransactionOnProductStore(fs -> {
            if (jsonFeature != null) {
                updateProductInternal(collection, product, jsonFeature, fs);
            }

            final String nsURI = fs.getSchema().getName().getNamespaceURI();
            Filter filter = getProductFilter(collection, product);

            if (linksCollection != null) {
                fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, linksCollection, filter);
            }

            if (thumbnail != null) {
                fs.modifyFeatures(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME, thumbnail, filter);
            }

            if (granulesCollection != null) {
                fs.modifyFeatures(new NameImpl(nsURI, OpenSearchAccess.GRANULES), granulesCollection, filter);
            }
        });
        return ResponseEntity.status(200).build();
    }
}

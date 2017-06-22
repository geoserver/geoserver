/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.opensearch.eo.ListComplexFeatureCollection;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the OpenSearch collections
 *
 * @author Andrea Aime - GeoSolutions
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/oseo/collections")
public class CollectionsController extends AbstractOpenSearchController {

    static final List<String> COLLECTION_HREFS = Arrays.asList("ogcLinksHref", "metadataHref",
            "descriptionHref", "thumbnailHref");

    static final FeatureFactory FEATURE_FACTORY = CommonFactoryFinder.getFeatureFactory(null);

    static final Name COLLECTION_ID = new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier");

    public CollectionsController(OpenSearchAccessProvider accessProvider) {
        super(accessProvider);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public CollectionReferences getCollections(HttpServletRequest request,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "limit", required = false) Integer limit) throws IOException {
        // query the collections for their names
        Query query = new Query();
        setupQueryPaging(query, offset, limit);
        query.setSortBy(new SortBy[] { FF.sort("name", SortOrder.ASCENDING) });
        query.setPropertyNames(new String[] { "name" });
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureStore<FeatureType, Feature> fs = (FeatureStore<FeatureType, Feature>) access
                .getCollectionSource();
        FeatureCollection<FeatureType, Feature> features = fs.getFeatures(query);

        // map to java beans for JSON encoding
        String baseURL = ResponseUtils.baseURL(request);
        List<CollectionReference> list = new ArrayList<>();
        features.accepts(f -> {
            String name = (String) f.getProperty("name").getValue();
            String collectionHref = ResponseUtils.buildURL(baseURL,
                    "/rest/oseo/collections/" + name, null, URLType.RESOURCE);
            String oseoHref = ResponseUtils.buildURL(baseURL, "/oseo/description",
                    Collections.singletonMap("parentId", name), URLType.RESOURCE);
            CollectionReference cr = new CollectionReference(name, collectionHref, oseoHref);
            list.add(cr);
        }, null);
        return new CollectionReferences(list);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postCollectionJson(HttpServletRequest request,
            @RequestBody(required = true) SimpleFeature feature) throws IOException, URISyntaxException {
        // get the identifier and name, make sure they are the same
        String name = (String) feature.getAttribute("name");
        if (name == null) {
            throw new RestException("Missing mandatory property 'name'", HttpStatus.BAD_REQUEST);
        }
        String eoId = (String) feature.getAttribute("eo:identifier");
        if (eoId == null) {
            throw new RestException("Missing mandatory 'eo:identifier'", HttpStatus.BAD_REQUEST);
        }
        if (!eoId.equals(name)) {
            throw new RestException(
                    "Inconsistent, collection 'name' and 'eo:identifier' should have the same value (eventually name will be removed)",
                    HttpStatus.BAD_REQUEST);
        }

        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        final FeatureSource<FeatureType, Feature> collectionSource = access.getCollectionSource();
        final FeatureType schema = collectionSource.getSchema();
        ComplexFeatureBuilder builder = new ComplexFeatureBuilder(schema);
        AttributeBuilder ab = new AttributeBuilder(FEATURE_FACTORY);
        for (AttributeDescriptor ad : feature.getType().getAttributeDescriptors()) {
            String sourceName = ad.getLocalName();
            // ignore links
            if (COLLECTION_HREFS.contains(sourceName)) {
                continue;
            }
            // map to complex feature attribute and check
            Name pname = toName(sourceName, schema.getName().getNamespaceURI());
            PropertyDescriptor pd = schema.getDescriptor(pname);
            if (pd == null) {
                throw new RestException("Unexpected attribute found: '" + sourceName + "'",
                        HttpStatus.BAD_REQUEST);
            }

            ab.setDescriptor((AttributeDescriptor) pd);
            Attribute attribute = ab.buildSimple(null, feature.getAttribute(sourceName));
            builder.append(pd.getName(), attribute);
        }
        Feature collectionFeature = builder.buildFeature(feature.getID());

        FeatureStore store = (FeatureStore) getOpenSearchAccess().getCollectionSource();
        try (Transaction t = new DefaultTransaction()) {
            store.setTransaction(t);
            try {
                List<FeatureId> ids = store.addFeatures(singleton(collectionFeature));
                t.commit();
            } catch (Exception e) {
                t.rollback();
            }
        }

        // if got here, all is fine
        String baseURL = ResponseUtils.baseURL(request);
        String newCollectionLocation = ResponseUtils.buildURL(baseURL, "/rest/oseo/collections/" + eoId, null, URLType.RESOURCE);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(newCollectionLocation));
        return new ResponseEntity<>(eoId, headers, HttpStatus.CREATED);
    }

    private FeatureCollection singleton(Feature f) {
        ListComplexFeatureCollection fc = new ListComplexFeatureCollection(f);
        return fc;

    }

    private Name toName(String sourceName, String defaultNamespace) {
        String[] split = sourceName.split(":");
        switch (split.length) {
        case 1:
            if ("geometry".equals(sourceName)) {
                return new NameImpl(defaultNamespace, "footprint");
            } else {
                return new NameImpl(defaultNamespace, sourceName);
            }
        case 2:
            String prefix = split[0];
            String localName = split[1];
            String namespaceURI = null;
            if ("eo".equals(prefix)) {
                namespaceURI = OpenSearchAccess.EO_NAMESPACE;
            } else {
                for (OpenSearchAccess.ProductClass pc : OpenSearchAccess.ProductClass.values()) {
                    if (prefix.equals(pc.getPrefix())) {
                        namespaceURI = pc.getNamespace();
                    }
                }
            }

            if (namespaceURI == null) {
                throw new RestException("Unrecognized attribute prefix in property " + sourceName,
                        HttpStatus.BAD_REQUEST);
            }

            return new NameImpl(namespaceURI, localName);
        default:
            throw new RestException("Unrecognized attribute " + sourceName, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "{collection}", produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public SimpleFeature getCollection(HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // grab the collection
        Feature feature = queryCollection(collection, q -> {
        });

        // map to the output schema for GeoJSON encoding
        SimpleFeatureType targetSchema = mapFeatureTypeToSimple(feature.getType(), ftb -> {
            COLLECTION_HREFS.forEach(href -> ftb.add(href, String.class));
        });
        return mapFeatureToSimple(feature, targetSchema, fb -> {
            String baseURL = ResponseUtils.baseURL(request);
            String pathBase = "/rest/oseo/collections/" + collection + "/";
            String ogcLinks = ResponseUtils.buildURL(baseURL, pathBase + "ogcLinks", null,
                    URLType.RESOURCE);
            String metadata = ResponseUtils.buildURL(baseURL, pathBase + "metadata", null,
                    URLType.RESOURCE);
            String description = ResponseUtils.buildURL(baseURL, pathBase + "description", null,
                    URLType.RESOURCE);
            String thumb = ResponseUtils.buildURL(baseURL, pathBase + "thumbnail", null,
                    URLType.RESOURCE);

            fb.set("ogcLinksHref", ogcLinks);
            fb.set("metadataHref", metadata);
            fb.set("descriptionHref", description);
            fb.set("thumbnailHref", thumb);
        });
    }

    @GetMapping(path = "{collection}/ogcLinks", produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public OgcLinks getCollectionOgcLinks(HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryCollection(collection, q -> {
            q.setProperties(Collections
                    .singletonList(FF.property(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME)));
        });

        OgcLinks links = buildOgcLinksFromFeature(feature);
        return links;
    }

    @GetMapping(path = "{collection}/metadata", produces = { MediaType.TEXT_XML_VALUE })
    public void getCollectionMetadata(
            @PathVariable(name = "collection", required = true) String collection,
            HttpServletResponse response) throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryCollection(collection, q -> {
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
            throw new ResourceNotFoundException(
                    "Metadata for collection '" + collection + "' could not be found");
        }
    }

    @GetMapping(path = "{collection}/description", produces = { MediaType.TEXT_HTML_VALUE })
    public void getCollectionDescription(
            @PathVariable(name = "collection", required = true) String collection,
            HttpServletResponse response) throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryCollection(collection, q -> {
            q.setPropertyNames(new String[] { "htmlDescription" });
        });

        // grab the description
        Property descriptionProperty = feature.getProperty("htmlDescription");
        if (descriptionProperty != null && descriptionProperty.getValue() instanceof String) {
            String value = (String) descriptionProperty.getValue();
            response.setContentType("text/html");
            StreamUtils.copy(value, Charset.forName("UTF-8"), response.getOutputStream());
        } else {
            throw new ResourceNotFoundException(
                    "Description for collection '" + collection + "' could not be found");
        }
    }

}

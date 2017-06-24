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

import org.apache.commons.io.IOUtils;
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
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
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
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
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
 * Controller for the OpenSearch collections
 *
 * @author Andrea Aime - GeoSolutions
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/oseo/collections")
public class CollectionsController extends AbstractOpenSearchController {

    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

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
            @RequestBody(required = true) SimpleFeature feature)
            throws IOException, URISyntaxException {
        String eoId = checkCollectionIdentifier(feature);
        Feature collectionFeature = simpleToComplex(feature);

        // insert the new feature
        runTransactionOnCollectionStore(fs -> fs.addFeatures(singleton(collectionFeature)));

        // if got here, all is fine
        String baseURL = ResponseUtils.baseURL(request);
        String newCollectionLocation = ResponseUtils.buildURL(baseURL,
                "/rest/oseo/collections/" + eoId, null, URLType.RESOURCE);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(newCollectionLocation));
        return new ResponseEntity<>(eoId, headers, HttpStatus.CREATED);
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

    @PutMapping(path = "{collection}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void putCollectionJson(HttpServletRequest request,
            @PathVariable(required = true, name = "collection") String collection,
            @RequestBody(required = true) SimpleFeature feature)
            throws IOException, URISyntaxException {
        // check the collection exists
        queryCollection(collection, q -> {
        });

        // check the id, mind, could be different from the collection one if the client
        // is trying to change
        String eoId = checkCollectionIdentifier(feature);

        // prepare the update, need to convert each field into a Name/Value couple
        Feature collectionFeature = simpleToComplex(feature);
        List<Name> names = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Property p : collectionFeature.getProperties()) {
            // skip over the large/complex attributes that are being modified via separate calls
            final Name propertyName = p.getName();
            if (OpenSearchAccess.METADATA_PROPERTY_NAME.equals(propertyName)
                    || OpenSearchAccess.OGC_LINKS_PROPERTY_NAME.equals(propertyName)
                    || OpenSearchAccess.DESCRIPTION.equals(propertyName.getLocalPart())) {
                continue;
            }
            names.add(propertyName);
            values.add(p.getValue());
        }
        Name[] attributeNames = (Name[]) names.toArray(new Name[names.size()]);
        Object[] attributeValues = (Object[]) values.toArray();
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(
                fs -> fs.modifyFeatures(attributeNames, attributeValues, filter));
    }

    @DeleteMapping(path = "{collection}")
    public void deleteCollection(
            @PathVariable(required = true, name = "collection") String collection)
            throws IOException {
        // check the collection exists
        queryCollection(collection, q -> {
        });

        // TODO: handle cascading on products, and removing the publishing side without removing the metadata
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(fs -> fs.removeFeatures(filter));
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

        OgcLinks links = buildOgcLinksFromFeature(feature, true);
        return links;
    }
    
    @PutMapping(path = "{collection}/ogcLinks", consumes = MediaType.APPLICATION_JSON_VALUE )
    public void putCollectionOgcLinks(HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @RequestBody OgcLinks links)
            throws IOException {
        // check the collection is there
        queryCollection(collection, q -> {});

        SimpleFeatureType schema = buildOgcLinksType();
        ListFeatureCollection linksCollection = new ListFeatureCollection(schema);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        for (OgcLink link : links.links) {
            fb.set("offering", link.offering);
            fb.set("method", link.method);
            fb.set("code", link.code);
            fb.set("type", link.type);
            fb.set("href", link.href);
            SimpleFeature sf = fb.buildFeature(null);
            linksCollection.add(sf);
        }
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(
                fs -> fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, linksCollection, filter));
    }
    
    private SimpleFeatureType buildOgcLinksType() throws IOException {
        String ns = getOpenSearchAccess().getCollectionSource().getName().getNamespaceURI();
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("ogc_links");
        tb.setNamespaceURI(ns);
        tb.add("offering", String.class);
        tb.add("method", String.class);
        tb.add("code", String.class);
        tb.add("type", String.class);
        tb.add("href", String.class);
        return tb.buildFeatureType();
    }
    
    @DeleteMapping(path = "{collection}/ogcLinks")
    public void deleteCollectionLinks(
            @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // check the collection is there
        queryCollection(collection, q -> {
        });

        // prepare the update
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(
                fs -> fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, null, filter));
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

    @PutMapping(path = "{collection}/metadata", consumes = MediaType.TEXT_XML_VALUE)
    public void putCollectionMetadata(
            @PathVariable(name = "collection", required = true) String collection,
            HttpServletRequest request) throws IOException {
        // check the collection is there
        queryCollection(collection, q -> {
        });

        // TODO: validate it's actual ISO metadata
        String metadata = IOUtils.toString(request.getReader());
        checkWellFormedXML(metadata);

        // prepare the update
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(
                fs -> fs.modifyFeatures(OpenSearchAccess.METADATA_PROPERTY_NAME, metadata, filter));
    }

    @DeleteMapping(path = "{collection}/metadata")
    public void deleteCollectionMetadata(
            @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // check the collection is there
        queryCollection(collection, q -> {
        });

        // prepare the update
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(
                fs -> fs.modifyFeatures(OpenSearchAccess.METADATA_PROPERTY_NAME, null, filter));
    }

    @GetMapping(path = "{collection}/description", produces = { MediaType.TEXT_HTML_VALUE })
    public void getCollectionDescription(
            @PathVariable(name = "collection", required = true) String collection,
            HttpServletResponse response) throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryCollection(collection, q -> {
            q.setPropertyNames(new String[] { OpenSearchAccess.DESCRIPTION });
        });

        // grab the description
        Property descriptionProperty = feature.getProperty(OpenSearchAccess.DESCRIPTION);
        if (descriptionProperty != null && descriptionProperty.getValue() instanceof String) {
            String value = (String) descriptionProperty.getValue();
            response.setContentType("text/html");
            StreamUtils.copy(value, Charset.forName("UTF-8"), response.getOutputStream());
        } else {
            throw new ResourceNotFoundException(
                    "Description for collection '" + collection + "' could not be found");
        }
    }

    @PutMapping(path = "{collection}/description", consumes = MediaType.TEXT_HTML_VALUE)
    public void putCollectionDescription(
            @PathVariable(name = "collection", required = true) String collection,
            HttpServletRequest request) throws IOException {
        // check the collection is there
        queryCollection(collection, q -> {
        });

        String description = IOUtils.toString(request.getReader());

        updateDescription(collection, description);
    }

    @DeleteMapping(path = "{collection}/description")
    public void deleteCollectionDescritiopn(
            @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // check the collection is there
        queryCollection(collection, q -> {
        });

        // set it to null
        updateDescription(collection, null);
    }

    private void updateDescription(String collection, String description) throws IOException {
        // prepare the update
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(fs -> {
            // set the description to null
            final FeatureSource<FeatureType, Feature> collectionSource = getOpenSearchAccess()
                    .getCollectionSource();
            final FeatureType schema = collectionSource.getSchema();
            final String nsURI = schema.getName().getNamespaceURI();
            fs.modifyFeatures(new NameImpl(nsURI, OpenSearchAccess.DESCRIPTION),
                    description, filter);
        });
    }

    private void runTransactionOnCollectionStore(IOConsumer<FeatureStore> featureStoreConsumer)
            throws IOException {
        FeatureStore store = (FeatureStore) getOpenSearchAccess().getCollectionSource();
        try (Transaction t = new DefaultTransaction()) {
            store.setTransaction(t);
            try {
                featureStoreConsumer.accept(store);
                t.commit();
            } catch (Exception e) {
                t.rollback();
                throw new IOException(
                        "Failed to run modification on collection store:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Converts the simple feature representatin of a collection into a complex feature suitable for OpenSearchAccess usage
     * 
     * @param feature
     * @return
     * @throws IOException
     */
    private Feature simpleToComplex(SimpleFeature feature) throws IOException {
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
        return collectionFeature;
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

    private String checkCollectionIdentifier(SimpleFeature feature) {
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
        return eoId;
    }

}

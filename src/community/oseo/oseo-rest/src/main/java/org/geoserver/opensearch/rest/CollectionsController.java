/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.Catalog;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.OseoEvent;
import org.geoserver.opensearch.eo.OseoEventListener;
import org.geoserver.opensearch.eo.OseoEventType;
import org.geoserver.opensearch.eo.store.CollectionLayer;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.api.referencing.FactoryException;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public static final Consumer<Query> IDENTITY = q -> {};
    protected List<OseoEventListener> eventListeners = new ArrayList<>();
    /** List of parts making up a zipfile for a collection */
    enum CollectionPart implements ZipPart {
        Collection("collection.json"),
        Thumbnail("thumbnail\\.[png|jpeg|jpg]"),
        OwsLinks("owsLinks.json");

        Pattern pattern;

        CollectionPart(String pattern) {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean matches(String name) {
            return pattern.matcher(name).matches();
        }
    }

    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

    static final List<String> COLLECTION_HREFS =
            Arrays.asList("ogcLinksHref", "metadataHref", "descriptionHref", "thumbnailHref");

    static final Name COLLECTION_ID = new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier");

    Catalog catalog;

    public CollectionsController(
            OpenSearchAccessProvider accessProvider, OseoJSONConverter jsonConverter, Catalog catalog) {
        super(accessProvider, jsonConverter);
        this.catalog = catalog;
        eventListeners.addAll(GeoServerExtensions.extensions(OseoEventListener.class));
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public CollectionReferences getCollections(
            HttpServletRequest request,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "limit", required = false) Integer limit)
            throws IOException {
        // query the collections for their names
        Query query = new Query();
        setupQueryPaging(query, offset, limit);
        query.setSortBy(new SortBy[] {FF.sort("name", SortOrder.ASCENDING)});
        query.setPropertyNames(new String[] {"name"});
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureStore<FeatureType, Feature> fs = (FeatureStore<FeatureType, Feature>) access.getCollectionSource();
        FeatureCollection<FeatureType, Feature> features = fs.getFeatures(query);

        // map to java beans for JSON encoding
        String baseURL = ResponseUtils.baseURL(request);
        List<CollectionReference> list = new ArrayList<>();
        features.accepts(
                f -> {
                    String name = (String) f.getProperty("name").getValue();
                    String collectionHref =
                            ResponseUtils.buildURL(baseURL, "/rest/oseo/collections/" + name, null, URLType.RESOURCE);
                    String oseoHref = ResponseUtils.buildURL(
                            baseURL, "/oseo/description", Collections.singletonMap("parentId", name), URLType.RESOURCE);
                    CollectionReference cr = new CollectionReference(name, collectionHref, oseoHref);
                    list.add(cr);
                },
                null);
        return new CollectionReferences(list);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postCollectionJson(
            HttpServletRequest request, @RequestBody(required = true) SimpleFeature feature)
            throws IOException, URISyntaxException {
        String eoId = checkCollectionIdentifier(feature);
        Feature collectionFeature = simpleToComplex(feature, getCollectionSchema(), COLLECTION_HREFS);

        // insert the new feature
        runTransactionOnCollectionStore(fs -> fs.addFeatures(singleton(collectionFeature)));

        Feature collectionAfter = queryCollection(eoId, IDENTITY);
        broadcastOseoEvent(OseoEventType.POST_INSERT, eoId, null, collectionAfter);

        // if got here, all is fine
        return returnCreatedCollectionReference(request, eoId);
    }

    private ResponseEntity<String> returnCreatedCollectionReference(HttpServletRequest request, String eoId)
            throws URISyntaxException {
        String baseURL = ResponseUtils.baseURL(request);
        String newCollectionLocation =
                ResponseUtils.buildURL(baseURL, "/rest/oseo/collections/" + eoId, null, URLType.RESOURCE);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(newCollectionLocation));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(eoId, headers, HttpStatus.CREATED);
    }

    @PostMapping(consumes = MediaTypeExtensions.APPLICATION_ZIP_VALUE)
    public ResponseEntity<String> postCollectionZip(HttpServletRequest request, InputStream body)
            throws IOException, URISyntaxException {

        Map<CollectionPart, byte[]> parts = parsePartsFromZip(body, CollectionPart.values());

        // process the collection part
        final byte[] collectionPayload = parts.get(CollectionPart.Collection);
        if (collectionPayload == null) {
            throw new RestException("collection.json file is missing from the zip", HttpStatus.BAD_REQUEST);
        }
        SimpleFeature jsonFeature = parseGeoJSONFeature("collection.json", collectionPayload);
        String eoId = checkCollectionIdentifier(jsonFeature);
        Feature collectionFeature = simpleToComplex(jsonFeature, getCollectionSchema(), COLLECTION_HREFS);

        // grab the other parts
        byte[] rawLinks = parts.get(CollectionPart.OwsLinks);
        SimpleFeatureCollection linksCollection;
        if (rawLinks != null) {
            OgcLinks links = parseJSON(OgcLinks.class, rawLinks);
            linksCollection = beansToLinksCollection(links);
        } else {
            linksCollection = null;
        }

        // insert the new feature and accessory bits
        runTransactionOnCollectionStore(fs -> {
            fs.addFeatures(singleton(collectionFeature));

            Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(eoId), true);

            if (linksCollection != null) {
                fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, linksCollection, filter);
            }
        });

        Feature collectionAfter = queryCollection(eoId, IDENTITY);
        broadcastOseoEvent(OseoEventType.POST_INSERT, eoId, null, collectionAfter);
        return returnCreatedCollectionReference(request, eoId);
    }

    @GetMapping(
            path = "{collection}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public SimpleFeature getCollection(
            HttpServletRequest request, @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // grab the collection
        Feature feature = queryCollection(collection, IDENTITY);

        // map to the output schema for GeoJSON encoding
        SimpleFeatureType targetSchema = mapFeatureTypeToSimple(feature.getType(), ftb -> {
            COLLECTION_HREFS.forEach(href -> ftb.add(href, String.class));
        });
        return mapFeatureToSimple(feature, targetSchema, fb -> {
            String baseURL = ResponseUtils.baseURL(request);
            String pathBase = "/rest/oseo/collections/" + collection + "/";
            String ogcLinks = ResponseUtils.buildURL(baseURL, pathBase + "ogcLinks", null, URLType.RESOURCE);
            String metadata = ResponseUtils.buildURL(baseURL, pathBase + "metadata", null, URLType.RESOURCE);
            String description = ResponseUtils.buildURL(baseURL, pathBase + "description", null, URLType.RESOURCE);
            String thumb = ResponseUtils.buildURL(baseURL, pathBase + "thumbnail", null, URLType.RESOURCE);

            fb.set("ogcLinksHref", ogcLinks);
            fb.set("metadataHref", metadata);
            fb.set("descriptionHref", description);
            fb.set("thumbnailHref", thumb);
        });
    }

    @PutMapping(path = "{collection}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void putCollectionJson(
            HttpServletRequest request,
            @PathVariable(required = true, name = "collection") String collection,
            @RequestBody(required = true) SimpleFeature feature)
            throws IOException, URISyntaxException {
        // check the collection exists
        Feature collectionBefore = queryCollection(collection, IDENTITY);

        // check the id, mind, could be different from the collection one if the client
        // is trying to change
        checkCollectionIdentifier(feature);

        // prepare the update, need to convert each field into a Name/Value couple
        Feature collectionFeature = simpleToComplex(feature, getCollectionSchema(), COLLECTION_HREFS);
        List<Name> names = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Property p : collectionFeature.getProperties()) {
            // skip over the large/complex attributes that are being modified via separate calls
            final Name propertyName = p.getName();
            if (OpenSearchAccess.OGC_LINKS_PROPERTY_NAME.equals(propertyName)
                    || OpenSearchAccess.DESCRIPTION.equals(propertyName.getLocalPart())) {
                continue;
            }
            names.add(propertyName);
            values.add(p.getValue());
        }
        broadcastOseoEvent(OseoEventType.PRE_UPDATE, collection, collectionBefore, null);
        Name[] attributeNames = names.toArray(new Name[names.size()]);
        Object[] attributeValues = values.toArray();
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);

        runTransactionOnCollectionStore(fs -> fs.modifyFeatures(attributeNames, attributeValues, filter));
        Feature collectionAfter = queryCollection(collection, IDENTITY);
        broadcastOseoEvent(OseoEventType.POST_UPDATE, collection, collectionBefore, collectionAfter);
    }

    private void broadcastOseoEvent(
            OseoEventType eventType, String collectionName, Feature collection, Feature collectionAfter) {
        OseoListenerMux oseoListenerMux = new OseoListenerMux();
        OseoEvent oseovent = new OseoEvent();
        oseovent.setType(eventType);
        oseovent.setCollectionName(collectionName);
        oseoListenerMux.dataStoreChange(oseovent);
    }

    @DeleteMapping(path = "{collection}")
    public void deleteCollection(@PathVariable(required = true, name = "collection") String collection)
            throws IOException {
        // check the collection exists
        Feature collectionBefore = queryCollection(collection, IDENTITY);

        broadcastOseoEvent(OseoEventType.PRE_DELETE, collection, collectionBefore, null);

        // TODO: handle cascading on products, and removing the publishing side without removing the
        // metadata
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(fs -> fs.removeFeatures(filter));
    }

    @GetMapping(
            path = "{collection}/layer",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public CollectionLayer getDefaultCollectionLayer(
            HttpServletRequest request, @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // query one collection and grab its OGC links
        return getCollectionLayer(collection, null);
    }

    @PutMapping(path = "{collection}/layer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> putDefaultCollectionLayer(
            @PathVariable(name = "collection", required = true) String collection,
            @RequestBody CollectionLayer newDefaultLayer)
            throws IOException {
        newDefaultLayer.setDefaultLayer(true);
        return createReplaceLayer(collection, newDefaultLayer, CollectionLayer::isDefaultLayer);
    }

    private ResponseEntity<Object> createReplaceLayer(
            @PathVariable(name = "collection", required = true) String collection,
            @RequestBody CollectionLayer layer,
            Predicate<CollectionLayer> previousLayerPredicate)
            throws IOException {
        // check the collection is there
        Feature collectionBefore = queryCollection(collection, IDENTITY);

        // validate the layer
        validateLayer(layer);

        // see if there was a layer here, if so, remove it and put the new one in its place
        Feature collectionFeature = queryCollection(collection, IDENTITY);
        List<CollectionLayer> collectionLayers = buildCollectionLayersFromFeature(collectionFeature, false);
        CollectionLayer previousLayer = collectionLayers.stream()
                .filter(previousLayerPredicate)
                .findFirst()
                .orElse(null);
        if (previousLayer != null) {
            collectionLayers.remove(previousLayer);
        }
        collectionLayers.add(layer);
        normalizeDefaultLayer(collectionLayers, layer.isDefaultLayer() ? layer : null);

        // prepare the update
        ListFeatureCollection layersCollection = beansToLayerCollectionFeature(collectionLayers);
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        Name layersName = getOpenSearchAccess().getName(OpenSearchAccess.LAYERS);
        runTransactionOnCollectionStore(fs -> {
            fs.modifyFeatures(layersName, layersCollection, filter);
        });

        // this is done after the changes in the DB to make sure it's reading the same data
        // we are inserting (feature sources do not accept a transaction...)
        CollectionLayerManager layerManager = new CollectionLayerManager(catalog, accessProvider);
        if (previousLayer != null) {
            layerManager.removeMosaicAndLayer(previousLayer);
        }
        try {
            layerManager.createMosaicAndLayer(collection, layer);
        } catch (Exception e) {
            // rethrow to make the transaction fail
            throw new RuntimeException(e);
        }
        broadcastOseoEvent(OseoEventType.POST_INSERT, collection, collectionBefore, null);
        // see if we have to return a creation or not
        if (previousLayer != null) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
    }

    private CollectionLayer getCollectionLayer(String collection, String name) throws IOException {
        List<CollectionLayer> layers = getCollectionLayers(collection);
        return getCollectionLayer(layers, name);
    }

    private CollectionLayer getCollectionLayer(List<CollectionLayer> layers, String name) {
        if (name == null) {
            return layers.stream()
                    .filter(CollectionLayer::isDefaultLayer)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException());
        } else {
            return layers.stream()
                    .filter(l -> name.equals(l.getLayer()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException());
        }
    }

    private List<CollectionLayer> getCollectionLayers(String collection) throws IOException {
        Feature feature = queryCollection(collection, IDENTITY);
        return buildCollectionLayersFromFeature(feature, false);
    }

    private List<CollectionLayer> buildCollectionLayersFromFeature(Feature feature, boolean notFoundOnEmpty)
            throws IOException {
        List<CollectionLayer> layers = CollectionLayer.buildCollectionLayersFromFeature(feature);
        if (layers == null && notFoundOnEmpty) {
            throw new ResourceNotFoundException();
        }

        return layers;
    }

    /** Validates the layer and throws appropriate exceptions in case mandatory bits are missing */
    private void validateLayer(CollectionLayer layer) {
        if (layer.getWorkspace() == null) {
            throw new RestException(
                    "Invalid layer configuration, workspace name is missing or null", HttpStatus.BAD_REQUEST);
        }
        if (catalog.getWorkspaceByName(layer.getWorkspace()) == null) {
            throw new RestException(
                    "Invalid layer configuration, workspace '" + layer.getWorkspace() + "' does not exist",
                    HttpStatus.BAD_REQUEST);
        }
        if (layer.getLayer() == null) {
            throw new RestException(
                    "Invalid layer configuration, layer name is missing or null", HttpStatus.BAD_REQUEST);
        }

        if (layer.isSeparateBands()) {
            if (layer.getBands() == null || layer.getBands().length == 0) {
                throw new RestException(
                        "Invalid layer configuration, claims to have separate bands but does "
                                + "not list the band names",
                        HttpStatus.BAD_REQUEST);
            }
            if (layer.getBrowseBands() == null || layer.getBrowseBands().length == 0) {
                throw new RestException(
                        "Invalid layer configuration, claims to have separate bands but does not "
                                + "list the browse band names (hence cannot setup a style for it)",
                        HttpStatus.BAD_REQUEST);
            } else if ((layer.getBrowseBands().length != 3 && layer.getBrowseBands().length != 1)) {
                throw new RestException(
                        "Invalid layer configuration, browse bands must be either " + "one (gray) or three (RGB)",
                        HttpStatus.BAD_REQUEST);
            }
        }
        // right now the mosaic can only be in 4326 because the granule table is in that CRS
        if (layer.getMosaicCRS() != null) {
            try {
                if (!CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, CRS.decode(layer.getMosaicCRS()))) {
                    throw new RestException(
                            "Invalid mosaicCRS value, can only be EPSG:4326 for the time being",
                            HttpStatus.BAD_REQUEST);
                }
            } catch (FactoryException e) {
                throw new RestException(
                        "Invalid mosaicCRS value, cannot be decoded: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    @DeleteMapping(path = "{collection}/layer")
    public void deleteDefaultCollectionLayer(@PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // check the collection is there
        queryCollection(collection, IDENTITY);

        // prepare the update
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(fs -> {
            List<CollectionLayer> collectionLayers = getCollectionLayers(collection);
            CollectionLayer previousDefaultLayer = getCollectionLayer(collectionLayers, null);
            if (previousDefaultLayer != null) {
                collectionLayers.remove(previousDefaultLayer);
            }

            // remove from DB if empty, otherwise promote one to default
            Name layersName = getOpenSearchAccess().getName(OpenSearchAccess.LAYERS);
            if (collectionLayers.isEmpty()) {
                fs.modifyFeatures(layersName, null, filter);
            } else {
                normalizeDefaultLayer(collectionLayers, null);
                ListFeatureCollection layersCollection = beansToLayerCollectionFeature(collectionLayers);
                fs.modifyFeatures(layersName, layersCollection, filter);
            }

            // remove from configuration if needed
            if (previousDefaultLayer != null) {
                new CollectionLayerManager(catalog, accessProvider).removeMosaicAndLayer(previousDefaultLayer);
            }
        });
    }

    @DeleteMapping(path = "{collection}/layers/{layer}")
    public void deleteCollectionLayer(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "layer", required = true) String layer)
            throws IOException {
        // check the collection is there
        queryCollection(collection, IDENTITY);

        // prepare the update
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(fs -> {
            List<CollectionLayer> collectionLayers = getCollectionLayers(collection);
            CollectionLayer previousDefaultLayer = getCollectionLayer(collectionLayers, null);
            CollectionLayer removedLayer = getCollectionLayer(collectionLayers, layer);
            if (removedLayer != null) {
                collectionLayers.remove(removedLayer);
            }

            // remove from DB if empty, otherwise promote one to default
            Name layersName = getOpenSearchAccess().getName(OpenSearchAccess.LAYERS);
            if (collectionLayers.isEmpty()) {
                fs.modifyFeatures(layersName, null, filter);
            } else {
                normalizeDefaultLayer(collectionLayers, null);
                ListFeatureCollection layersCollection = beansToLayerCollectionFeature(collectionLayers);
                fs.modifyFeatures(layersName, layersCollection, filter);
            }

            // remove from configuration if needed
            if (removedLayer != null) {
                new CollectionLayerManager(catalog, accessProvider).removeMosaicAndLayer(removedLayer);
            }
        });
    }

    /**
     * Makes sure there is only one default collection layer
     *
     * @param collectionLayers the layer list to normalize
     * @param newDefault the layer to set as default, or null if no specific layer should be made the default (the
     *     method will check if there is already one, if not, will set the first found)
     */
    private void normalizeDefaultLayer(List<CollectionLayer> collectionLayers, CollectionLayer newDefault) {
        for (CollectionLayer collectionLayer : collectionLayers) {
            if (newDefault == null) {
                if (collectionLayer.isDefaultLayer()) {
                    newDefault = collectionLayer;
                }
            } else if (collectionLayer.isDefaultLayer() && !newDefault.equals(collectionLayer)) {
                collectionLayer.setDefaultLayer(false);
            }
        }
        if (newDefault == null && !collectionLayers.isEmpty()) {
            collectionLayers.get(0).setDefaultLayer(true);
        }
    }

    private ListFeatureCollection beansToLayerCollectionFeature(List<CollectionLayer> layers) throws IOException {
        SimpleFeatureType schema = getOpenSearchAccess().getCollectionLayerSchema();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        ListFeatureCollection result = new ListFeatureCollection(schema);
        for (CollectionLayer layer : layers) {
            fb.set("workspace", layer.getWorkspace());
            fb.set("layer", layer.getLayer());
            fb.set("separateBands", layer.isSeparateBands());
            fb.set("bands", layer.getBands());
            fb.set("browseBands", layer.getBrowseBands());
            fb.set("heterogeneousCRS", layer.isHeterogeneousCRS());
            fb.set("mosaicCRS", layer.getMosaicCRS());
            fb.set("defaultLayer", layer.isDefaultLayer());
            SimpleFeature sf = fb.buildFeature(null);
            result.add(sf);
        }

        return result;
    }

    @GetMapping(
            path = "{collection}/ogcLinks",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public OgcLinks getCollectionOgcLinks(
            HttpServletRequest request, @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // query one collection and grab its OGC links
        Feature feature = queryCollection(collection, q -> {
            q.setProperties(Collections.singletonList(FF.property(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME)));
        });

        OgcLinks links = buildOgcLinksFromFeature(feature, true);
        return links;
    }

    @PutMapping(path = "{collection}/ogcLinks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void putCollectionOgcLinks(
            HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @RequestBody OgcLinks links)
            throws IOException {
        // check the collection is there
        queryCollection(collection, IDENTITY);

        ListFeatureCollection linksCollection = beansToLinksCollection(links);

        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(
                fs -> fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, linksCollection, filter));
    }

    @DeleteMapping(path = "{collection}/ogcLinks")
    public void deleteCollectionLinks(@PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // check the collection is there
        queryCollection(collection, IDENTITY);

        // prepare the update
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(
                fs -> fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, null, filter));
    }

    private void runTransactionOnCollectionStore(IOConsumer<FeatureStore> featureStoreConsumer) throws IOException {
        FeatureStore store = (FeatureStore) getOpenSearchAccess().getCollectionSource();
        super.runTransactionOnStore(store, featureStoreConsumer);
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

    FeatureType getCollectionSchema() throws IOException {
        final OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        final FeatureSource<FeatureType, Feature> collectionSource = access.getCollectionSource();
        final FeatureType schema = collectionSource.getSchema();
        return schema;
    }

    @GetMapping(
            path = "{collection}/layers",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public LayerReferences getCollectionLayers(
            HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "limit", required = false) Integer limit)
            throws IOException {
        List<CollectionLayer> layers = getCollectionLayers(collection);

        // apply paging (no errors in case of invalid offset/limit as the total size cannot be
        // discovered)
        if (offset != null) {
            if (offset > layers.size()) {
                return LayerReferences.EMPTY;
            }
            layers = layers.subList(offset, layers.size());
        }
        if (limit != null) {
            layers = layers.subList(0, limit);
        }

        // map to list of references and return
        String baseURL = ResponseUtils.baseURL(request);
        List<LayerReference> layerReferences = layers.stream()
                .map(cl -> {
                    String layerName = cl.getLayer();
                    String collectionHref = ResponseUtils.buildURL(
                            baseURL,
                            "/rest/oseo/collections/" + collection + "/layers/" + layerName,
                            null,
                            URLType.RESOURCE);
                    return new LayerReference(layerName, collectionHref);
                })
                .collect(Collectors.toList());

        return new LayerReferences(layerReferences);
    }

    @GetMapping(
            path = "{collection}/layers/{layer}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public CollectionLayer getCollectionLayer(
            HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "layer", required = true) String layer)
            throws IOException {
        return getCollectionLayer(collection, layer);
    }

    @PutMapping(path = "{collection}/layers/{layer}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> putCollectionLayer(
            @PathVariable(name = "collection", required = true) String collection,
            @PathVariable(name = "layer", required = true) String layer,
            @RequestBody CollectionLayer collectionLayer)
            throws IOException {
        return createReplaceLayer(collection, collectionLayer, l -> layer.equals(l.getLayer()));
    }

    @PostMapping(path = "{collection}/layers", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postCollectionLayer(
            @PathVariable(name = "collection", required = true) String collection,
            @RequestBody CollectionLayer collectionLayer)
            throws IOException {
        return createReplaceLayer(
                collection, collectionLayer, l -> collectionLayer.getLayer().equals(l.getLayer()));
    }

    class OseoListenerMux implements OseoEventListener {
        public void dataStoreChange(List listeners, OseoEvent event) {
            for (Object o : listeners) {
                OseoEventListener listener = (OseoEventListener) o;
                listener.dataStoreChange(event);
            }
        }

        @Override
        public void dataStoreChange(OseoEvent event) {
            dataStoreChange(eventListeners, event);
        }
    }
}

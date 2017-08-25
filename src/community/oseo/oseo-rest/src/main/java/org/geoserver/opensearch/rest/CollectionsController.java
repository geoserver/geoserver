/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.ImageLayout;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.CollectionLayer;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.image.io.ImageIOExt;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.styling.builder.ChannelSelectionBuilder;
import org.geotools.styling.builder.RasterSymbolizerBuilder;
import org.geotools.util.Version;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.referencing.FactoryException;
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

    private static final String TIME_START = "timeStart";

    static final Hints EXCLUDE_MOSAIC_HINTS = new Hints(Utils.EXCLUDE_MOSAIC, true);

    /**
     * List of parts making up a zipfile for a collection
     */
    enum CollectionPart implements ZipPart {
        Collection("collection.json"), Description("description.html"), Metadata(
                "metadata.xml"), Thumbnail("thumbnail\\.[png|jpeg|jpg]"), OwsLinks("owsLinks.json");

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

    static final List<String> COLLECTION_HREFS = Arrays.asList("ogcLinksHref", "metadataHref",
            "descriptionHref", "thumbnailHref");

    static final Name COLLECTION_ID = new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier");

    Catalog catalog;

    public CollectionsController(OpenSearchAccessProvider accessProvider,
            OseoJSONConverter jsonConverter, Catalog catalog) {
        super(accessProvider, jsonConverter);
        this.catalog = catalog;
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
        Feature collectionFeature = simpleToComplex(feature, getCollectionSchema(),
                COLLECTION_HREFS);

        // insert the new feature
        runTransactionOnCollectionStore(fs -> fs.addFeatures(singleton(collectionFeature)));

        // if got here, all is fine
        return returnCreatedCollectionReference(request, eoId);
    }

    private ResponseEntity<String> returnCreatedCollectionReference(HttpServletRequest request,
            String eoId) throws URISyntaxException {
        String baseURL = ResponseUtils.baseURL(request);
        String newCollectionLocation = ResponseUtils.buildURL(baseURL,
                "/rest/oseo/collections/" + eoId, null, URLType.RESOURCE);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(newCollectionLocation));
        return new ResponseEntity<>(eoId, headers, HttpStatus.CREATED);
    }

    @PostMapping(consumes = MediaTypeExtensions.APPLICATION_ZIP_VALUE)
    public ResponseEntity<String> postCollectionZip(HttpServletRequest request, InputStream body)
            throws IOException, URISyntaxException {

        Map<CollectionPart, byte[]> parts = parsePartsFromZip(body, CollectionPart.values());

        // process the collection part
        final byte[] collectionPayload = parts.get(CollectionPart.Collection);
        if (collectionPayload == null) {
            throw new RestException("collection.json file is missing from the zip",
                    HttpStatus.BAD_REQUEST);
        }
        SimpleFeature jsonFeature = parseGeoJSONFeature("collection.json", collectionPayload);
        String eoId = checkCollectionIdentifier(jsonFeature);
        Feature collectionFeature = simpleToComplex(jsonFeature, getCollectionSchema(),
                COLLECTION_HREFS);

        // grab the other parts
        byte[] description = parts.get(CollectionPart.Description);
        byte[] metadata = parts.get(CollectionPart.Metadata);
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

            final String nsURI = fs.getSchema().getName().getNamespaceURI();
            Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(eoId), true);

            if (description != null) {
                String descriptionString = new String(description);
                fs.modifyFeatures(new NameImpl(nsURI, OpenSearchAccess.DESCRIPTION),
                        descriptionString, filter);
            }

            if (metadata != null) {
                String descriptionString = new String(metadata);
                fs.modifyFeatures(OpenSearchAccess.METADATA_PROPERTY_NAME, descriptionString,
                        filter);
            }

            if (linksCollection != null) {
                fs.modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, linksCollection,
                        filter);
            }

        });

        return returnCreatedCollectionReference(request, eoId);
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
        checkCollectionIdentifier(feature);

        // prepare the update, need to convert each field into a Name/Value couple
        Feature collectionFeature = simpleToComplex(feature, getCollectionSchema(),
                COLLECTION_HREFS);
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

    @GetMapping(path = "{collection}/layer", produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public CollectionLayer getCollectionLayer(HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // query one collection and grab its OGC links
        final Name layerPropertyName = getCollectionLayerPropertyName();
        final PropertyName layerProperty = FF.property(layerPropertyName);
        Feature feature = queryCollection(collection, q -> {
            q.setProperties(Collections.singletonList(layerProperty));
        });

        CollectionLayer layer = buildCollectionLayerFromFeature(feature, true);
        return layer;
    }

    private Name getCollectionLayerPropertyName() throws IOException {
        String namespaceURI = getOpenSearchAccess().getCollectionSource().getSchema().getName()
                .getNamespaceURI();
        final Name layerPropertyName = new NameImpl(namespaceURI, OpenSearchAccess.LAYER);
        return layerPropertyName;
    }

    @PutMapping(path = "{collection}/layer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> putCollectionLayer(
            @PathVariable(name = "collection", required = true) String collection,
            @RequestBody CollectionLayer layer) throws IOException {
        // check the collection is there
        queryCollection(collection, q -> {
        });

        // validate the layer
        validateLayer(layer);

        // check if the layer was there
        CollectionLayer previousLayer = getCollectionLayer(collection);

        // prepare the update
        SimpleFeature layerCollectionFeature = beanToLayerCollectionFeature(layer);
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        final Name layerPropertyName = getCollectionLayerPropertyName();
        runTransactionOnCollectionStore(fs -> {
            fs.modifyFeatures(layerPropertyName, layerCollectionFeature, filter);
        });

        // this is done after the changes in the DB to make sure it's reading the same data
        // we are inserting (feature sources do not accept a transaction...)
        if (previousLayer != null) {
            removeMosaicAndLayer(previousLayer);
        }
        try {
            createMosaicAndLayer(collection, layer);
        } catch (Exception e) {
            // rethrow to make the transaction fail
            throw new RuntimeException(e);
        }

        // see if we have to return a creation or not
        if (previousLayer != null) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
    }

    private CollectionLayer getCollectionLayer(String collection) throws IOException {
        final Name layerPropertyName = getCollectionLayerPropertyName();
        final PropertyName layerProperty = FF.property(layerPropertyName);
        Feature feature = queryCollection(collection, q -> {
            q.setProperties(Collections.singletonList(layerProperty));
        });
        CollectionLayer layer = buildCollectionLayerFromFeature(feature, false);
        return layer;
    }

    private CollectionLayer buildCollectionLayerFromFeature(Feature feature,
            boolean notFoundOnEmpty) {
        CollectionLayer layer = CollectionLayer.buildCollectionLayerFromFeature(feature);
        if (layer == null && notFoundOnEmpty) {
            throw new ResourceNotFoundException();
        }

        return layer;
    }

    private void removeMosaicAndLayer(CollectionLayer previousLayer) throws IOException {
        // look for the layer and trace your way back to the store
        String name = previousLayer.getWorkspace() + ":" + previousLayer.getLayer();
        LayerInfo layerInfo = catalog.getLayerByName(name);
        if (layerInfo == null) {
            LOGGER.warning("Could not locate previous layer " + name
                    + ", skipping removal of old publishing configuration");
            return;
        }
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);

        // see if it has the style the code normally attaches
        StyleInfo si = layerInfo.getDefaultStyle();
        if (si.getWorkspace() != null
                && previousLayer.getWorkspace().equals(si.getWorkspace().getName())
                && previousLayer.getLayer().equals(si.getName())) {
            visitor.visit(si);
        }

        // move to the resource
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo == null) {
            LOGGER.warning("Located layer " + name
                    + ", but it references a dangling resource, cannot perform full removal, limiting"
                    + " to layer removal");
            visitor.visit(layerInfo);
            return;
        } else if (!(resourceInfo instanceof CoverageInfo)) {
            throw new RuntimeException("Unexpected, the old layer in configuration, " + name
                    + ", is not a coverage, bailing out");
        }
        // see if we have a store
        StoreInfo storeInfo = resourceInfo.getStore();
        if (storeInfo == null) {
            LOGGER.warning("Located layer " + name
                    + ", but it references a dangling store , cannot perform full removal, limiting"
                    + " to layer and resource removal");
            visitor.visit((CoverageInfo) resourceInfo);
            return;
        }

        // cascade delete
        visitor.visit((CoverageStoreInfo) storeInfo);

        // and remove the configuration files too
        GeoServerResourceLoader rl = catalog.getResourceLoader();
        final String relativePath = rl.get("data") + "/" + previousLayer.getWorkspace() + "/"
                + previousLayer.getLayer();
        Resource mosaicResource = rl.fromPath(relativePath);
        mosaicResource.delete();
    }

    /**
     * Validates the layer and throws appropriate exceptions in case mandatory bits are missing
     * 
     * @param layer
     * @param catalog2
     */
    private void validateLayer(CollectionLayer layer) {
        if (layer.getWorkspace() == null) {
            throw new RestException(
                    "Invalid layer configuration, workspace name is missing or null",
                    HttpStatus.BAD_REQUEST);
        }
        if (catalog.getWorkspaceByName(layer.getWorkspace()) == null) {
            throw new RestException("Invalid layer configuration, workspace '"
                    + layer.getWorkspace() + "' does not exist", HttpStatus.BAD_REQUEST);
        }
        if (layer.getLayer() == null) {
            throw new RestException("Invalid layer configuration, layer name is missing or null",
                    HttpStatus.BAD_REQUEST);
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
                throw new RestException("Invalid layer configuration, browse bands must be either "
                        + "one (gray) or three (RGB)", HttpStatus.BAD_REQUEST);
            } else if (layer.getBands().length > 0 && layer.getBrowseBands().length > 0
                    && !containedFully(layer.getBrowseBands(), layer.getBands())) {
                throw new RestException(
                        "Invalid layer configuration, browse bands contains entries "
                                + "that are not part of the bands declaration",
                        HttpStatus.BAD_REQUEST);
            }
        }
        if (layer.isHeterogeneousCRS()) {
            if (layer.getMosaicCRS() == null) {
                throw new RestException(
                        "Invalid layer configuration, mosaic is heterogeneous but the mosaic CRS is missing",
                        HttpStatus.BAD_REQUEST);
            }
        }
        if (layer.getMosaicCRS() != null) {
            try {
                CRS.decode(layer.getMosaicCRS());
            } catch (FactoryException e) {
                throw new RestException(
                        "Invalid mosaicCRS value, cannot be decoded: " + e.getMessage(),
                        HttpStatus.BAD_REQUEST);
            }
        }

    }

    private boolean containedFully(int[] browseBands, int[] bands) {
        for (int i = 0; i < browseBands.length; i++) {
            int bb = browseBands[i];
            boolean found = false;
            for (int j = 0; j < bands.length; j++) {
                if(bands[j] == bb) {
                    found = true;
                    break;
                }
            }
            
            if(!found) {
                return false;
            }
        }
        return true;
    }

    private void createMosaicAndLayer(String collection, CollectionLayer layer) throws Exception {
        GeoServerResourceLoader rl = catalog.getResourceLoader();

        // grab the target directory and create it if needed
        final String relativePath = "data/" + layer.getWorkspace() + "/" + layer.getLayer();
        Resource mosaicDirectory = rl.fromPath(relativePath);
        if (mosaicDirectory.getType() != Type.UNDEFINED) {
            mosaicDirectory.dir();
        }

        if (layer.isSeparateBands()) {
            configureSeparateBandsMosaic(collection, layer, relativePath, mosaicDirectory);
        } else {
            configureSimpleMosaic(collection, layer, relativePath, mosaicDirectory);
        }
    }

    private void configureSeparateBandsMosaic(String collection, CollectionLayer layerConfiguration,
            String relativePath, Resource mosaicDirectory) throws Exception {
        // get the namespace URI for the store
        final FeatureSource<FeatureType, Feature> collectionSource = getOpenSearchAccess()
                .getCollectionSource();
        final FeatureType schema = collectionSource.getSchema();
        final String nsURI = schema.getName().getNamespaceURI();

        // image mosaic won't automatically create the mosaic config for us in this case,
        // we have to setup both the mosaic property file and sample image for all bands
        for (int band : layerConfiguration.getBands()) {
            final String mosaicName = collection + OpenSearchAccess.BAND_LAYER_SEPARATOR + band;

            // get the sample granule
            File file = getSampleGranule(collection, nsURI, band, mosaicName);
            AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(file,
                    EXCLUDE_MOSAIC_HINTS);
            if (format == null) {
                throw new RestException("Could not find a coverage reader able to process "
                        + file.getAbsolutePath(), HttpStatus.PRECONDITION_FAILED);
            }
            ImageLayout imageLayout;
            double[] nativeResolution;
            AbstractGridCoverage2DReader reader = null;
            try {
                reader = format.getReader(file);
                if (reader == null) {
                    throw new RestException("Could not find a coverage reader able to process "
                            + file.getAbsolutePath(), HttpStatus.PRECONDITION_FAILED);
                }
                imageLayout = reader.getImageLayout();
                nativeResolution = reader.getResolutionLevels()[0];
            } finally {
                if (reader != null) {
                    reader.dispose();
                }
            }
            ImageReaderSpi spi = null;
            try (FileImageInputStream fis = new FileImageInputStream(file)) {
                ImageReader imageReader = ImageIOExt.getImageioReader(fis);
                if (imageReader != null) {
                    spi = imageReader.getOriginatingProvider();
                }
            }

            // the mosaic configuration
            Properties mosaicConfig = new Properties();
            mosaicConfig.put("Levels", nativeResolution[0] + "," + nativeResolution[1]);
            mosaicConfig.put("Heterogeneous", "true");
            mosaicConfig.put("AbsolutePath", "true");
            mosaicConfig.put("Name", "" + band);
            mosaicConfig.put("TypeName", mosaicName);
            mosaicConfig.put("TypeNames", "false"); // disable typename scanning
            mosaicConfig.put("Caching", "false");
            mosaicConfig.put("LocationAttribute", "location");
            mosaicConfig.put("TimeAttribute", TIME_START);
            mosaicConfig.put("CanBeEmpty", "true");
            if (spi != null) {
                mosaicConfig.put("SuggestedSPI", spi.getClass().getName());
            }
            // TODO: the index is now always in 4326, so the mosaic has to be heterogeneous
            // in general, unless we know the data is uniformly in something else, in that
            // case we could reproject the view reporting the footprints...
//            if (layerConfiguration.isHeterogeneousCRS()) {
                mosaicConfig.put("HeterogeneousCRS", "true");
                mosaicConfig.put("MosaicCRS", "EPSG:4326" /* layerConfiguration.getMosaicCRS() */);
                mosaicConfig.put("CrsAttribute", "crs");
//            }
            Resource propertyResource = mosaicDirectory.get(band + ".properties");
            try (OutputStream os = propertyResource.out()) {
                mosaicConfig.store(os, "DataStore configuration for collection '" + collection
                        + "' and band '" + band + "'");
            }

            // create the sample image
            Resource sampleImageResource = mosaicDirectory.get(band + Utils.SAMPLE_IMAGE_NAME);
            Utils.storeSampleImage(sampleImageResource.file(), imageLayout.getSampleModel(null),
                    imageLayout.getColorModel(null));
        }
        
        // this is ridicolous, but for the moment, multi-crs mosaics won't work if there
        // is no indexer.properties around, even if no collection is actually done
        buildIndexer(collection, mosaicDirectory);

        // mosaic datastore connection
        createDataStoreProperties(collection, mosaicDirectory);

        // the mosaic datastore itself
        CatalogBuilder cb = new CatalogBuilder(catalog);
        CoverageStoreInfo mosaicStoreInfo = createMosaicStore(cb, collection, layerConfiguration,
                relativePath);

        // and finally the layer, with a coverage view associated to it
        List<CoverageBand> coverageBands = buildCoverageBands(layerConfiguration);
        final String coverageName = layerConfiguration.getLayer();
        final CoverageView coverageView = new CoverageView(coverageName, coverageBands);
        CoverageInfo coverageInfo = coverageView.createCoverageInfo(coverageName, mosaicStoreInfo,
                cb);
        timeEnableResource(coverageInfo);
        final LayerInfo layerInfo = cb.buildLayer(coverageInfo);

        catalog.add(coverageInfo);
        catalog.add(layerInfo);

        // configure the style if needed
        createStyle(layerConfiguration, layerInfo);
    }

    private List<CoverageBand> buildCoverageBands(CollectionLayer collectionLayer) {
        List<CoverageBand> result = new ArrayList<>();
        int[] bands = collectionLayer.getBands();
        for (int i = 0; i < bands.length; i++) {
            int band = bands[i];
            CoverageBand cb = new CoverageBand(
                    Collections.singletonList(new InputCoverageBand("" + band, "0")), "B" + i, i,
                    CompositionType.BAND_SELECT);
            result.add(cb);
        }
        return result;
    }

    private File getSampleGranule(String collection, final String nsURI, int band,
            final String mosaicName) throws IOException {
        // make sure there is at least one granule to grab resolution, sample/color model,
        // and preferred SPI
        SimpleFeatureSource granuleSource = DataUtilities
                .simple(getOpenSearchAccess().getFeatureSource(new NameImpl(nsURI, mosaicName)));
        SimpleFeature firstFeature = DataUtilities.first(granuleSource.getFeatures());
        if (firstFeature == null) {
            throw new RestException("Could not locate any granule for collection '" + collection
                    + "' and band '" + band + "'", HttpStatus.EXPECTATION_FAILED);
        }
        // grab the file
        String location = (String) firstFeature.getAttribute("location");
        File file = new File(location);
        if (!file.exists()) {
            throw new RestException(
                    "Sample granule '" + location
                            + "' could not be found on the file system, check your database",
                    HttpStatus.EXPECTATION_FAILED);
        }

        return file;
    }

    private void configureSimpleMosaic(String collection, CollectionLayer layerConfiguration,
            final String relativePath, Resource mosaic) throws IOException, Exception {
        // make sure there is at least one granule
        final FeatureSource<FeatureType, Feature> collectionSource = getOpenSearchAccess()
                .getCollectionSource();
        final FeatureType schema = collectionSource.getSchema();
        final String nsURI = schema.getName().getNamespaceURI();
        final NameImpl fsName = new NameImpl(nsURI, collection);
        final FeatureSource<FeatureType, Feature> genericGranuleSource = getOpenSearchAccess()
                .getFeatureSource(fsName);
        SimpleFeatureSource granuleSource = DataUtilities.simple(genericGranuleSource);
        SimpleFeature firstFeature = DataUtilities.first(granuleSource.getFeatures());
        if (firstFeature == null) {
            throw new RestException(
                    "Cannot configure a mosaic, please add at least one product "
                            + "with granules in order to set it up",
                    HttpStatus.PRECONDITION_FAILED);
        }

        buildIndexer(collection, mosaic);

        createDataStoreProperties(collection, mosaic);

        CatalogBuilder cb = new CatalogBuilder(catalog);
        createMosaicStore(cb, collection, layerConfiguration, relativePath);

        // and then the layer
        CoverageInfo coverageInfo = cb.buildCoverage(collection);
        coverageInfo.setName(layerConfiguration.getLayer());
        timeEnableResource(coverageInfo);
        catalog.add(coverageInfo);
        LayerInfo layerInfo = cb.buildLayer(coverageInfo);
        catalog.add(layerInfo);

        // configure the style if needed
        createStyle(layerConfiguration, layerInfo);
    }

    private void buildIndexer(String collection, Resource mosaic) throws IOException {
        // prepare the mosaic configuration
        Properties indexer = new Properties();
        indexer.put("UseExistingSchema", "true");
        indexer.put("Name", collection);
        indexer.put("TypeName", collection);
        indexer.put("AbsolutePath", "true");
        indexer.put("TimeAttribute", TIME_START);
        // TODO: should we setup also a end time and prepare a interval based time setup?

        // TODO: the index is now always in 4326, so the mosaic has to be heterogeneous
        // in general, unless we know the data is uniformly in something else, in that
        // case we could reproject the view reporting the footprints...
        // if (layerConfiguration.isHeterogeneousCRS()) {
            indexer.put("HeterogeneousCRS", "true");
            indexer.put("MosaicCRS", "EPSG:4326" /* layerConfiguration.getMosaicCRS() */);
            indexer.put("CrsAttribute", "crs");
        // }
        Resource resource = mosaic.get("indexer.properties");
        try (OutputStream os = resource.out()) {
            indexer.store(os, "Indexer for collection: " + collection);
        }
    }

    /**
     * Enables the ResourceInfo time dimension, defaulting it to the highest available value 
     * TODO: it's probably useful to make this configurable 
     */
    private void timeEnableResource(ResourceInfo resource) {
        DimensionInfo dimension = new DimensionInfoImpl();
        dimension.setEnabled(true);
        dimension.setAttribute(TIME_START);
        dimension.setUnits(DimensionInfo.TIME_UNITS);
        dimension.setPresentation(DimensionPresentation.CONTINUOUS_INTERVAL);
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);
        dimension.setDefaultValue(defaultValueSetting);
        
        resource.getMetadata().put(ResourceInfo.TIME, dimension);
    }

    private void createStyle(CollectionLayer layerConfiguration, LayerInfo layerInfo)
            throws IOException {
        CoverageInfo ci = (CoverageInfo) layerInfo.getResource();
        int[] defaultBands = IntStream.rangeClosed(1,  ci.getDimensions().size()).toArray();
        final int[] browseBands = layerConfiguration.getBrowseBands();
        if (browseBands != null && browseBands.length > 0
                && !Arrays.equals(defaultBands, browseBands)) {
            RasterSymbolizerBuilder rsb = new RasterSymbolizerBuilder();
            if (browseBands.length == 1) {
                ChannelSelectionBuilder cs = rsb.channelSelection();
                cs.gray().channelName("" + browseBands[0]);
            } else if (browseBands.length == 3) {
                ChannelSelectionBuilder cs = rsb.channelSelection();
                cs.red().channelName("" + browseBands[0]);
                cs.green().channelName("" + browseBands[1]);
                cs.blue().channelName("" + browseBands[2]);
            }
            Style style = rsb.buildStyle();
            StyleInfo si = catalog.getFactory().createStyle();
            si.setFormat("SLD");
            si.setFormatVersion(new Version("1.0"));
            si.setName(layerInfo.getName());
            si.setWorkspace(catalog.getWorkspaceByName(layerConfiguration.getWorkspace()));
            si.setFilename(layerInfo.getName() + ".sld");
            catalog.getResourcePool().writeStyle(si, style);
            catalog.add(si);

            // associate style (we need a proxy instance, cannot use the original layerinfo)
            LayerInfo savedLayer = catalog.getLayer(layerInfo.getId());
            savedLayer.setDefaultStyle(si);
            catalog.save(savedLayer);
        }

    }

    private void createDataStoreProperties(String collection, Resource mosaic) throws IOException {
        // prepare the datastore.properties now
        // TODO : should we use the store identifier instead of the prefixed name? Would be
        // resilient across store renames
        Properties datastore = new Properties();
        datastore.put("StoreName", prefixedName(getOpenSearchStoreInfo()));
        Resource datastoreResource = mosaic.get("datastore.properties");
        try (OutputStream os = datastoreResource.out()) {
            datastore.store(os, "DataStore configuration for collection: " + collection);
        }
    }

    private CoverageStoreInfo createMosaicStore(CatalogBuilder cb, String collection,
            CollectionLayer layer, final String relativePath) {
        // good to go, create the store
        cb.setWorkspace(catalog.getWorkspace(layer.getWorkspace()));
        CoverageStoreInfo mosaicStore = cb.buildCoverageStore(layer.getLayer());
        mosaicStore.setType(new ImageMosaicFormat().getName());
        mosaicStore.setDescription("Image mosaic wrapping OpenSearch collection: " + collection);
        mosaicStore.setURL("file:/" + relativePath);
        catalog.add(mosaicStore);
        cb.setStore(mosaicStore);

        return mosaicStore;
    }

    private Object prefixedName(DataStoreInfo info) {
        String ws = info.getWorkspace().getName();
        String name = info.getName();
        return ws + ":" + name;
    }

    @DeleteMapping(path = "{collection}/layer")
    public void deleteCollectionLayer(
            @PathVariable(name = "collection", required = true) String collection)
            throws IOException {
        // check the collection is there
        queryCollection(collection, q -> {
        });

        // prepare the update
        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        final Name layerPropertyName = getCollectionLayerPropertyName();
        runTransactionOnCollectionStore(fs -> {
            CollectionLayer previousLayer = getCollectionLayer(collection);

            // remove from DB
            fs.modifyFeatures(layerPropertyName, null, filter);

            // remove from configuration if needed
            if (previousLayer != null) {
                removeMosaicAndLayer(previousLayer);
            }
        });
    }

    private SimpleFeature beanToLayerCollectionFeature(CollectionLayer layer) throws IOException {
        Name collectionLayerPropertyName = getCollectionLayerPropertyName();
        PropertyDescriptor pd = getOpenSearchAccess().getCollectionSource().getSchema()
                .getDescriptor(collectionLayerPropertyName);
        SimpleFeatureType schema = (SimpleFeatureType) pd.getType();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        fb.set("workspace", layer.getWorkspace());
        fb.set("layer", layer.getLayer());
        fb.set("separateBands", layer.isSeparateBands());
        fb.set("bands", layer.getBands());
        fb.set("browseBands", layer.getBrowseBands());
        fb.set("heterogeneousCRS", layer.isHeterogeneousCRS());
        fb.set("mosaicCRS", layer.getMosaicCRS());
        SimpleFeature sf = fb.buildFeature(null);
        return sf;
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

    @PutMapping(path = "{collection}/ogcLinks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void putCollectionOgcLinks(HttpServletRequest request,
            @PathVariable(name = "collection", required = true) String collection,
            @RequestBody OgcLinks links) throws IOException {
        // check the collection is there
        queryCollection(collection, q -> {
        });

        ListFeatureCollection linksCollection = beansToLinksCollection(links);

        Filter filter = FF.equal(FF.property(COLLECTION_ID), FF.literal(collection), true);
        runTransactionOnCollectionStore(fs -> fs
                .modifyFeatures(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME, linksCollection, filter));
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
            fs.modifyFeatures(new NameImpl(nsURI, OpenSearchAccess.DESCRIPTION), description,
                    filter);
        });
    }

    private void runTransactionOnCollectionStore(IOConsumer<FeatureStore> featureStoreConsumer)
            throws IOException {
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

}

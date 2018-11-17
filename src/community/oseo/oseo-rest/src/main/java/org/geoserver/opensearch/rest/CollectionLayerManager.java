/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.ImageLayout;
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
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.rest.RestException;
import org.geotools.brewer.styling.builder.ChannelSelectionBuilder;
import org.geotools.brewer.styling.builder.RasterSymbolizerBuilder;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.image.io.ImageIOExt;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.util.Version;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;

/**
 * Helper class with the responsibility of setting up and tearing down mosaics, coverage views,
 * layers and styles for the collections.
 *
 * @author Andrea Aime - GeoSolutions
 */
class CollectionLayerManager {
    static final Logger LOGGER = Logging.getLogger(CollectionLayerManager.class);
    private static final String TIME_START = "timeStart";
    private static final String TIME_START_END = "timeStart;timeEnd";
    static final Hints EXCLUDE_MOSAIC_HINTS = new Hints(Utils.EXCLUDE_MOSAIC, true);
    public static final Pattern BAND_SPEC_PATTERN = Pattern.compile("([^\\[]+)(\\[(\\d+)\\])?");

    Catalog catalog;

    OpenSearchAccessProvider accessProvider;

    public CollectionLayerManager(Catalog catalog, OpenSearchAccessProvider accessProvider) {
        this.catalog = catalog;
        this.accessProvider = accessProvider;
    }

    void removeMosaicAndLayer(CollectionLayer previousLayer) throws IOException {
        // look for the layer and trace your way back to the store
        String name = previousLayer.getWorkspace() + ":" + previousLayer.getLayer();
        LayerInfo layerInfo = catalog.getLayerByName(name);
        if (layerInfo == null) {
            LOGGER.warning(
                    "Could not locate previous layer "
                            + name
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
            LOGGER.warning(
                    "Located layer "
                            + name
                            + ", but it references a dangling resource, cannot perform full removal, limiting"
                            + " to layer removal");
            visitor.visit(layerInfo);
            return;
        } else if (!(resourceInfo instanceof CoverageInfo)) {
            throw new RuntimeException(
                    "Unexpected, the old layer in configuration, "
                            + name
                            + ", is not a coverage, bailing out");
        }
        // see if we have a store
        StoreInfo storeInfo = resourceInfo.getStore();
        if (storeInfo == null) {
            LOGGER.warning(
                    "Located layer "
                            + name
                            + ", but it references a dangling store , cannot perform full removal, limiting"
                            + " to layer and resource removal");
            visitor.visit((CoverageInfo) resourceInfo);
            return;
        }

        // cascade delete
        visitor.visit((CoverageStoreInfo) storeInfo);

        // and remove the configuration files too
        GeoServerResourceLoader rl = catalog.getResourceLoader();
        final String relativePath =
                rl.get("data")
                        + "/"
                        + previousLayer.getWorkspace()
                        + "/"
                        + previousLayer.getLayer();
        Resource mosaicResource = rl.fromPath(relativePath);
        mosaicResource.delete();
    }

    void createMosaicAndLayer(String collection, CollectionLayer layer) throws Exception {
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

    private void configureSeparateBandsMosaic(
            String collection,
            CollectionLayer layerConfiguration,
            String relativePath,
            Resource mosaicDirectory)
            throws Exception {
        // get the namespace URI for the store
        final FeatureSource<FeatureType, Feature> collectionSource =
                this.accessProvider.getOpenSearchAccess().getCollectionSource();
        final FeatureType schema = collectionSource.getSchema();
        final String nsURI = schema.getName().getNamespaceURI();

        // image mosaic won't automatically create the mosaic config for us in this case,
        // we have to setup both the mosaic property file and sample image for all bands
        for (String band : layerConfiguration.getBands()) {
            final String mosaicName = collection + OpenSearchAccess.BAND_LAYER_SEPARATOR + band;

            // get the sample granule
            File file = getSampleGranule(collection, nsURI, band, mosaicName);
            AbstractGridFormat format =
                    (AbstractGridFormat) GridFormatFinder.findFormat(file, EXCLUDE_MOSAIC_HINTS);
            if (format == null) {
                throw new RestException(
                        "Could not find a coverage reader able to process "
                                + file.getAbsolutePath(),
                        HttpStatus.PRECONDITION_FAILED);
            }
            ImageLayout imageLayout;
            double[] nativeResolution;
            AbstractGridCoverage2DReader reader = null;
            try {
                reader = format.getReader(file);
                if (reader == null) {
                    throw new RestException(
                            "Could not find a coverage reader able to process "
                                    + file.getAbsolutePath(),
                            HttpStatus.PRECONDITION_FAILED);
                }
                imageLayout = reader.getImageLayout();
                double[][] resolutionLevels =
                        getResolutionLevelsInCRS(reader, DefaultGeographicCRS.WGS84);
                nativeResolution = resolutionLevels[0];
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
            if (layerConfiguration.isTimeRanges()) {
                mosaicConfig.put("TimeAttribute", TIME_START_END);
            } else {
                mosaicConfig.put("TimeAttribute", TIME_START);
            }
            mosaicConfig.put("CanBeEmpty", "true");
            if (spi != null) {
                mosaicConfig.put("SuggestedSPI", spi.getClass().getName());
            }
            // TODO: the index is now always in 4326, so the mosaic has to be heterogeneous
            // in general, unless we know the data is uniformly in something else, in that
            // case we could reproject the view reporting the footprints...
            if (layerConfiguration.isHeterogeneousCRS()) {
                mosaicConfig.put("HeterogeneousCRS", "true");
                mosaicConfig.put("MosaicCRS", "EPSG:4326");
                mosaicConfig.put("CrsAttribute", "crs");
            }
            Resource propertyResource = mosaicDirectory.get(band + ".properties");
            try (OutputStream os = propertyResource.out()) {
                mosaicConfig.store(
                        os,
                        "DataStore configuration for collection '"
                                + collection
                                + "' and band '"
                                + band
                                + "'");
            }

            // create the sample image
            Resource sampleImageResource = mosaicDirectory.get(band + Utils.SAMPLE_IMAGE_NAME);
            Utils.storeSampleImage(
                    sampleImageResource.file(),
                    imageLayout.getSampleModel(null),
                    imageLayout.getColorModel(null));
        }

        // this is ridiculous, but for the moment, multi-crs mosaics won't work if there
        // is no indexer.properties around, even if no collection is actually done
        buildIndexer(collection, layerConfiguration, mosaicDirectory);

        // mosaic datastore connection
        createDataStoreProperties(collection, mosaicDirectory);

        // the mosaic datastore itself
        CatalogBuilder cb = new CatalogBuilder(catalog);
        CoverageStoreInfo mosaicStoreInfo =
                createMosaicStore(cb, collection, layerConfiguration, relativePath);

        // and finally the layer, with a coverage view associated to it
        List<CoverageBand> coverageBands =
                buildCoverageBands(mosaicStoreInfo, layerConfiguration.getBands());
        final String coverageName = layerConfiguration.getLayer();
        final CoverageView coverageView = new CoverageView(coverageName, coverageBands);
        CoverageInfo coverageInfo =
                coverageView.createCoverageInfo(coverageName, mosaicStoreInfo, cb);
        timeEnableResource(coverageInfo);
        final LayerInfo layerInfo = cb.buildLayer(coverageInfo);

        catalog.add(coverageInfo);
        catalog.add(layerInfo);

        // configure the style if needed
        createStyle(layerConfiguration, layerInfo, mosaicStoreInfo);
    }

    private double[][] getResolutionLevelsInCRS(
            GridCoverage2DReader reader, CoordinateReferenceSystem targetCRS)
            throws FactoryException, TransformException, IOException {

        double[][] resolutionLevels = reader.getResolutionLevels();
        CoordinateReferenceSystem readerCRS = reader.getCoordinateReferenceSystem();
        GeneralEnvelope sourceEnvelope = reader.getOriginalEnvelope();

        // prepare a set of points at middle of the envelope and their
        // corresponding offsets based on resolutions
        final int numLevels = resolutionLevels.length;
        double[] points = new double[numLevels * 8];
        double baseX = sourceEnvelope.getMedian(0);
        double baseY = sourceEnvelope.getMedian(1);
        for (int i = 0, j = 0; i < numLevels; i++) {
            // delta x point
            points[j++] = baseX;
            points[j++] = baseY;
            points[j++] = baseX + resolutionLevels[i][0];
            points[j++] = baseY;
            // delta y point
            points[j++] = baseX;
            points[j++] = baseY;
            points[j++] = baseX;
            points[j++] = baseY + resolutionLevels[i][1];
        }

        // transform to get offsets in the target CRS
        MathTransform mt = CRS.findMathTransform(readerCRS, targetCRS);
        mt.transform(points, 0, points, 0, numLevels * 4);

        // compute back the offsets
        double[][] result = new double[numLevels][2];
        for (int i = 0; i < numLevels; i++) {
            result[i][0] = distance(points, i * 8);
            result[i][1] = distance(points, i * 8 + 4);
        }
        return result;
    }

    private double distance(double[] points, int base) {
        double dx = points[base + 2] - points[base];
        double dy = points[base + 3] - points[base + 1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    private List<CoverageBand> buildCoverageBands(CoverageStoreInfo mosaicStoreInfo, String[] bands)
            throws IOException {
        // get the coverage names for validation purposes
        Set<String> coverageNames = new LinkedHashSet<>();
        GridCoverage2DReader reader =
                (GridCoverage2DReader) mosaicStoreInfo.getGridCoverageReader(null, null);
        coverageNames.addAll(Arrays.asList(reader.getGridCoverageNames()));

        // go through all the band selection specs
        List<CoverageBand> result = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < bands.length; i++) {
            String band = bands[i];

            // the band name could be a straight coverage name, or refer to specific band indexes
            Matcher matcher = BAND_SPEC_PATTERN.matcher(band);
            if (!matcher.matches()) {
                throw new RestException(
                        "Invalid band name specification, should be a band name as "
                                + "a string without square brackets, or bandName[idx], but was "
                                + band,
                        HttpStatus.BAD_REQUEST);
            }
            String coverageName = matcher.group(1);
            if (!coverageNames.contains(coverageName)) {
                throw new RestException(
                        "Could not find coverage named "
                                + coverageName
                                + ", the available ones are "
                                + coverageNames,
                        HttpStatus.BAD_REQUEST);
            }
            SampleModel sm = reader.getImageLayout(coverageName).getSampleModel(null);

            String bandIndexSpec = matcher.group(3);
            if (bandIndexSpec == null) {
                // getting all the bands in the coverage, how many do we have?
                for (int b = 0; b < sm.getNumBands(); b++) {
                    CoverageBand cb =
                            new CoverageBand(
                                    Collections.singletonList(
                                            new InputCoverageBand(coverageName, String.valueOf(b))),
                                    coverageName + ((sm.getNumBands() == 1) ? "" : "_" + b),
                                    j++,
                                    CompositionType.BAND_SELECT);
                    result.add(cb);
                }
            } else {
                CoverageBand cb =
                        new CoverageBand(
                                Collections.singletonList(
                                        new InputCoverageBand(
                                                coverageName, String.valueOf(bandIndexSpec))),
                                coverageName + "_" + bandIndexSpec,
                                j++,
                                CompositionType.BAND_SELECT);
                result.add(cb);
            }
        }
        return result;
    }

    private File getSampleGranule(
            String collection, final String nsURI, String band, final String mosaicName)
            throws IOException {
        // make sure there is at least one granule to grab resolution, sample/color model,
        // and preferred SPI
        SimpleFeatureSource granuleSource =
                DataUtilities.simple(
                        this.accessProvider
                                .getOpenSearchAccess()
                                .getFeatureSource(new NameImpl(nsURI, mosaicName)));
        SimpleFeature firstFeature = DataUtilities.first(granuleSource.getFeatures());
        if (firstFeature == null) {
            throw new RestException(
                    "Could not locate any granule for collection '"
                            + collection
                            + "' and band '"
                            + band
                            + "'",
                    HttpStatus.EXPECTATION_FAILED);
        }
        // grab the file
        String location = (String) firstFeature.getAttribute("location");
        File file = new File(location);
        if (!file.exists()) {
            throw new RestException(
                    "Sample granule '"
                            + location
                            + "' could not be found on the file system, check your database",
                    HttpStatus.EXPECTATION_FAILED);
        }

        return file;
    }

    private void configureSimpleMosaic(
            String collection,
            CollectionLayer layerConfiguration,
            final String relativePath,
            Resource mosaic)
            throws IOException, Exception {
        // make sure there is at least one granule
        final FeatureSource<FeatureType, Feature> collectionSource =
                this.accessProvider.getOpenSearchAccess().getCollectionSource();
        final FeatureType schema = collectionSource.getSchema();
        final String nsURI = schema.getName().getNamespaceURI();
        final NameImpl fsName = new NameImpl(nsURI, collection);
        final FeatureSource<FeatureType, Feature> genericGranuleSource =
                this.accessProvider.getOpenSearchAccess().getFeatureSource(fsName);
        SimpleFeatureSource granuleSource = DataUtilities.simple(genericGranuleSource);
        SimpleFeature firstFeature = DataUtilities.first(granuleSource.getFeatures());
        if (firstFeature == null) {
            throw new RestException(
                    "Cannot configure a mosaic, please add at least one product "
                            + "with granules in order to set it up",
                    HttpStatus.PRECONDITION_FAILED);
        }

        buildIndexer(collection, layerConfiguration, mosaic);

        createDataStoreProperties(collection, mosaic);

        CatalogBuilder cb = new CatalogBuilder(catalog);
        CoverageStoreInfo mosaicStore =
                createMosaicStore(cb, collection, layerConfiguration, relativePath);

        // and then the layer
        CoverageInfo coverageInfo = cb.buildCoverage(layerConfiguration.getLayer());
        coverageInfo.setName(layerConfiguration.getLayer());
        timeEnableResource(coverageInfo);
        catalog.add(coverageInfo);
        LayerInfo layerInfo = cb.buildLayer(coverageInfo);
        catalog.add(layerInfo);

        // configure the style if needed
        createStyle(layerConfiguration, layerInfo, mosaicStore);
    }

    private void buildIndexer(
            String collection, CollectionLayer layerConfiguration, Resource mosaic)
            throws IOException {
        // prepare the mosaic configuration
        Properties indexer = new Properties();
        indexer.put("UseExistingSchema", "true");
        indexer.put("Name", layerConfiguration.getLayer());
        indexer.put("TypeName", collection);
        indexer.put("AbsolutePath", "true");
        if (layerConfiguration.isTimeRanges()) {
            indexer.put("TimeAttribute", TIME_START_END);
        } else {
            indexer.put("TimeAttribute", TIME_START);
        }
        // TODO: should we setup also a end time and prepare a interval based time setup?

        // TODO: the index is now always in 4326, so the mosaic has to be heterogeneous
        // in general, unless we know the data is uniformly in something else, in that
        // case we could reproject the view reporting the footprints...
        if (layerConfiguration.isHeterogeneousCRS()) {
            indexer.put("HeterogeneousCRS", "true");
            indexer.put("MosaicCRS", "EPSG:4326");
            indexer.put("CrsAttribute", "crs");
        }
        Resource resource = mosaic.get("indexer.properties");
        try (OutputStream os = resource.out()) {
            indexer.store(os, "Indexer for collection: " + collection);
        }
    }

    /**
     * Enables the ResourceInfo time dimension, defaulting it to the highest available value TODO:
     * it's probably useful to make this configurable
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

    private void createStyle(
            CollectionLayer layerConfiguration,
            LayerInfo layerInfo,
            CoverageStoreInfo mosaicStoreInfo)
            throws IOException {
        CoverageInfo ci = (CoverageInfo) layerInfo.getResource();
        // get the band making up the layer, if not found, use the native ones
        String[] bandSpecs = layerConfiguration.getBands();
        if (bandSpecs == null) {
            bandSpecs = mosaicStoreInfo.getGridCoverageReader(null, null).getGridCoverageNames();
        }
        final String[] browseBandSpecs = layerConfiguration.getBrowseBands();
        if (browseBandSpecs != null
                && browseBandSpecs.length > 0
                && !Arrays.equals(bandSpecs, browseBandSpecs)) {

            RasterSymbolizerBuilder rsb = new RasterSymbolizerBuilder();

            List<CoverageBand> coverageBands = buildCoverageBands(mosaicStoreInfo, bandSpecs);
            List<CoverageBand> browseBands = buildCoverageBands(mosaicStoreInfo, browseBandSpecs);

            if (browseBands.size() == 1) {
                ChannelSelectionBuilder cs = rsb.channelSelection();
                cs.gray().channelName("" + getBandIndex(browseBands.get(0), coverageBands));
            } else if (browseBands.size() == 3) {
                ChannelSelectionBuilder cs = rsb.channelSelection();
                cs.red().channelName("" + getBandIndex(browseBands.get(0), coverageBands));
                cs.green().channelName("" + getBandIndex(browseBands.get(1), coverageBands));
                cs.blue().channelName("" + getBandIndex(browseBands.get(2), coverageBands));
            } else {
                throw new RestException(
                        "Browse bands should select either 1 or 3 bands, but instead they created "
                                + +browseBands.size()
                                + " raster bands",
                        HttpStatus.PRECONDITION_FAILED);
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

    private int getBandIndex(CoverageBand band, List<CoverageBand> bands) {
        // lookup the band order in the split multiband case
        for (int i = 0; i < bands.size(); i++) {
            if (band.equals(bands.get(i))) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException(
                "Could not find browse band " + band + " among the layer bands " + bands);
    }

    private void createDataStoreProperties(String collection, Resource mosaic) throws IOException {
        // prepare the datastore.properties now
        // TODO : should we use the store identifier instead of the prefixed name? Would be
        // resilient across store renames
        Properties datastore = new Properties();
        datastore.put("StoreName", prefixedName(this.accessProvider.getDataStoreInfo()));
        Resource datastoreResource = mosaic.get("datastore.properties");
        try (OutputStream os = datastoreResource.out()) {
            datastore.store(os, "DataStore configuration for collection: " + collection);
        }
    }

    private CoverageStoreInfo createMosaicStore(
            CatalogBuilder cb,
            String collection,
            CollectionLayer layer,
            final String relativePath) {
        // good to go, create the store
        cb.setWorkspace(catalog.getWorkspaceByName(layer.getWorkspace()));
        CoverageStoreInfo mosaicStore = cb.buildCoverageStore(layer.getLayer());
        mosaicStore.setType(new ImageMosaicFormat().getName());
        mosaicStore.setDescription("Image mosaic wrapping OpenSearch collection: " + collection);
        mosaicStore.setURL("file:" + relativePath);
        catalog.add(mosaicStore);
        cb.setStore(mosaicStore);

        return mosaicStore;
    }

    private Object prefixedName(DataStoreInfo info) {
        String ws = info.getWorkspace().getName();
        String name = info.getName();
        return ws + ":" + name;
    }
}

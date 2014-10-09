/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.ImageLayout;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.coverage.configuration.CoverageConfiguration;
import org.geoserver.coverage.layer.CoverageTileLayer;
import org.geoserver.coverage.layer.CoverageTileLayerInfo;
import org.geoserver.coverage.layer.CoverageTileLayerInfoImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.HarvestedSource;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * A {@link GridCoverage2DReader} implementation doing caching of read
 * {@link GridCoverage2D}.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Nicola Lagomarsini, GeoSolutions SAS
 *
 */
public class CachingGridCoverage2DReader implements GridCoverage2DReader {

    private final static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(CachingGridCoverage2DReader.class);

    private ISO8601Formatter formatter = new ISO8601Formatter();

    /** The underlying gridCoverageReader */
    private GridCoverage2DReader delegate;

    /** A TileLayer instance associated to the coverage served by this reader */
    private CoverageTileLayer coverageTileLayer;

    /** The configuration referencing brokers and temp folder */
    private GridCoveragesCache cache;

    private List<GridSubset> gridSubSets;

    private GridSet defaultGridSet;

    private CoverageTileComposer composer;
    private boolean axisOrderingTopDown;

    private final static int BASE_LEVEL = 2;

    private final static long[] FIRST_Y_TILE = { 0, 0, BASE_LEVEL };

    private final static long[] SECOND_Y_TILE = { 0, 1, BASE_LEVEL };

    public static CachingGridCoverage2DReader wrap(ResourcePool pool, GridCoveragesCache cache,
            CoverageInfo info, String coverageName, Hints hints) throws IOException {
        Hints localHints = null;

        // Set hints to exclude gridCoverage extensions lookup to go through
        // the standard gridCoverage reader lookup on ResourcePool
        Hints newHints = new Hints(ResourcePool.SKIP_COVERAGE_EXTENSIONS_LOOKUP, true);
        if (hints != null) {
            localHints = hints.clone();
            localHints.add(newHints);
        } else {
            localHints = newHints;
        }
        GridCoverage2DReader delegate = (GridCoverage2DReader) pool.getGridCoverageReader(info,
                coverageName, localHints);
        if (delegate instanceof StructuredGridCoverage2DReader) {
            return new CachingStructuredGridCoverage2DReader(cache, info,
                    (StructuredGridCoverage2DReader) delegate);
        } else {
            return new CachingGridCoverage2DReader(cache, info, delegate);
        }

    }

    /**
     * Main constructor for {@link CachingGridCoverage2DReader}
     * 
     * @param cache the cache configuration
     * @param info a {@link CoverageInfo} instance
     * @param reader the underlying {@link GridCoverage2DReader} used to return reader properties.
     */
    public CachingGridCoverage2DReader(GridCoveragesCache cache,
            CoverageInfo info, GridCoverage2DReader reader) {
        this.cache = cache;
        try {
            delegate = reader;
            String coverageName = info.getNativeCoverageName();
            if (coverageName == null) {
                coverageName = info.getName();
            }
            ImageLayout layout = reader.getImageLayout(coverageName);

            // Getting the Metadata Map
            CoverageTileLayerInfo tlInfo = info.getMetadata().get(CachingGridCoverageReaderCallback.COVERAGETILELAYERINFO_KEY, CoverageTileLayerInfoImpl.class);
            gridSubSets = CoverageConfiguration.parseGridSubsets(cache.getGridSetBroker(), tlInfo);
            if (gridSubSets != null && !gridSubSets.isEmpty()) {
                defaultGridSet = gridSubSets.get(0).getGridSet();
            }
            coverageTileLayer = (CoverageTileLayer) GWC.get().getTileLayerByName(tlInfo.getName());
            coverageTileLayer.setLayout(layout);
            composer = new CoverageTileComposer(coverageTileLayer);
            axisOrderingTopDown = axisOrderingTopDown(defaultGridSet);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * This method checks if the Gridset Y axis order increases from top to bottom.
     * @param gridSet 
     * 
     * @param gridSet
    * @return
     */
    private boolean axisOrderingTopDown(GridSet gridSet) {
        GridSubset subset = GridSubsetFactory.createGridSubSet(gridSet,
                gridSet.getOriginalExtent(), BASE_LEVEL, BASE_LEVEL);
        BoundingBox b1 = subset.boundsFromIndex(FIRST_Y_TILE);
        BoundingBox b2 = subset.boundsFromIndex(SECOND_Y_TILE);
        return b2.getMinX() < b1.getMinX();
    }

    @Override
    public Format getFormat() {
        return delegate.getFormat();
    }

    @Override
    public Object getSource() {
        return delegate.getSource();
    }

    @Override
    public String[] getMetadataNames() throws IOException {
        return delegate.getMetadataNames();
    }

    @Override
    public String[] getMetadataNames(String coverageName) throws IOException {
        return delegate.getMetadataNames(coverageName);
    }

    @Override
    public String getMetadataValue(String name) throws IOException {
        return delegate.getMetadataValue(name);
    }

    @Override
    public String getMetadataValue(String coverageName, String name) throws IOException {
        return delegate.getMetadataValue(coverageName, name);
    }

    @Override
    public String[] listSubNames() throws IOException {
        return delegate.listSubNames();
    }

    @Override
    public String[] getGridCoverageNames() throws IOException {
        return delegate.getGridCoverageNames();
    }

    @Override
    public int getGridCoverageCount() throws IOException {
        return delegate.getGridCoverageCount();
    }

    @Override
    public String getCurrentSubname() throws IOException {
        return delegate.getCurrentSubname();
    }

    @Override
    public boolean hasMoreGridCoverages() throws IOException {
        return delegate.hasMoreGridCoverages();
    }

    @Override
    public void skip() throws IOException {
        delegate.skip();
    }

    @Override
    public void dispose() throws IOException {
        delegate.dispose();

    }

    @Override
    public GeneralEnvelope getOriginalEnvelope() {
        return delegate.getOriginalEnvelope();
    }

    @Override
    public GeneralEnvelope getOriginalEnvelope(String coverageName) {
        return delegate.getOriginalEnvelope(coverageName);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        return delegate.getCoordinateReferenceSystem(coverageName);
    }

    @Override
    public GridEnvelope getOriginalGridRange() {
        return delegate.getOriginalGridRange();
    }

    @Override
    public GridEnvelope getOriginalGridRange(String coverageName) {
        return delegate.getOriginalGridRange(coverageName);
    }

    @Override
    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(pixInCell);
    }

    @Override
    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(coverageName, pixInCell);
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return delegate.getDynamicParameters();
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        return delegate.getDynamicParameters(coverageName);
    }

    @Override
    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(policy, requestedResolution);
    }

    @Override
    public double[] getReadingResolutions(String coverageName, OverviewPolicy policy,
            double[] requestedResolution) throws IOException {
        return delegate.getReadingResolutions(coverageName, policy, requestedResolution);
    }

    @Override
    public int getNumOverviews() {
        return delegate.getNumOverviews();
    }

    @Override
    public int getNumOverviews(String coverageName) {
        return delegate.getNumOverviews(coverageName);
    }

    @Override
    public ImageLayout getImageLayout() throws IOException {
        return delegate.getImageLayout();
    }

    @Override
    public ImageLayout getImageLayout(String coverageName) throws IOException {
        return delegate.getImageLayout(coverageName);
    }

    @Override
    public double[][] getResolutionLevels() throws IOException {
        return delegate.getResolutionLevels();
    }

    @Override
    public double[][] getResolutionLevels(String coverageName) throws IOException {
        return delegate.getResolutionLevels(coverageName);
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IOException {
        return read(null, parameters);
    }

    @Override
    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IOException {

        try {

            // TODO: Extract the current gridSet from some vendor parameter
            final GridSet gridSet = defaultGridSet;

            // Getting requested gridGeometry and envelope
            final GridGeometry2D gridGeometry = extractEnvelope(parameters);
            Envelope requestedEnvelope = null;
            if (gridGeometry != null) {
                requestedEnvelope = gridGeometry.getEnvelope();
            }
            if (requestedEnvelope == null) {
                requestedEnvelope = getOriginalEnvelope(coverageName);
            }

            ReferencedEnvelope env = new ReferencedEnvelope(requestedEnvelope);
            BoundingBox bbox = new BoundingBox(env.getMinX(), env.getMinY(), env.getMaxX(),
                    env.getMaxY());

            // Finding tiles involved by the request
            GridEnvelope2D gridEnv = null;
            if (gridGeometry != null) {
                gridEnv = gridGeometry.getGridRange2D();
            }

            if (gridEnv == null) {
                gridEnv = (GridEnvelope2D) getOriginalGridRange();
            }

            //TODO: customize this behaviour by parsing read params and 
            // getting the gridset to be used from there
            Integer zoomLevel = findClosestZoom(gridSet, env, gridEnv.width);
            long[] tiles = gridSubSets.get(0).getCoverageIntersection(zoomLevel, bbox);// TODO CHANGE HERE
            Map<String, String> filteringParameters = extractParameters(parameters);

            // // 
            // Getting tiles
            // //
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Getting tiles");
            }
            Map<String, ConveyorTile> cTiles = composer.getTiles(tiles, gridSet,
                    cache.getStorageBroker(), filteringParameters);

            // //
            // Composing a gridCoverage
            // //
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Composing the gridCoverage");
            }
            GridCoverage2D gridCoverage = composer.composeGridCoverage(coverageName, tiles, cTiles,
                    gridSet, requestedEnvelope, zoomLevel, axisOrderingTopDown);
            tiles = null;
            return gridCoverage;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Extract conveyor tile parameters from the request
     * @param parameters
     * @return
     * @throws IOException
     */
    private Map<String, String> extractParameters(GeneralParameterValue[] parameters) throws IOException {
        Map<String, String> params = null;
        Set<ParameterDescriptor<List>> dynamicParams = delegate.getDynamicParameters();
        for (GeneralParameterValue gParam : parameters) {
            GeneralParameterDescriptor descriptor = gParam.getDescriptor();
            final ReferenceIdentifier name = descriptor.getName();

            if (name.equals(AbstractGridFormat.TIME.getName())) {
                // TIME management
                if (gParam instanceof ParameterValue<?>) {
                    final ParameterValue<?> param = (ParameterValue<?>) gParam;
                    final Object value = param.getValue();
                    List times = (List)value;
                    Object object = times.get(0);
                    String timeValue = null;
                    if (object instanceof Date) {
                        Date date = (Date) object;
                        timeValue = formatter.format(date);
                    } else if (object instanceof DateRange) {
                        DateRange dateRange = (DateRange) object;
                        Date min = dateRange.getMinValue();
                        Date max = dateRange.getMaxValue();
                        boolean sameDate = min.compareTo(max) == 0;
                        timeValue = formatter.format(min)
                                + (sameDate ? "" : ("/" + formatter.format(max)));
                    }
                    if (params == null) {
                        params = new HashMap<String, String>();
                    }
                    
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Time param found: " + timeValue);
                    }
                    params.put(WCSSourceHelper.TIME, timeValue);
                }
            } else if (name.equals(AbstractGridFormat.ELEVATION.getName())) {
                // ELEVATION management
                if (gParam instanceof ParameterValue<?>) {
                    final ParameterValue<?> param = (ParameterValue<?>) gParam;
                    final Object value = param.getValue();
                    List elevations = (List)value;
                    Object object = elevations .get(0);
                    String elevationValue = null;
                    if (object instanceof Number) {
                        Number elevation = (Number) object;
                        elevationValue = elevation.toString();
                    } else if (object instanceof NumberRange) {
                        NumberRange elevationRange = (NumberRange) object;
                        elevationValue = elevationRange.getMinValue() +"/"+ elevationRange.getMaxValue();
                    }
                    if (params == null) {
                        params = new HashMap<String, String>();
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Elevation param found: " + elevationValue);
                    }
                    params.put(WCSSourceHelper.ELEVATION, elevationValue);
                }
            } else {
                // CUSTOM DIMENSION management
                for (ParameterDescriptor dynamicDescriptor : dynamicParams) {
                    if (name.equals(dynamicDescriptor.getName())) {
                        if (gParam instanceof ParameterValue<?>) {
                            final ParameterValue<?> param = (ParameterValue<?>) gParam;
                            final Object value = param.getValue();
                            List objValue = (List)value;
                            if (params == null) {
                                params = new HashMap<String, String>();
                            }
                            final Object customObject = objValue.get(0);
                            String customValue = null;
                            if (customObject instanceof String) {
                                customValue = (String) customObject;
                            } else if (customObject instanceof Number) {
                                customValue = ((Number)customObject).toString();
                            }
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine("custom param " + name + " found: " + customValue);
                            }
                            params.put(name.getCode(), customValue);
                        }
                    }
                }
            }
        }
        return params;
    }

    /**
     * This method returns the closest zoom level for the requested BBOX and resolution
     * 
     * @param gridSet
     * @param env
     * @param width
     * @return closest zoom level to the requested resolution
     */
    private Integer findClosestZoom(GridSet gridSet, ReferencedEnvelope env, int width) {
        double reqScale = RendererUtilities.calculateOGCScale(env, width, null);

        int i = 0;
        double error = Math.abs(gridSet.getGrid(i).getScaleDenominator() - reqScale);
        while (i < gridSet.getNumLevels() - 1) {
            Grid g = gridSet.getGrid(i + 1);
            double e = Math.abs(g.getScaleDenominator() - reqScale);

            if (e > error) {
                break;
            } else {
                error = e;
            }
            i++;
        }

        return Math.max(i, 0);
    }


    /**
     * Extract the reading envelope from the parameter list.
     * 
     * @param coverageName
     * @param parameters
     * @return
     */
    private GridGeometry2D extractEnvelope(GeneralParameterValue[] parameters) {
        for (GeneralParameterValue gParam : parameters) {
            GeneralParameterDescriptor descriptor = gParam.getDescriptor();
            final ReferenceIdentifier name = descriptor.getName();
            if (name.equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName())) {
                if (gParam instanceof ParameterValue<?>) {
                    final ParameterValue<?> param = (ParameterValue<?>) gParam;
                    final Object value = param.getValue();
                    final GridGeometry2D gg = (GridGeometry2D) value;
                    return gg;
                }
            }
        }
        return null;
    }

    /**
     * Caching Structured GridCoverage2DReader implementation
     */
    static class CachingStructuredGridCoverage2DReader extends CachingGridCoverage2DReader implements StructuredGridCoverage2DReader {

        private StructuredGridCoverage2DReader structuredDelegate;

        public CachingStructuredGridCoverage2DReader( GridCoveragesCache cache,
                CoverageInfo info, StructuredGridCoverage2DReader reader) {
            super(cache, info, reader);
            this.structuredDelegate = reader;
        }

        @Override
        public GranuleSource getGranules(String coverageName, boolean readOnly) throws IOException,
                UnsupportedOperationException {
            return structuredDelegate.getGranules(coverageName, readOnly);
        }

        @Override
        public boolean isReadOnly() {
            return structuredDelegate.isReadOnly();
        }

        @Override
        public void createCoverage(String coverageName, SimpleFeatureType schema)
                throws IOException, UnsupportedOperationException {
            structuredDelegate.createCoverage(coverageName, schema);
        }

        @Override
        public boolean removeCoverage(String coverageName) throws IOException,
                UnsupportedOperationException {
            return structuredDelegate.removeCoverage(coverageName);
        }

        @Override
        public boolean removeCoverage(String coverageName, boolean delete) throws IOException,
                UnsupportedOperationException {
            return structuredDelegate.removeCoverage(coverageName, delete);
        }

        @Override
        public void delete(boolean deleteData) throws IOException {
            structuredDelegate.delete(deleteData);
        }

        @Override
        public List<HarvestedSource> harvest(String defaultTargetCoverage, Object source,
                Hints hints) throws IOException, UnsupportedOperationException {
            return structuredDelegate.harvest(defaultTargetCoverage, source, hints);
        }

        @Override
        public List<DimensionDescriptor> getDimensionDescriptors(String coverageName)
                throws IOException {
            return structuredDelegate.getDimensionDescriptors(coverageName);
        }
    }
}

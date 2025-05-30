/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeDouble;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.zonal.ZonalStatsDescriptor;
import it.geosolutions.jaiext.zonal.ZoneGeometry;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.WPSException;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform2D;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.raster.CoverageUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.util.DateRange;
import org.geotools.util.DateTimeParser;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * A GeoServer process for computing spatio-temporal zonal statistics on multi-temporal raster coverages.
 *
 * <p>This process allows computing statistical aggregations (such as min, max, sum, mean, median) across multiple time
 * steps for specific zone polygon features. It supports time specification through either a comma-separated list of
 * timestamps or a time range.
 *
 * <p>When specifying a time range, the process will extract the time domain from the underlying coverage limiting the
 * results to a max of 1000 time entries (configurable via the "spatio.temporal.max.entries" system property).
 */
@DescribeProcess(
        title = "SpatioTemporal Zonal statistics",
        description = "Compute aggregated zonal statistics on a multi-temporal coverage")
public class SpatioTemporalZonalStatistics implements GeoServerProcess {

    private static final String DESCRIPTION = "A feature collection containing aggregated statistics for each zone. "
            + " The process will iterate over the specified times and aggregate the requested stats for the zone."
            + " Aggregation is made by computing the min of the mins, the max of the maxes, the sum of the sums, "
            + " the mean of the means, and the mean of the medians.";

    private static final Integer[] BAND_STAT = {0};

    public static final int MAX_TIME_ENTRIES =
            Integer.parseInt(System.getProperty("spatio.temporal.max.entries", "1000"));

    private static final EnumSet<StatsType> ALLOWED_STATS =
            EnumSet.of(StatsType.MIN, StatsType.MAX, StatsType.SUM, StatsType.MEAN, StatsType.MEDIAN);

    static final Logger LOGGER = Logging.getLogger(SpatioTemporalZonalStatistics.class);

    private Catalog catalog;

    private DateTimeParser timeParser = new DateTimeParser(MAX_TIME_ENTRIES);

    public SpatioTemporalZonalStatistics(Catalog catalog) {
        this.catalog = catalog;
    }

    @DescribeResult(name = "result", description = DESCRIPTION, type = SimpleFeatureCollection.class)
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "layerName", description = "Input layer name of a multi-temporal raster")
                    String layerName,
            @DescribeParameter(
                            name = "timeValues",
                            description =
                                    "Time values over which the statistics should be computed. Either a comma separated list of values or a temporal range")
                    String times,
            @DescribeParameter(name = "zones", description = "Zone polygon features for which to compute statistics")
                    SimpleFeatureCollection zones,
            @DescribeParameter(
                            name = "statsNames",
                            description =
                                    "Comma separated list of requested statistics within this set (min/max/sum/mean/median). Compute all statistics if not specified",
                            min = 0)
                    String statsNames)
            throws ProcessException {

        GridCoverage2DReader reader;
        StatsType[] requestedStats = parseStatistics(statsNames);
        LayerInfo layer = catalog.getLayerByName(layerName);
        if (layer == null) {
            throw new ProcessException("Layer '" + layerName + "' not found in catalog.");
        }

        // Validate and retrieve the coverage resource
        if (!(layer.getResource() instanceof CoverageInfo)) {
            throw new ProcessException("Layer '" + layerName + "' is not a coverage resource.");
        }
        CoverageInfo coverage = (CoverageInfo) layer.getResource();

        // Obtain a reader from the coverage (going through the resource pool)
        try {
            reader = (GridCoverage2DReader) coverage.getGridCoverageReader(null, null);
        } catch (IOException e) {
            throw new ProcessException("Unable to obtain a reader for layer: " + layerName, e);
        }
        if (reader == null) {
            throw new ProcessException("Unable to obtain a reader for layer: " + layerName);
        }

        List<Object> timeDomain = parseTimes(reader, times);
        return new SpatioTemporalZonalStatisticsCollection(reader, zones, timeDomain, requestedStats);
    }

    /**
     * Aggregates statistical values across multiple coverage readings (from multiple times). Supports computing
     * aggregate statistics like sum, mean, min, max, and median for a set of requested statistical measures.
     */
    static class StatisticsAggregator {

        private StatsType[] requestedStats;
        private double aggregatedSum;
        private double aggregatedMean;
        private double aggregatedMin = Double.POSITIVE_INFINITY;
        private double aggregatedMax = Double.NEGATIVE_INFINITY;
        private double aggregatedMedian;
        private int count;
        private int aggregated;

        public StatisticsAggregator(StatsType[] requestedStats) {
            this.requestedStats = requestedStats;
        }

        /**
         * Aggregates a temporal ZonalStats result into the current aggregator.
         *
         * @param stats the statistics for one temporal coverage reading.
         */
        public void aggregate(List<ZoneGeometry> stats) {
            if (stats == null) {
                return;
            }
            // no classification raster, only one set of stats expected
            Statistics[] statsArray = stats.get(0)
                    .getStatsPerBand(0)
                    .values()
                    .iterator()
                    .next()
                    .values()
                    .iterator()
                    .next();

            aggregated++;
            count += statsArray[0].getNumSamples().intValue();

            for (int i = 0; i < requestedStats.length; i++) {
                StatsType stat = requestedStats[i];
                double result = ((Number) statsArray[i].getResult()).doubleValue();
                if (stat == StatsType.MIN) {
                    aggregatedMin = Math.min(aggregatedMin, result);
                } else if (stat == StatsType.MAX) {
                    aggregatedMax = Math.max(aggregatedMax, result);
                } else if (stat == StatsType.SUM) {
                    aggregatedSum += result;
                } else if (stat == StatsType.MEAN) {
                    aggregatedMean = aggregatedMean + (result - aggregatedMean) / aggregated;
                } else if (stat == StatsType.MEDIAN) {
                    aggregatedMedian = aggregatedMedian + (result - aggregatedMedian) / aggregated;
                }
            }
        }

        public double getAggregatedSum() {
            return aggregatedSum;
        }

        public double getAggregatedMean() {
            return aggregatedMean;
        }

        public double getAggregatedMin() {
            return aggregatedMin;
        }

        public double getAggregatedMax() {
            return aggregatedMax;
        }

        public int getAggregated() {
            return aggregated;
        }

        public long getAggregatedCount() {
            return count;
        }

        public double getAggregatedMedian() {
            return aggregatedMedian;
        }
    }

    /**
     * A feature collection that computes zonal statistics in a streaming fashion
     *
     * <p>Part of this code has been imported and adapted from RasterZonalStatistics
     */
    static class SpatioTemporalZonalStatisticsCollection extends DecoratingSimpleFeatureCollection {

        private GridCoverage2DReader reader;
        private List<Object> times;
        private SimpleFeatureType targetSchema;
        private StatsType[] requestedStats;

        public SpatioTemporalZonalStatisticsCollection(
                GridCoverage2DReader reader,
                SimpleFeatureCollection zones,
                List<Object> times,
                StatsType[] requestedStats) {
            super(zones);
            this.reader = reader;
            this.times = times;
            this.requestedStats = requestedStats;

            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            for (AttributeDescriptor att : zones.getSchema().getAttributeDescriptors()) {
                tb.minOccurs(att.getMinOccurs());
                tb.maxOccurs(att.getMaxOccurs());
                tb.restrictions(att.getType().getRestrictions());
                if (att instanceof GeometryDescriptor) {
                    GeometryDescriptor gatt = (GeometryDescriptor) att;
                    tb.crs(gatt.getCoordinateReferenceSystem());
                }
                tb.add("z_" + att.getLocalName(), att.getType().getBinding());
            }

            addAttributes(tb, requestedStats);
            tb.setName(zones.getSchema().getName());
            targetSchema = tb.buildFeatureType();
        }

        private static void addAttributes(SimpleFeatureTypeBuilder tb, StatsType[] requestedStats) {
            tb.add("count", Long.class); // count is always added

            for (StatsType requestedStat : requestedStats) {
                if (requestedStat == StatsType.MIN) {
                    tb.add("min", Double.class);
                } else if (requestedStat == StatsType.MAX) {
                    tb.add("max", Double.class);
                } else if (requestedStat == StatsType.SUM) {
                    tb.add("sum", Double.class);
                } else if (requestedStat == StatsType.MEAN) {
                    tb.add("mean", Double.class);
                } else if (requestedStat == StatsType.MEDIAN) {
                    tb.add("median", Double.class);
                }
            }
        }

        @Override
        public SimpleFeatureType getSchema() {
            return targetSchema;
        }

        @Override
        public SimpleFeatureIterator features() {
            return new SpatioTemporalZonalStatisticsIterator(
                    delegate.features(), reader, targetSchema, times, requestedStats);
        }
    }

    /**
     * An iterator computing statistics as we go
     *
     * <p>Part of this code has been imported and adapted from RasterZonalStatistics
     */
    static class SpatioTemporalZonalStatisticsIterator implements SimpleFeatureIterator {

        private final List<Object> times;
        private GridCoverage2DReader reader;
        private SimpleFeatureIterator zones;
        private SimpleFeatureBuilder builder;
        private SimpleFeature nextFeature;
        private StatsType[] requestedStats;

        public SpatioTemporalZonalStatisticsIterator(
                SimpleFeatureIterator zones,
                GridCoverage2DReader reader,
                SimpleFeatureType targetSchema,
                List<Object> times,
                StatsType[] requestedStats) {
            this.zones = zones;
            this.builder = new SimpleFeatureBuilder(targetSchema);
            this.reader = reader;
            this.requestedStats = requestedStats;
            this.times = times;
        }

        @Override
        public void close() {
            zones.close();
        }

        @Override
        public boolean hasNext() {
            if (nextFeature != null) {
                return true;
            }
            if (!zones.hasNext()) {
                return false;
            }
            // Compute nextFeature
            while (zones.hasNext()) {
                // grab the current zone
                SimpleFeature zone = zones.next();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Next feature zone is: " + zone);
                }

                try {
                    // grab the geometry and eventually reproject it to the
                    Geometry zoneGeom = (Geometry) zone.getDefaultGeometry();
                    CoordinateReferenceSystem dataCrs = reader.getCoordinateReferenceSystem();
                    CoordinateReferenceSystem zonesCrs =
                            builder.getFeatureType().getGeometryDescriptor().getCoordinateReferenceSystem();
                    if (!CRS.equalsIgnoreMetadata(zonesCrs, dataCrs)) {
                        zoneGeom = JTS.transform(zoneGeom, CRS.findMathTransform(zonesCrs, dataCrs, true));
                    }

                    // gather the statistics
                    StatisticsAggregator stats = processStatistics(zoneGeom);

                    // build the resulting feature
                    if (stats != null) {
                        builder.addAll(zone.getAttributes());
                        addStatsToFeature(stats, requestedStats);
                    } else {
                        builder.addAll(zone.getAttributes());
                    }
                    nextFeature = builder.buildFeature(zone.getID());
                    return true;
                } catch (Exception e) {
                    throw new ProcessException("Failed to compute statistics on feature " + zone, e);
                }
            }
            return false;
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            SimpleFeature result = nextFeature;
            nextFeature = null;
            return result;
        }

        /** Add the statistics to the feature builder */
        private void addStatsToFeature(StatisticsAggregator stats, StatsType[] requestedStats) {
            double count = stats.getAggregatedCount();
            builder.add(count); // count
            addDynamicStatsToFeature(builder, stats, requestedStats);
        }

        private void addDynamicStatsToFeature(
                SimpleFeatureBuilder builder, StatisticsAggregator stats, StatsType[] requestedStats) {
            for (StatsType requestedStat : requestedStats) {
                if (requestedStat == StatsType.MIN) {
                    builder.add(stats.getAggregatedMin());
                } else if (requestedStat == StatsType.MAX) {
                    builder.add(stats.getAggregatedMax());
                } else if (requestedStat == StatsType.SUM) {
                    builder.add(stats.getAggregatedSum());
                } else if (requestedStat == StatsType.MEAN) {
                    builder.add(stats.getAggregatedMean());
                } else if (requestedStat == StatsType.MEDIAN) {
                    builder.add(stats.getAggregatedMedian());
                }
            }
        }

        @SuppressWarnings("unchecked")
        private StatisticsAggregator processStatistics(Geometry geometry) throws TransformException, IOException {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Starting statistics aggregation on geometry: " + geometry);
            }
            List<RangeDouble> noDataValueRangeList = null;
            ROI roi = null;
            ParameterValue<List> timeParam = AbstractGridFormat.TIME.createValue();
            StatisticsAggregator aggregator = new StatisticsAggregator(requestedStats);
            boolean initialized = false;
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
            ReferencedEnvelope geometryEnvelope = null;
            ParameterValue<GridGeometry2D> gg = AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();

            for (Object temporalItem : times) {
                if (temporalItem instanceof Date) {
                    Date time = (Date) temporalItem;
                    LOGGER.fine("Computing stat for time: " + time);
                    timeParam.setValue(Collections.singletonList(time));
                } else if (temporalItem instanceof DateRange) {
                    DateRange range = (DateRange) temporalItem;
                    LOGGER.fine("Computing stat for time: " + range);
                    timeParam.setValue(Collections.singletonList(range));
                } else {
                    throw new IllegalArgumentException("Unsupported temporal item: " + temporalItem);
                }
                GeneralParameterValue[] gpv;
                GridCoverage2D dataCoverage = null;
                GridCoverage2D cropped = null;
                Range nodata = null;
                try {
                    // Assume we can share the same bbox/crop to all the times
                    if (!initialized) {
                        geometryEnvelope = new ReferencedEnvelope(geometry.getEnvelopeInternal(), crs);
                        ReferencedEnvelope nativeEnvelope = new ReferencedEnvelope(reader.getOriginalEnvelope());
                        AffineTransform gridToWorld = getGridToWorld(reader, nativeEnvelope);
                        double resX = Math.abs(gridToWorld.getScaleX());
                        double resY = Math.abs(gridToWorld.getScaleY());
                        geometryEnvelope.expandBy(resX, resY);
                        if (!nativeEnvelope.intersects((Envelope) geometryEnvelope)) {
                            // no intersection, no stats
                            return null;
                        } else if (!nativeEnvelope.contains((Envelope) geometryEnvelope)) {
                            // the geometry goes outside of the coverage envelope, that makes
                            // the stats fail for some reason
                            geometry = JTS.toGeometry((Envelope) nativeEnvelope).intersection(geometry);
                            geometryEnvelope = new ReferencedEnvelope(geometry.getEnvelopeInternal(), crs);
                        }
                        gg.setValue(getGridGeometry(geometryEnvelope, gridToWorld));
                        gpv = new GeneralParameterValue[] {timeParam, gg};
                        dataCoverage = reader.read(gpv);
                        // check if the novalue is != from NaN
                        noDataValueRangeList = CoverageUtilities.getNoDataAsList(dataCoverage);
                        if (noDataValueRangeList != null && !noDataValueRangeList.isEmpty()) {
                            nodata = noDataValueRangeList.get(0);
                        }
                        roi = CoverageUtilities.getSimplifiedRoiGeometry(dataCoverage, geometry);
                        initialized = true;
                    } else {
                        gpv = new GeneralParameterValue[] {timeParam, gg};
                        dataCoverage = reader.read(gpv);
                    }
                    if (dataCoverage == null) {
                        LOGGER.warning("null coverage has been returned for time " + temporalItem
                                + ". Excluding it from the computations");
                        continue;
                    }
                    LOGGER.fine("Cropping the coverage on geometry: " + geometryEnvelope);
                    cropped = CoverageUtilities.crop(dataCoverage, geometryEnvelope);
                    LOGGER.fine("Executing the zonal stat operation");
                    RenderedOp op = ZonalStatsDescriptor.create(
                            cropped.getRenderedImage(),
                            null,
                            null,
                            Arrays.asList(roi),
                            nodata,
                            null,
                            false,
                            new int[] {0},
                            requestedStats,
                            null,
                            false,
                            null);
                    aggregator.aggregate((List<ZoneGeometry>) op.getProperty(ZonalStatsDescriptor.ZS_PROPERTY));
                } finally {
                    // dispose coverages
                    if (cropped != null) {
                        cropped.dispose(true);
                    }
                    if (dataCoverage != null) {
                        dataCoverage.dispose(true);
                    }
                }
            }
            return aggregator;
        }

        private AffineTransform getGridToWorld(GridCoverage2DReader reader, ReferencedEnvelope nativeEnvelope) {
            GridEnvelope nativeGrid = reader.getOriginalGridRange();
            final GridToEnvelopeMapper geMapper = new GridToEnvelopeMapper(nativeGrid, nativeEnvelope);
            geMapper.setPixelAnchor(PixelInCell.CELL_CORNER);
            AffineTransform sourceGridToWorldTransform = geMapper.createAffineTransform();
            return sourceGridToWorldTransform;
        }
    }

    private static GridGeometry2D getGridGeometry(
            ReferencedEnvelope geometryEnvelope, AffineTransform sourceGridToWorldTransform) throws TransformException {
        MathTransform2D gridToWorld = ((MathTransform2D) sourceGridToWorldTransform);
        MathTransform2D worldToGrid = gridToWorld.inverse();
        Envelope gridEnvelope = JTS.transform(geometryEnvelope, worldToGrid);

        int minX = (int) Math.floor(gridEnvelope.getMinX());
        int maxX = (int) Math.ceil(gridEnvelope.getMaxX());
        int minY = (int) Math.floor(gridEnvelope.getMinY());
        int maxY = (int) Math.ceil(gridEnvelope.getMaxY());
        int width = maxX - minX;
        int height = maxY - minY;
        GridEnvelope2D gridRange = new GridEnvelope2D(minX, minY, width, height);
        return new GridGeometry2D(gridRange, geometryEnvelope);
    }

    private static StatsType[] parseStatistics(String statsNames) throws WPSException {
        List<StatsType> requestedStats = new ArrayList<>();

        if (statsNames == null || statsNames.trim().isEmpty()) {
            requestedStats.addAll(ALLOWED_STATS);
            return requestedStats.toArray(StatsType[]::new);
        }

        String[] tokens = statsNames.split(",");
        for (String token : tokens) {
            String statName = token.trim().toUpperCase();
            try {
                StatsType stat = StatsType.valueOf(statName);
                if (!ALLOWED_STATS.contains(stat)) {
                    throw new WPSException(
                            "Statistic not allowed: " + statName, ServiceException.INVALID_PARAMETER_VALUE, statsNames);
                }
                requestedStats.add(stat);
            } catch (IllegalArgumentException e) {
                WPSException wpse = new WPSException(
                        "Unknown statistic: " + statName, ServiceException.INVALID_PARAMETER_VALUE, statsNames);
                wpse.initCause(e);
                throw wpse;
            }
        }

        return requestedStats.toArray(StatsType[]::new);
    }

    private List<Object> parseTimes(GridCoverage2DReader reader, String timeValues) throws ProcessException {
        LOGGER.fine("Retrieving timeDomain for the specified timeValues: " + timeValues);
        if (timeValues == null || timeValues.trim().isEmpty()) {
            throw new WPSException(
                    "Time parameter cannot be null or empty", ServiceException.INVALID_PARAMETER_VALUE, timeValues);
        }
        try {
            Collection<?> parsed = timeParser.parse(timeValues);
            List<Object> timeDomain = parsed.stream().collect(Collectors.toList());
            if (timeDomain.size() == 1 && timeDomain.get(0) instanceof DateRange) {
                // If it's a single timeRange delegate to the ReaderDimensionsAccessor
                // to retrieve the underlying times.
                timeDomain = parseTimeRange(reader, ((DateRange) timeDomain.get(0)));
            }
            return timeDomain;
        } catch (IOException | ParseException e) {
            throw new ProcessException("Error retrieving the temporal domain", e);
        }
    }

    private static List<Object> parseTimeRange(GridCoverage2DReader reader, DateRange dateRange) throws IOException {
        ReaderDimensionsAccessor dimensionsAccessor = new ReaderDimensionsAccessor(reader);
        TreeSet<Object> timeDomain = dimensionsAccessor.getTimeDomain(dateRange, MAX_TIME_ENTRIES);
        if (timeDomain == null || timeDomain.isEmpty()) {
            throw new ProcessException("No entries have been found in the specified dateRange: " + dateRange);
        }
        return new ArrayList<>(timeDomain);
    }
}

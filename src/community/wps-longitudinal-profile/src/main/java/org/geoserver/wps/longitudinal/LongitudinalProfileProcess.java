/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.MathTransform2D;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.util.NullProgressListener;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.DisposableBean;

@DescribeProcess(
        title = "Longitudinal Profile Process",
        description = "The process splits provided linestring to segments, that are no bigger then distance parameter, "
                + "then evaluates altitude for each point and builds longitudinal profile. "
                + "Altitude will be adjusted if adjustment layer is provided as parameter. "
                + "Also supports reprojection to different crs")
public class LongitudinalProfileProcess implements GeoServerProcess, DisposableBean {

    static final Logger LOGGER = Logging.getLogger(LongitudinalProfileProcess.class);

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final String SEP = System.lineSeparator();
    // for reference, 18k points would currently use 1MB of memory (56 bytes per point)
    public static final int DEFAULT_MAX_POINTS = 50000;
    // default chunk size for parallel processing (used to be 1000, load tests shows this 10% faster)
    public static final int DEFAULT_CHUNK_SIZE = 5000;

    private final GeoServer geoServer;
    private final int chunkSize;
    private final int maxPoints;

    private ExecutorService executor;

    public LongitudinalProfileProcess(GeoServer geoServer) {
        this.geoServer = geoServer;

        // maximum points that will be computed in a single process call
        maxPoints = getMaxPoints();

        // Chunk size for parallel processing
        chunkSize = getChunkSize();

        // Create threads executor
        int nbTreads = getMaxThreads();
        this.executor = Executors.newFixedThreadPool(nbTreads);
    }

    private static int getMaxThreads() {
        int nbTreads = Runtime.getRuntime().availableProcessors();
        try {
            String maxThreads = GeoServerExtensions.getProperty("wpsLongitudinalMaxThreadPoolSize");
            if (maxThreads != null && !maxThreads.isEmpty()) nbTreads = Integer.parseInt(maxThreads);
        } catch (NumberFormatException e) {
            LOGGER.warning(
                    "Can't parse wpsLongitudinalMaxThreadPoolSize property, must be an integer. Will use Runtime.getRuntime().availableProcessors() instead.");
        }
        return nbTreads;
    }

    private int getChunkSize() {
        int chunkSize = DEFAULT_CHUNK_SIZE;
        try {
            chunkSize = Integer.parseInt(GeoServerExtensions.getProperty("wpsLongitudinalVerticesChunkSize"));
        } catch (NumberFormatException e) {
            LOGGER.warning("Can't parse wpsLongitudinalVerticesChunkSize property, must be an integer. Will use "
                    + chunkSize + " instead.");
        }
        return chunkSize;
    }

    private int getMaxPoints() {
        int maxPoints = DEFAULT_MAX_POINTS;
        try {
            String maxPointsStr = GeoServerExtensions.getProperty("wpsLongitudinalMaxPoints");
            if (maxPointsStr != null && !maxPointsStr.isEmpty()) {
                maxPoints = Integer.parseInt(maxPointsStr);
            }
        } catch (NumberFormatException e) {
            LOGGER.warning("Can't parse wpsLongitudinalMaxPoints property, must be an integer. Will use " + maxPoints
                    + " instead.");
        }
        return maxPoints;
    }

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
    }

    @DescribeResult(
            name = "result",
            description = "Longitudinal Profile Process result.",
            meta = {"mimeTypes=application/json"},
            type = LongitudinalProfileProcessResult.class)
    public LongitudinalProfileProcessResult execute(
            @DescribeParameter(name = "layerName", description = "Input raster name", min = 0) String layerName,
            @DescribeParameter(name = "coverage", description = "Input coverage", min = 0) GridCoverage2D coverage,
            @DescribeParameter(name = "adjustmentLayerName", description = "adjustment layer name", min = 0)
                    String adjustmentLayerName,
            @DescribeParameter(name = "geometry", description = "geometry for profile", min = 1)
                    final Geometry geometry,
            @DescribeParameter(name = "distance", description = "distance between points in meters", min = 0)
                    final Double distance,
            @DescribeParameter(name = "targetProjection", description = "projection for result", min = 0)
                    CoordinateReferenceSystem projection,
            @DescribeParameter(
                            name = "altitudeIndex",
                            description = "index of altitude in coordinate array",
                            min = 0,
                            defaultValue = "0")
                    int altitudeIndex,
            @DescribeParameter(
                            name = "altitudeName",
                            description = "name of altitude attribute on adjustment layer",
                            min = 0)
                    String altitudeName,
            @DescribeParameter(
                            name = "projectedDistance",
                            description = "measure distances on the targetProjection plane rather than on the ground",
                            min = 0,
                            defaultValue = "false")
                    boolean projectedDistance,
            ProgressListener monitor)
            throws IOException, FactoryException, TransformException, CQLException, InterruptedException,
                    ExecutionException {
        // null safety for the progress listener
        if (monitor == null) {
            monitor = new NullProgressListener();
        }
        monitor.started();

        long startTime = System.currentTimeMillis();
        LOGGER.fine(() -> {
            StringJoiner joiner = new StringJoiner(SEP);
            joiner.add("Starting processing at:" + startTime + " with params: ")
                    .add("layer name: " + layerName)
                    .add("adjustment layer name: " + adjustmentLayerName)
                    .add("geometry: " + geometry)
                    .add("distance: " + distance)
                    .add("altitude index: " + altitudeIndex)
                    .add("altitude name: " + altitudeName)
                    .add("projected distance: " + projectedDistance);
            return joiner.toString();
        });

        GridCoverage2D gridCoverage2D;
        if (layerName != null) {
            CoverageInfo coverageInfo = geoServer.getCatalog().getCoverageByName(layerName);
            GridCoverage2DReader gridCoverageReader =
                    (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
            // critical to avoid OOM on large DEMs, the reader might be using immediate reading otherwise
            ParameterValue<Boolean> useImageRead = AbstractGridFormat.USE_IMAGEN_IMAGEREAD.createValue();
            useImageRead.setValue(true);
            GeneralParameterValue[] readParameters = {useImageRead};
            gridCoverage2D = gridCoverageReader.read(readParameters);
        } else if (coverage != null) {
            gridCoverage2D = coverage;
        } else {
            throw new WPSException("Either layerName or coverage must be provided");
        }

        // check the geometry, reproject if necessary, densify it
        if (!(geometry instanceof LineString)) throw new IllegalArgumentException("Geometry must be a LineString");

        // Project to CRS of provided geometry, if projection parameter is not provided
        CoordinateReferenceSystem geometryCRS = null;
        if (geometry.getUserData() instanceof CoordinateReferenceSystem) {
            geometryCRS = (CoordinateReferenceSystem) geometry.getUserData();
        }

        if (projection != null) {
            LOGGER.fine(" targetProjection: " + projection.getName());
        } else {
            projection = geometryCRS != null ? geometryCRS : gridCoverage2D.getCoordinateReferenceSystem2D();
        }

        // If geometry does not contain any info on CRS we will use CRS of the input coverage
        CoordinateReferenceSystem coverageCRS = gridCoverage2D.getCoordinateReferenceSystem2D();
        LineString reprojected = (LineString) geometry;
        if (geometryCRS != null) {
            if (CRS.isTransformationRequired(geometryCRS, coverageCRS)) {
                reprojected = reprojectGeometry(geometryCRS, coverageCRS, reprojected);
            }
        }
        // densification runs on the line in coverage CRS
        Coordinate[] coords = densify(reprojected, distance, gridCoverage2D);
        List<ProfileVertice> vertices = IntStream.range(0, coords.length)
                .mapToObj(i -> new ProfileVertice(i, coords[i], ProfileVertice.UNSET))
                .collect(Collectors.toList());

        List<List<ProfileVertice>> chunks = divide(vertices, chunkSize);
        List<Future<List<ProfileVertice>>> treated = new ArrayList<>();

        // Process parallel altitude reading
        FeatureSource adjustmentFeatureSource = getAdjustmentLayerFeatureSource(adjustmentLayerName);
        for (List<ProfileVertice> chunk : chunks) {
            DistanceSlopeCalculator calculator =
                    getDistanceSlopeCalculator(gridCoverage2D, altitudeIndex, projection, projectedDistance);
            treated.add(executor.submit(new AltitudeReaderThread(
                    chunk, altitudeIndex, adjustmentFeatureSource, altitudeName, gridCoverage2D, calculator, monitor)));
        }

        List<ProfileVertice> result = new ArrayList<>();
        for (Future<List<ProfileVertice>> f : treated) {
            List<ProfileVertice> futureResult = f.get();
            // check for cancellation, but don't stop the executor, as it's used for other process calls as well
            if (monitor.isCanceled()) return null;
            if (futureResult != null) {
                if (futureResult.get(0).getNumber() > 0)
                    // remove the first point, as it was added to allow for slope and distance calculation
                    futureResult.remove(0);

                result.addAll(futureResult);
            }
        }

        // Sort vertices by their number
        result.sort(new Comparator<>() {
            @Override
            public int compare(ProfileVertice pv1, ProfileVertice pv2) {
                return Integer.compare(pv1.number, pv2.number);
            }
        });

        List<ProfileInfo> profileInfos = new ArrayList<>();
        double positiveAltitude = 0;
        double negativeAltitude = 0;
        double previousAltitude = 0;
        boolean hasPreviousAltitude = false;
        double totalDistance = 0;
        for (ProfileVertice v : result) {
            // check for cancellation
            if (monitor.isCanceled()) return null;
            Coordinate coordinate = v.getCoordinate();
            if (v.getDistancePrevious() != ProfileVertice.UNSET) totalDistance += v.getDistancePrevious();
            ProfileInfo currentInfo =
                    new ProfileInfo(totalDistance, coordinate.getX(), coordinate.getY(), v.getAltitude(), v.getSlope());
            if (hasPreviousAltitude) {
                double profileAltitude = v.getAltitude() - previousAltitude;
                if (profileAltitude >= 0) {
                    positiveAltitude += profileAltitude;
                } else {
                    negativeAltitude += profileAltitude;
                }
            }

            previousAltitude = v.getAltitude();
            hasPreviousAltitude = true;

            profileInfos.add(currentInfo);
        }

        OperationInfo operationInfo = buildOperationInfo(
                layerName, startTime, profileInfos, positiveAltitude, negativeAltitude, totalDistance);

        monitor.complete();
        return new LongitudinalProfileProcessResult(profileInfos, operationInfo);
    }

    /** Returns the line vertices, at most {@code distance} ground meters apart, or a pixel diagonal when null. */
    private Coordinate[] densify(LineString line, Double distance, GridCoverage2D coverage) {
        if (distance != null && !(distance > 0)) throw new WPSException("Distance must be positive, was " + distance);
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
        GeodeticCalculator gc = null;
        double step = Double.NaN;
        if (distance == null) step = getPixelDiagonal(coverage);
        else if (crs instanceof GeographicCRS || crs instanceof ProjectedCRS) gc = new GeodeticCalculator(crs);
        // no ellipsoid to measure on, assume CRS units are ground units
        else step = distance * DistanceSlopeCalculator.getUnitsPerMeter(crs);

        Coordinate[] coords = line.getCoordinates();
        List<Coordinate> dense = new ArrayList<>(List.of(coords[0]));
        for (int i = 1; i < coords.length; i++) {
            LineSegment segment = new LineSegment(coords[i - 1], coords[i]);
            // a repeated vertex would give a zero run, and an undefined slope
            if (segment.getLength() == 0) continue;
            long pieces = gc != null
                    ? countGroundPieces(gc, segment, distance)
                    : Math.max(1, (long) Math.ceil(segment.getLength() / step));
            long expectedPoints = dense.size() + pieces;
            if (expectedPoints > maxPoints)
                throw new WPSException("Too many points in the line, please increase the distance parameter "
                        + "or reduce the line length. Would extract at least " + expectedPoints
                        + " points, but maximum is " + maxPoints);
            for (long j = 1; j < pieces; j++) dense.add(segment.pointAlong((double) j / pieces));
            dense.add(coords[i]);
        }
        return dense.toArray(Coordinate[]::new);
    }

    /**
     * Returns how many equal pieces split the segment so that no piece is longer than {@code distance} meters on the
     * ellipsoid. The meters in a CRS unit change along the segment: with the projection scale (Mercator grows with
     * latitude) and, in geographic CRSs, with direction.
     */
    private long countGroundPieces(GeodeticCalculator gc, LineSegment segment, double distance) {
        long pieces = 1;
        // Pass 0 measures the whole segment, so the count fits the average scale.
        // Pass 1 measures each piece and keeps the smallest ratio, from the piece where a CRS unit covers the most
        // ground, so the count fits that piece too. Inside a piece the scale barely changes, so two passes are enough.
        // Above maxPoints there is no point in refining, the caller rejects the count.
        for (int pass = 0; pass < 2 && pieces <= maxPoints; pass++) {
            double ratio = Double.POSITIVE_INFINITY;
            DistanceSlopeCalculator.setDestination(gc, segment.p0);
            for (long j = 1; j <= pieces; j++) {
                // the previous destination becomes the start, so each point is projected to geographic once
                gc.setStartingGeographicPoint(gc.getDestinationGeographicPoint());
                DistanceSlopeCalculator.setDestination(gc, segment.pointAlong((double) j / pieces));
                ratio = Math.min(ratio, segment.getLength() / pieces / gc.getOrthodromicDistance());
            }
            pieces = Math.max(pieces, (long) Math.ceil(segment.getLength() / (distance * ratio)));
        }
        return pieces;
    }

    private static double getPixelDiagonal(GridCoverage2D coverage) {
        LOGGER.fine("Distance parameter has not been provided, using the pixel diagonal");
        MathTransform2D gridToCRS = coverage.getGridGeometry().getGridToCRS2D();
        if (!(gridToCRS instanceof AffineTransform affine))
            throw new WPSException("Unsupported non affine grid to world transformation: " + gridToCRS);
        // Compute the diagonal distance
        double step = Math.hypot(XAffineTransform.getScaleX0(affine), XAffineTransform.getScaleY0(affine));
        LOGGER.fine("Computed distance: " + step);
        return step;
    }

    /** Unecessary for runtime, but useful for testing */
    protected DistanceSlopeCalculator getDistanceSlopeCalculator(
            GridCoverage2D coverage,
            int altitudeIndex,
            CoordinateReferenceSystem projection,
            boolean projectedDistance) {
        return new DistanceSlopeCalculator(coverage, altitudeIndex, projection, projectedDistance);
    }

    private static OperationInfo buildOperationInfo(
            String layerName,
            long startTime,
            List<ProfileInfo> profileInfos,
            double positiveAltitude,
            double negativeAltitude,
            double totalDistance) {
        OperationInfo operationInfo = new OperationInfo();
        operationInfo.setTotalDistance(totalDistance);
        operationInfo.setProcessedPoints(profileInfos.size());
        ProfileInfo firstProfile = profileInfos.get(0);
        operationInfo.setFirstPointX(firstProfile.getX());
        operationInfo.setFirstPointY(firstProfile.getY());

        ProfileInfo lastProfile = profileInfos.get(profileInfos.size() - 1);
        operationInfo.setLastPointX(lastProfile.getX());
        operationInfo.setLastPointY(lastProfile.getY());
        operationInfo.setAltitudePositive(positiveAltitude);
        operationInfo.setAltitudeNegative(negativeAltitude);
        operationInfo.setLayer(layerName);

        operationInfo.setExecutedTime(System.currentTimeMillis() - startTime);
        return operationInfo;
    }

    @SuppressWarnings("unchecked")
    static <T extends Geometry> T reprojectGeometry(
            CoordinateReferenceSystem source, CoordinateReferenceSystem target, T geometry)
            throws FactoryException, TransformException {
        MathTransform tx = CRS.findMathTransform(source, target, true);

        return (T) JTS.transform(geometry, tx);
    }

    protected FeatureSource<? extends FeatureType, ? extends Feature> getAdjustmentLayerFeatureSource(
            String adjustmentLayerName) throws IOException {
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource = null;
        if (adjustmentLayerName != null && !adjustmentLayerName.isBlank()) {
            LayerInfo adjustmentLayer = geoServer.getCatalog().getLayerByName(adjustmentLayerName);
            FeatureTypeInfo resource = (FeatureTypeInfo) adjustmentLayer.getResource();
            featureSource = resource.getFeatureSource(null, null);
        }
        return featureSource;
    }

    private static List<List<ProfileVertice>> divide(List<ProfileVertice> list, final int L) {
        List<List<ProfileVertice>> parts = new ArrayList<>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            if (i > 0) {
                // add an extra point at the beginning to allow for slope and distance calculation
                parts.add(new ArrayList<>(list.subList(i - 1, Math.min(N, i + L))));
            } else {
                parts.add(new ArrayList<>(list.subList(i, Math.min(N, i + L))));
            }
        }
        return parts;
    }

    /** Object for storing result of longitudinal profile WPS process */
    public static final class LongitudinalProfileProcessResult {

        public LongitudinalProfileProcessResult(List<ProfileInfo> profileInfoList, OperationInfo operationInfo) {
            this.profileInfoList = profileInfoList;
            this.operationInfo = operationInfo;
        }

        @JsonProperty("profile")
        private List<ProfileInfo> profileInfoList;

        @JsonProperty("infos")
        private OperationInfo operationInfo;

        public List<ProfileInfo> getProfileInfoList() {
            return profileInfoList;
        }

        public OperationInfo getOperationInfo() {
            return operationInfo;
        }
    }
}

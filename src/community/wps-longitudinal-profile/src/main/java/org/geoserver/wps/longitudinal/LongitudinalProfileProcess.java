/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import static org.locationtech.jts.densify.Densifier.densify;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;
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
import org.geotools.api.referencing.operation.MathTransform;
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
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.DisposableBean;
import si.uom.SI;

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
            @DescribeParameter(name = "distance", description = "distance between points in meters", min = 1)
                    double distance,
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
                    .add("altitude name: " + altitudeName);
            return joiner.toString();
        });

        GridCoverage2D gridCoverage2D;
        if (layerName != null) {
            CoverageInfo coverageInfo = geoServer.getCatalog().getCoverageByName(layerName);
            GridCoverage2DReader gridCoverageReader =
                    (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
            // critical to avoid OOM on large DEMs, the reader might be using immediate reading otherwise
            ParameterValue<Boolean> useImageRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
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
        if (projection != null) {
            LOGGER.fine(" targetProjection: " + projection.getName());
        } else {
            if (geometry.getUserData() instanceof CoordinateReferenceSystem)
                projection = (CoordinateReferenceSystem) geometry.getUserData();
            else projection = gridCoverage2D.getCoordinateReferenceSystem2D();
        }

        // If geometry does not contain any info on CRS we will use CRS of the input coverage
        CoordinateReferenceSystem coverageCRS = gridCoverage2D.getCoordinateReferenceSystem2D();
        LineString reprojected = (LineString) geometry;
        if (geometry.getUserData() instanceof CoordinateReferenceSystem) {
            CoordinateReferenceSystem lineCRS = (CoordinateReferenceSystem) geometry.getUserData();
            if (CRS.isTransformationRequired(lineCRS, coverageCRS)) {
                reprojected = reprojectGeometry(lineCRS, coverageCRS, (LineString) geometry);
            }
        }

        Geometry denseLine = densifyLine(distance, reprojected, coverageCRS);

        // Create an array with all geometry vertices
        Coordinate[] coords = denseLine.getCoordinates();
        List<ProfileVertice> vertices = IntStream.range(0, coords.length)
                .mapToObj(i -> new ProfileVertice(i, coords[i], ProfileVertice.UNSET))
                .collect(Collectors.toList());

        List<List<ProfileVertice>> chunks = divide(vertices, chunkSize);
        List<Future<List<ProfileVertice>>> treated = new ArrayList<>();

        // Process parallel altitude reading
        FeatureSource adjustmentFeatureSource = getAdjustmentLayerFeatureSource(adjustmentLayerName);
        for (List<ProfileVertice> chunk : chunks) {
            DistanceSlopeCalculator calculator = getDistanceSlopeCalculator(projection);
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
        double totalDistance = 0;
        for (ProfileVertice v : result) {
            // check for cancellation
            if (monitor.isCanceled()) return null;
            Coordinate coordinate = v.getCoordinate();
            if (v.getDistancePrevious() != ProfileVertice.UNSET) totalDistance += v.getDistancePrevious();
            ProfileInfo currentInfo =
                    new ProfileInfo(totalDistance, coordinate.getX(), coordinate.getY(), v.getAltitude(), v.getSlope());
            double profileAltitude = v.getAltitude() - previousAltitude;
            if (profileAltitude >= 0) {
                positiveAltitude += profileAltitude;
            } else {
                negativeAltitude += profileAltitude;
            }

            previousAltitude = v.getAltitude();

            profileInfos.add(currentInfo);
        }

        OperationInfo operationInfo = buildOperationInfo(
                layerName, startTime, profileInfos, positiveAltitude, negativeAltitude, totalDistance);

        monitor.complete();
        return new LongitudinalProfileProcessResult(profileInfos, operationInfo);
    }

    /** Unecessary for runtime, but useful for testing */
    protected DistanceSlopeCalculator getDistanceSlopeCalculator(CoordinateReferenceSystem projection) {
        return new DistanceSlopeCalculator(projection);
    }

    private Geometry densifyLine(double distance, LineString lineString, CoordinateReferenceSystem crs) {
        double distanceInTargetCrsUnits =
                metersToCrsUnits(crs, lineString.getCentroid().getCoordinate(), distance);
        long expectedPoints = (long) Math.ceil(lineString.getLength() / distanceInTargetCrsUnits);
        if (expectedPoints > maxPoints)
            throw new WPSException(
                    "Too many points in the line, please increase the distance parameter or reduce the line length. "
                            + "Would extract " + expectedPoints + " points, but maximum is " + maxPoints);
        return densify(lineString, distanceInTargetCrsUnits);
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

    private double metersToCrsUnits(CoordinateReferenceSystem crs, Coordinate centroidCoord, double distanceInMeters) {
        if (crs instanceof GeographicCRS) {
            double sizeDegree = 110574.2727;
            if (centroidCoord != null) {
                double cosLat = Math.cos(Math.PI * centroidCoord.y / 180.0);
                double latAdjustment = Math.sqrt(1 + cosLat * cosLat) / Math.sqrt(2.0);
                sizeDegree *= latAdjustment;
            }
            return distanceInMeters / sizeDegree;
        } else {
            @SuppressWarnings("unchecked")
            Unit<Length> unit =
                    (Unit<Length>) crs.getCoordinateSystem().getAxis(0).getUnit();
            if (unit == null) {
                return distanceInMeters;
            } else {
                UnitConverter converter = SI.METRE.getConverterTo(unit);
                return converter.convert(distanceInMeters);
            }
        }
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

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
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.util.logging.Logging;
import org.jaitools.jts.CoordinateSequence2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.DisposableBean;
import si.uom.SI;

@DescribeProcess(
        title = "Longitudinal Profile Process",
        description =
                "The process splits provided linestring to segments, that are no bigger then distance parameter, "
                        + "then evaluates altitude for each point and builds longitudinal profile. "
                        + "Altitude will be adjusted if adjustment layer is provided as parameter. "
                        + "Also supports reprojection to different crs")
public class LongitudinalProfileProcess implements GeoServerProcess, DisposableBean {

    static final Logger LOGGER = Logging.getLogger(LongitudinalProfileProcess.class);

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final String SEP = System.lineSeparator();

    private final GeoServer geoServer;

    private ExecutorService executor;

    public LongitudinalProfileProcess(GeoServer geoServer) {
        this.geoServer = geoServer;

        // Create threads executor
        int nbTreads = Runtime.getRuntime().availableProcessors();
        try {
            nbTreads =
                    Integer.parseInt(
                            GeoServerExtensions.getProperty("wpsLongitudinalMaxThreadPoolSize"));
        } catch (NumberFormatException e) {
            LOGGER.warning(
                    "Can't parse wpsLongitudinalMaxThreadPoolSize property, must be an integer. Will use Runtime.getRuntime().availableProcessors() instead.");
        }
        this.executor = Executors.newFixedThreadPool(nbTreads);
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
            @DescribeParameter(name = "layerName", description = "Input raster name", min = 1)
                    String layerName,
            @DescribeParameter(
                            name = "adjustmentLayerName",
                            description = "adjustment layer name",
                            min = 0)
                    String adjustmentLayerName,
            @DescribeParameter(name = "geometry", description = "geometry for profile", min = 1)
                    Geometry geometry,
            @DescribeParameter(name = "distance", description = "distance between points", min = 1)
                    double distance,
            @DescribeParameter(
                            name = "targetProjection",
                            description = "projection for result",
                            min = 0)
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
                    String altitudeName)
            throws IOException, FactoryException, TransformException, CQLException,
                    InterruptedException, ExecutionException {

        long startTime = System.currentTimeMillis();
        LOGGER.fine(
                "Starting processing at:"
                        + startTime
                        + " with params: "
                        + SEP
                        + " layer name: "
                        + layerName
                        + SEP
                        + " adjustment layer name: "
                        + adjustmentLayerName
                        + SEP
                        + " geometry: "
                        + geometry
                        + SEP
                        + " distance: "
                        + distance
                        + SEP
                        + " altitude index: "
                        + altitudeIndex
                        + SEP
                        + " altitude name: "
                        + altitudeName);
        if (projection != null) {
            LOGGER.fine(" targetProjection: " + projection.getName());
        }

        // Project to CRS of provided geometry, if projection parameter is not provided
        if (projection == null && geometry.getUserData() instanceof CoordinateReferenceSystem) {
            projection = (CoordinateReferenceSystem) geometry.getUserData();
        }

        CoverageInfo coverageInfo = geoServer.getCatalog().getCoverageByName(layerName);

        FeatureSource adjustmentFeatureSource =
                getAdjustmentLayerFeatureSource(adjustmentLayerName);

        Geometry denseLine;
        // If geometry does not contain any info on CRS we will use CRS of layer
        CoordinateReferenceSystem defaultCrs = coverageInfo.getCRS();
        if (geometry instanceof LineString) {
            if (geometry.getUserData() != null) {
                defaultCrs = (CoordinateReferenceSystem) geometry.getUserData();
            }
            denseLine = densifyLine(distance, (LineString) geometry, defaultCrs);
        } else {
            denseLine = geometry;
        }

        if (!coverageInfo.getCRS().equals(defaultCrs)) {
            denseLine = reprojectGeometry(defaultCrs, coverageInfo.getCRS(), denseLine);
        }

        List<ProfileInfo> profileInfos = new ArrayList<>();
        double positiveAltitude = 0;
        double negativeAltitude = 0;
        double previousAltitude = 0;
        double totalDistance = 0;
        Geometry previousPoint = null;

        CoordinateReferenceSystem coverageCrs = coverageInfo.getCRS();

        // Create an array with all geometry vertices
        Coordinate[] coords = denseLine.getCoordinates();
        List<ProfileVertice> vertices =
                IntStream.range(0, coords.length)
                        .mapToObj(i -> new ProfileVertice(i, coords[i], null))
                        .collect(Collectors.toList());

        // Divide vertices array in chunks
        int chunkSize = 1000;
        try {
            chunkSize =
                    Integer.parseInt(
                            GeoServerExtensions.getProperty("wpsLongitudinalVerticesChunkSize"));
        } catch (NumberFormatException e) {
            LOGGER.warning(
                    "Can't parse wpsLongitudinalVerticesChunkSize property, must be an integer. Will use 1000 instead.");
        }
        List<List<ProfileVertice>> chunks = divide(vertices, chunkSize);

        List<Future<List<ProfileVertice>>> treated = new ArrayList<>();
        GridCoverage2DReader gridCoverageReader =
                (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
        GridCoverage2D gridCoverage2D = gridCoverageReader.read(null);

        // Process parallel altitude reading
        for (List<ProfileVertice> chunk : chunks) {
            treated.add(
                    executor.submit(
                            new AltitudeReaderThread(
                                    chunk,
                                    altitudeIndex,
                                    adjustmentFeatureSource,
                                    altitudeName,
                                    gridCoverage2D)));
        }

        List<ProfileVertice> result = new ArrayList<>();
        for (Future<List<ProfileVertice>> f : treated) {
            result.addAll(f.get());
        }

        // Sort vertices by their number
        result.sort(
                new Comparator<ProfileVertice>() {
                    @Override
                    public int compare(ProfileVertice pv1, ProfileVertice pv2) {
                        return pv1.number.compareTo(pv2.number);
                    }
                });

        for (ProfileVertice v : result) {
            CoordinateSequence2D coordinateSequence =
                    new CoordinateSequence2D(v.getCoordinate().getX(), v.getCoordinate().getY());
            Geometry point = new Point(coordinateSequence, GEOMETRY_FACTORY);

            double profileAltitude = v.getAltitude() - previousAltitude;
            if (projection != null) {
                point = reprojectGeometry(coverageCrs, projection, point);
            }

            Coordinate coordinate = point.getCoordinate();

            double slope = 0;
            ProfileInfo currentInfo;
            if (previousPoint == null) {
                currentInfo =
                        new ProfileInfo(
                                0, coordinate.getX(), coordinate.getY(), v.getAltitude(), slope);
            } else {
                double distanceToPrevious = point.distance(previousPoint);

                totalDistance += distanceToPrevious;
                slope = calculateSlope(projection, previousPoint, point, v.getAltitude());
                currentInfo =
                        new ProfileInfo(
                                totalDistance,
                                coordinate.getX(),
                                coordinate.getY(),
                                v.getAltitude(),
                                slope);
            }
            if (profileAltitude >= 0) {
                positiveAltitude += profileAltitude;
            } else {
                negativeAltitude += profileAltitude;
            }

            previousAltitude = v.getAltitude();

            profileInfos.add(currentInfo);
            previousPoint = point;
        }

        OperationInfo operationInfo =
                buildOperationInfo(
                        layerName,
                        startTime,
                        profileInfos,
                        positiveAltitude,
                        negativeAltitude,
                        totalDistance);

        return new LongitudinalProfileProcessResult(profileInfos, operationInfo);
    }

    private Geometry densifyLine(
            Double distance, LineString lineString, CoordinateReferenceSystem crs) {
        double distanceInTargetCrsUnits =
                metersToCrsUnits(crs, lineString.getCentroid().getCoordinate(), distance);
        Geometry denseLine = densify(lineString, distanceInTargetCrsUnits);
        return denseLine;
    }

    private static double calculateSlope(
            CoordinateReferenceSystem projection,
            Geometry previousPoint,
            Geometry point,
            double altitude)
            throws TransformException {
        return altitude * 100 / distanceInMeters(projection, previousPoint, point);
    }

    /**
     * Attempts to calculate distance between 2 points in meters. If CRS is not using meters, then
     * attempting to reproject points to EPSG:3857 which supports them
     *
     * @param projection
     * @param previousPoint
     * @param point
     * @return
     * @throws FactoryException
     * @throws TransformException
     */
    private static double distanceInMeters(
            CoordinateReferenceSystem projection, Geometry previousPoint, Geometry point)
            throws TransformException {
        double distanceToPrevious;
        if (projection instanceof GeographicCRS) {
            GeodeticCalculator gc = new GeodeticCalculator(projection);
            gc.setStartingPosition(JTS.toDirectPosition(previousPoint.getCoordinate(), projection));
            gc.setDestinationPosition(JTS.toDirectPosition(point.getCoordinate(), projection));

            distanceToPrevious = gc.getOrthodromicDistance();
        } else {
            distanceToPrevious = point.distance(previousPoint);
        }
        return distanceToPrevious;
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

    private static Geometry reprojectGeometry(
            CoordinateReferenceSystem source, CoordinateReferenceSystem target, Geometry geometry)
            throws FactoryException, TransformException {
        MathTransform tx = CRS.findMathTransform(source, target, true);

        return JTS.transform(geometry, tx);
    }

    private FeatureSource<? extends FeatureType, ? extends Feature> getAdjustmentLayerFeatureSource(
            String adjustmentLayerName) throws IOException {
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource = null;
        if (adjustmentLayerName != null && !adjustmentLayerName.isBlank()) {
            LayerInfo adjustmentLayer = geoServer.getCatalog().getLayerByName(adjustmentLayerName);
            FeatureTypeInfo resource = (FeatureTypeInfo) adjustmentLayer.getResource();
            featureSource = resource.getFeatureSource(null, null);
        }
        return featureSource;
    }

    private double metersToCrsUnits(
            CoordinateReferenceSystem crs, Coordinate centroidCoord, double distanceInMeters) {
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
            Unit<Length> unit = (Unit<Length>) crs.getCoordinateSystem().getAxis(0).getUnit();
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
            parts.add(new ArrayList<ProfileVertice>(list.subList(i, Math.min(N, i + L))));
        }
        return parts;
    }

    /** Object for storing result of longitudinal profile WPS process */
    public static final class LongitudinalProfileProcessResult {

        public LongitudinalProfileProcessResult(
                List<ProfileInfo> profileInfoList, OperationInfo operationInfo) {
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

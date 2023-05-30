/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import static org.locationtech.jts.densify.Densifier.densify;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.jaitools.jts.CoordinateSequence2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

@DescribeProcess(
        title = "Longitudinal Profile Process",
        description =
                "The process splits provided linestring to segments of provided in paramters length, "
                        + "or smaller if distance between points is less then parameter length, "
                        + "then evaluates elevation for each point and builds longitudinal profile. "
                        + "Elevation will be adjusted if adjustment layer is provided as parameter. "
                        + "Also supports reprojection to different crs")
public class LongitudinalProfileProcess implements GeoServerProcess {

    static final Logger LOGGER = Logging.getLogger(LongitudinalProfileProcess.class);

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final String SEP = System.lineSeparator();

    private final GeoServer geoServer;

    public LongitudinalProfileProcess(GeoServer geoServer) {
        this.geoServer = geoServer;
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
            @DescribeParameter(name = "linestringWkt", description = "linestring wkt", min = 1)
                    LineString linestringWkt,
            @DescribeParameter(name = "distance", description = "distance between points", min = 1)
                    Double distance,
            @DescribeParameter(name = "projection", description = "projection for result", min = 0)
                    CoordinateReferenceSystem projection,
            @DescribeParameter(
                            name = "elevationIndex",
                            description = "index of elevation in coordinate array",
                            min = 0,
                            defaultValue = "0")
                    Integer elevationIndex,
            @DescribeParameter(
                            name = "elevationName",
                            description = "name of elevation attribute on adjustment layer",
                            min = 0)
                    String elevationName)
            throws IOException, FactoryException, TransformException, CQLException {

        long startTime = System.currentTimeMillis();
        LOGGER.info(
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
                        + " linestring wkt: "
                        + linestringWkt
                        + SEP
                        + " distance: "
                        + distance
                        + SEP
                        + " elevation index: "
                        + elevationIndex
                        + SEP
                        + " elevation name: "
                        + elevationName);
        if (projection != null) {
            LOGGER.info(" projection: " + projection.getName());
        }

        Geometry denseLine = densify(linestringWkt, distance);
        CoverageInfo coverageInfo = geoServer.getCatalog().getCoverageByName(layerName);

        FeatureSource featureSource = getAdjustmentLayerFeatureSource(adjustmentLayerName);

        GridCoverage2DReader gridCoverageReader =
                (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
        GridCoverage2D gridCoverage2D = gridCoverageReader.read(null);

        List<ProfileInfo> profileInfos = new ArrayList<>();
        double positiveElevation = 0;
        double negativeElevation = 0;
        double previousElevation = 0;
        double totalDistance = 0;
        Geometry previousPoint = null;

        CoordinateReferenceSystem coverageCrs = coverageInfo.getCRS();
        List<DirectPosition2D> positions2D =
                Arrays.stream(denseLine.getCoordinates())
                        .map(
                                coordinate ->
                                        new DirectPosition2D(
                                                coverageCrs, coordinate.x, coordinate.y))
                        .collect(Collectors.toList());

        for (DirectPosition2D position2D : positions2D) {
            LOGGER.info("processing position:" + position2D);
            CoordinateSequence2D coordinateSequence =
                    new CoordinateSequence2D(position2D.getX(), position2D.getY());
            Geometry point = new Point(coordinateSequence, GEOMETRY_FACTORY);

            double elevation =
                    getElevation(
                            featureSource,
                            gridCoverage2D,
                            previousElevation,
                            position2D,
                            point,
                            elevationIndex,
                            elevationName);

            if (projection != null) {
                point = reprojectPoint(coverageCrs, projection, point);
            }

            Coordinate coordinate = point.getCoordinate();

            double slope = 0;
            ProfileInfo currentInfo;
            if (previousPoint == null) {
                currentInfo =
                        new ProfileInfo(0, coordinate.getX(), coordinate.getY(), elevation, slope);
            } else {
                double distanceToPrevious = point.distance(previousPoint);

                totalDistance += distanceToPrevious;
                slope = calculateSlope(projection, previousPoint, point, elevation);
                currentInfo =
                        new ProfileInfo(
                                totalDistance,
                                coordinate.getX(),
                                coordinate.getY(),
                                elevation,
                                slope);
            }
            if (elevation >= 0) {
                positiveElevation += elevation;
            } else {
                negativeElevation += elevation;
            }

            previousElevation = elevation;

            profileInfos.add(currentInfo);
            previousPoint = point;
        }

        OperationInfo operationInfo =
                buildOperationInfo(
                        layerName,
                        startTime,
                        profileInfos,
                        positiveElevation,
                        negativeElevation,
                        totalDistance);

        return new LongitudinalProfileProcessResult(profileInfos, operationInfo);
    }

    private static double calculateSlope(
            CoordinateReferenceSystem projection,
            Geometry previousPoint,
            Geometry point,
            double elevation)
            throws FactoryException, TransformException {
        return elevation * 100 / distanceInMeters(projection, previousPoint, point);
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
            throws FactoryException, TransformException {
        double distanceToPrevious;
        if (projection != null && "WGS 84".equals(projection.getName().getCode())) {
            // In order to get distance in meters we will reproject points to EPSG:3857
            CoordinateReferenceSystem crs = CRS.decode("EPSG:3857");
            Geometry previousCoordinate = reprojectPoint(projection, crs, previousPoint);
            Geometry currentCoordinate = reprojectPoint(projection, crs, point);
            distanceToPrevious = currentCoordinate.distance(previousCoordinate);
        } else {
            distanceToPrevious = point.distance(previousPoint);
        }
        return distanceToPrevious;
    }

    private static double getElevation(
            FeatureSource featureSource,
            GridCoverage2D gridCoverage2D,
            double previousElevation,
            DirectPosition2D position2D,
            Geometry point,
            int elevationIndex,
            String elevationName)
            throws IOException, CQLException {
        double elevation =
                calculateElevation(
                        gridCoverage2D.evaluate(position2D), previousElevation, elevationIndex);
        // Round elevation
        elevation = BigDecimal.valueOf(elevation).setScale(2, RoundingMode.HALF_UP).doubleValue();

        if (featureSource != null) {
            elevation = getAdjustedElevation(featureSource, point, elevation, elevationName);
        }
        return elevation;
    }

    private static OperationInfo buildOperationInfo(
            String layerName,
            long startTime,
            List<ProfileInfo> profileInfos,
            double positiveElevation,
            double negativeElevation,
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
        operationInfo.setElevationPositive(positiveElevation);
        operationInfo.setElevationNegative(negativeElevation);
        operationInfo.setLayer(layerName);

        operationInfo.setExecutedTime(System.currentTimeMillis() - startTime);
        return operationInfo;
    }

    /**
     * Reprojects point to target CRS
     *
     * @param source
     * @param target
     * @param geometry
     * @return
     * @throws FactoryException
     * @throws TransformException
     */
    private static Geometry reprojectPoint(
            CoordinateReferenceSystem source, CoordinateReferenceSystem target, Geometry geometry)
            throws FactoryException, TransformException {
        MathTransform tx = CRS.findMathTransform(source, target, true);
        GeometryCoordinateSequenceTransformer gtx = new GeometryCoordinateSequenceTransformer();
        gtx.setMathTransform(tx);

        Coordinate coordinate = geometry.getCoordinate();
        CoordinateSequence2D coordinateSequence =
                new CoordinateSequence2D(coordinate.getX(), coordinate.getY());
        return gtx.transform(new Point(coordinateSequence, GEOMETRY_FACTORY));
    }

    /**
     * Process elevation using adjustment layer
     *
     * @param featureSource
     * @param geometry
     * @param elevation
     * @return
     * @throws IOException
     */
    private static double getAdjustedElevation(
            FeatureSource featureSource, Geometry geometry, double elevation, String elevationName)
            throws IOException, CQLException {
        Query query;
        Filter filter;
        Coordinate coordinate = geometry.getCoordinate();
        filter =
                CQL.toFilter(
                        "CONTAINS(the_geom, POINT("
                                + coordinate.getX()
                                + " "
                                + coordinate.getY()
                                + "))");
        query = new Query(featureSource.getSchema().getName().getLocalPart(), filter);
        FeatureIterator featureIterator = featureSource.getFeatures(query).features();
        if (featureIterator.hasNext()) {
            Feature feature = featureIterator.next();
            Double adjLayerElevation = (Double) feature.getProperty(elevationName).getValue();
            elevation = elevation - adjLayerElevation;
        }
        return elevation;
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

    private static double calculateElevation(
            Object obj, double previousElevation, int elevationIndex) {
        Class<?> objectClass = obj.getClass();
        if (objectClass.isArray()) {
            switch (objectClass.getComponentType().getName()) {
                case "byte":
                    return ((byte[]) obj)[elevationIndex] - previousElevation;
                case "int":
                    return ((int[]) obj)[elevationIndex] - previousElevation;
                case "float":
                    return ((float[]) obj)[elevationIndex] - previousElevation;
                case "double":
                    return ((double[]) obj)[elevationIndex] - previousElevation;
                default:
                    // Do nothing
            }
        }
        throw new IllegalArgumentException();
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

/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.filter.Filter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.Position2D;
import org.geotools.util.logging.Logging;
import org.jaitools.jts.CoordinateSequence2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class AltitudeReaderThread implements Callable<List<ProfileVertice>> {
    static final Logger LOGGER = Logging.getLogger(AltitudeReaderThread.class);

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private List<ProfileVertice> pvs;
    GridCoverage2D gridCoverage2D;
    private int altitudeIndex;
    FeatureSource adjustmentFeatureSource;
    String altitudeName;

    public AltitudeReaderThread(
            List<ProfileVertice> pvs,
            int altitudeIndex,
            FeatureSource adjustmentFeatureSource,
            String altitudeName,
            GridCoverage2D gridCoverage2D) {
        this.pvs = pvs;
        this.gridCoverage2D = gridCoverage2D;
        this.altitudeIndex = altitudeIndex;
        this.adjustmentFeatureSource = adjustmentFeatureSource;
        this.altitudeName = altitudeName;
    }

    @Override
    public List<ProfileVertice> call() throws Exception {
        Coordinate[] coords =
                pvs.stream()
                        .map(ProfileVertice::getCoordinate)
                        .collect(Collectors.toList())
                        .toArray(new Coordinate[pvs.size()]);
        Geometry geometry = GEOMETRY_FACTORY.createLineString(coords);

        Map<Geometry, Double> adjGeomValues = new HashMap<>();
        if (adjustmentFeatureSource != null) {
            Query query;
            Filter filter;
            filter = CQL.toFilter("INTERSECTS(the_geom, " + geometry.toText() + ")");
            query = new Query(adjustmentFeatureSource.getSchema().getName().getLocalPart(), filter);
            try (FeatureIterator<?> featureIterator =
                    adjustmentFeatureSource.getFeatures(query).features()) {
                while (featureIterator.hasNext()) {
                    Feature f = featureIterator.next();
                    Geometry g = (Geometry) f.getDefaultGeometryProperty().getValue();
                    Double altitude = (Double) f.getProperty(altitudeName).getValue();
                    adjGeomValues.put(g, altitude);
                }
            }
        }

        for (ProfileVertice pv : pvs) {
            LOGGER.fine("processing position:" + pv.getCoordinate());
            Double altCorrection = 0.0;
            for (Map.Entry<Geometry, Double> entry : adjGeomValues.entrySet()) {
                if (entry.getKey()
                        .intersects(
                                new Point(
                                        new CoordinateSequence2D(
                                                pv.getCoordinate().x, pv.getCoordinate().y),
                                        GEOMETRY_FACTORY))) {
                    altCorrection = entry.getValue();
                    break;
                }
            }

            double altitude =
                    getAltitude(
                            gridCoverage2D,
                            new Position2D(pv.getCoordinate().x, pv.getCoordinate().y),
                            altitudeIndex);
            pv.setAltitude(altitude - altCorrection);
        }
        return pvs;
    }

    private static double getAltitude(
            GridCoverage2D gridCoverage2D, Position2D position2D, int altitudeIndex) {
        double altitude = calculateAltitude(gridCoverage2D.evaluate(position2D), altitudeIndex);
        // Round altitude
        altitude = BigDecimal.valueOf(altitude).setScale(2, RoundingMode.HALF_UP).doubleValue();

        return altitude;
    }

    private static double calculateAltitude(Object obj, int altitudeIndex) {
        Class<?> objectClass = obj.getClass();
        if (objectClass.isArray()) {
            switch (objectClass.getComponentType().getName()) {
                case "byte":
                    return ((byte[]) obj)[altitudeIndex];
                case "int":
                    return ((int[]) obj)[altitudeIndex];
                case "float":
                    return ((float[]) obj)[altitudeIndex];
                case "double":
                    return ((double[]) obj)[altitudeIndex];
                default:
                    // Do nothing
            }
        }
        throw new IllegalArgumentException();
    }
}

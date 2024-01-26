package org.geoserver.wps.longitudinal;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.filter.Filter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.Position2D;
import org.geotools.util.logging.Logging;

public class AltitudeReaderThread implements Callable<Vector<ProfileVertice>> {
    static final Logger LOGGER = Logging.getLogger(AltitudeReaderThread.class);

    private Vector<ProfileVertice> pvs;
    GridCoverage2D gridCoverage2D;
    private int altitudeIndex;
    FeatureSource adjustmentFeatureSource;
    String altitudeName;

    public AltitudeReaderThread(
            Vector<ProfileVertice> pvs,
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
    public Vector<ProfileVertice> call() throws Exception {
        for (ProfileVertice pv : pvs) {
            LOGGER.fine("processing position:" + pv.getCoordinate());
            double altitude =
                    getAltitude(
                            adjustmentFeatureSource,
                            gridCoverage2D,
                            pv.getCoordinate(),
                            altitudeIndex,
                            altitudeName);
            pv.setAltitude(altitude);
        }
        return pvs;
    }

    private static double getAltitude(
            FeatureSource featureSource,
            GridCoverage2D gridCoverage2D,
            Position2D position2D,
            int altitudeIndex,
            String altitudeName)
            throws IOException, CQLException {
        double altitude = calculateAltitude(gridCoverage2D.evaluate(position2D), altitudeIndex);
        // Round altitude
        altitude = BigDecimal.valueOf(altitude).setScale(2, RoundingMode.HALF_UP).doubleValue();

        if (featureSource != null) {
            altitude = getAdjustedAltitude(featureSource, position2D, altitude, altitudeName);
        }
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

    /**
     * Process altitude using adjustment layer. Method attempts to find feature that contains
     * provided parameter point geometry, and if found, subtracts its altitude value from provided
     * parameter altitude
     *
     * @param featureSource
     * @param coordinate
     * @param altitude
     * @return
     * @throws IOException
     */
    private static double getAdjustedAltitude(
            FeatureSource featureSource,
            Position2D coordinate,
            double altitude,
            String altitudeName)
            throws IOException, CQLException {
        Query query;
        Filter filter;
        filter =
                CQL.toFilter(
                        "CONTAINS(the_geom, POINT("
                                + coordinate.getX()
                                + " "
                                + coordinate.getY()
                                + "))");
        query = new Query(featureSource.getSchema().getName().getLocalPart(), filter);
        try (FeatureIterator featureIterator = featureSource.getFeatures(query).features()) {
            if (featureIterator.hasNext()) {
                Feature feature = featureIterator.next();
                Double adjLayerAltitude = (Double) feature.getProperty(altitudeName).getValue();
                altitude = altitude - adjLayerAltitude;
            }
        }
        return altitude;
    }
}

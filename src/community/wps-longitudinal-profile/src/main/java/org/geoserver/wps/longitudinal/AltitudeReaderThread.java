/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.geotools.util.logging.Logging;

public class AltitudeReaderThread implements Callable<ArrayList<ProfileVertice>> {
    static final Logger LOGGER = Logging.getLogger(AltitudeReaderThread.class);

    private ArrayList<ProfileVertice> pvs;
    GridCoverage2D gridCoverage2D;
    private int altitudeIndex;

    public AltitudeReaderThread(
            ArrayList<ProfileVertice> pvs, int altitudeIndex, GridCoverage2D gridCoverage2D) {
        this.pvs = pvs;
        this.gridCoverage2D = gridCoverage2D;
        this.altitudeIndex = altitudeIndex;
    }

    @Override
    public ArrayList<ProfileVertice> call() throws Exception {
        for (ProfileVertice pv : pvs) {
            LOGGER.fine("processing position:" + pv.getCoordinate());
            double altitude = getAltitude(gridCoverage2D, pv.getCoordinate(), altitudeIndex);
            pv.setAltitude(altitude - pv.getAltitude());
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

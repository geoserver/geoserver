/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.dggs.rhealpix;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jep.SharedInterpreter;
import org.geotools.data.Parameter;
import org.geotools.dggs.DGGSFactory;
import org.geotools.dggs.DGGSInstance;
import org.geotools.util.logging.Logging;

/**
 * Factory for rHealPix DGGS instances. For now it always returns a TB16-Pix instance, but it should
 * be easy to extent to allow any parametrization.
 */
public class RHealPixDGGSFactory implements DGGSFactory {

    static final Logger LOGGER = Logging.getLogger(RHealPixDGGSFactory.class);

    @Override
    public String getId() {
        return "rHEALPix";
    }

    @Override
    public Parameter[] getParametersInfo() {
        // for the time being keep it simple, but this one will actually have params like
        // the ellipsoid, number of sides, and the like
        return new Parameter[0];
    }

    @Override
    public DGGSInstance createInstance(Map<String, Serializable> params) throws IOException {
        JEPWebRuntime.Initializer initializer =
                interpreter -> {
                    interpreter.exec("from rhealpixdggs import dggs, ellipsoids");
                    interpreter.exec("from rhealpixdggs.ellipsoids import Ellipsoid");
                    interpreter.exec("from rhealpixdggs.dggs import RHEALPixDGGS, Cell");
                    interpreter.exec(
                            "WGS84_TB16 = Ellipsoid(a=6378137.0, b=6356752.314140356, e=0.0578063088401, f=0.003352810681182, lon_0=-131.25)");
                    interpreter.exec(
                            "dggs = RHEALPixDGGS(ellipsoid=WGS84_TB16, north_square=0, south_square=0, N_side=3)");
                };
        return new RHealPixDGGSInstance(new JEPWebRuntime(initializer), "TB16-Pix");
    }

    @Override
    public boolean isAvailable() {
        try (SharedInterpreter interpreter = new SharedInterpreter()) {
            interpreter.exec("from rhealpixdggs import dggs");
            interpreter.close();
            return true;
        } catch (Exception | UnsatisfiedLinkError e) {
            LOGGER.log(Level.FINE, "Could not instantiate a rHEALPix DGGS", e);
            return false;
        }
    }
}

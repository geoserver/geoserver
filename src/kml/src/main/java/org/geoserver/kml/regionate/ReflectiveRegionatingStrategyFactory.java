/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.regionate;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;

public class ReflectiveRegionatingStrategyFactory implements RegionatingStrategyFactory {
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.geosearch");

    String myName;
    String myClassName;
    Class myStrategyClass;
    GeoServer gs;

    public ReflectiveRegionatingStrategyFactory(String name, String className, GeoServer gs) {
        myName = name;
        myClassName = className;
        this.gs = gs;
    }

    public ReflectiveRegionatingStrategyFactory(String name, Class strategy, GeoServer gs) {
        myName = name;
        myStrategyClass = strategy;
        this.gs = gs;
    }

    public boolean canHandle(String strategyName) {
        return (myName != null) && myName.equalsIgnoreCase(strategyName);
    }

    public String getName() {
        return myName;
    }

    public RegionatingStrategy createStrategy() {
        try {
            Class clazz = getStrategyClass();
            Constructor c = clazz.getConstructor(GeoServer.class);
            if (c != null) {
                return (RegionatingStrategy) c.newInstance(gs);
            }

            return (RegionatingStrategy) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected Class getStrategyClass() {
        if (myStrategyClass != null) return myStrategyClass;

        try {
            myStrategyClass = Class.forName(myClassName);
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Failed to find class " + myClassName + " for ReflectiveRegionatingStrategy.",
                    e);
        }

        return myStrategyClass;
    }
}

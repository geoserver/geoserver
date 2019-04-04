/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.BoundingBox;

/**
 * Class that extracts information from an ows request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class RequestObjectHandler {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    String reqObjClassName;
    protected MonitorConfig monitorConfig;

    protected RequestObjectHandler(String reqObjClassName, MonitorConfig config) {
        this.reqObjClassName = reqObjClassName;
        this.monitorConfig = config;
    }

    public boolean canHandle(Object request) {
        Class<?> clazz;
        try {
            clazz = Class.forName(reqObjClassName);
        } catch (ClassNotFoundException e) {
            return false;
        }

        return clazz.isInstance(request);
    }

    public void handle(Object request, RequestData data) {
        try {
            data.setResources(getLayers(request));
            if (monitorConfig.getBboxMode() != MonitorConfig.BboxMode.NONE) {
                data.setBbox(getBBox(request));
            }
        } catch (Exception e) {
            // TODO; rather than just catch and log we should add a configuration parameter,
            // development vs production, and throw the exception in development mode
            LOGGER.log(Level.WARNING, "Error handling request object", e);
        }
    }

    protected abstract List<String> getLayers(Object request);

    /** Find a bounding box for the area covered by the request. */
    protected BoundingBox getBBox(Object request) {
        return null;
    }
}

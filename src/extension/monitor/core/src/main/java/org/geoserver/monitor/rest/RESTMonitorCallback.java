/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestData.Category;
import org.geoserver.rest.DispatcherCallback;
import org.geoserver.rest.DispatcherCallbackAdapter;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RESTMonitorCallback extends DispatcherCallbackAdapter {

    static final Logger LOGGER = Logging.getLogger(RESTMonitorCallback.class);

    Monitor monitor;

    @Autowired
    public RESTMonitorCallback(Monitor monitor) {
        this.monitor = monitor;
    }

    public void init(HttpServletRequest request, HttpServletResponse response) {
        RequestData data = monitor.current();
        if (data == null) {
            // will happen in cases where the filter is not active
            return;
        }

        data.setCategory(Category.REST);
        if (request.getPathInfo() != null) {
            String resource = request.getPathInfo();
            final String[] pathParts = resource.split("/");
            data.getResources().add(pathParts[pathParts.length - 1]);
        }
        monitor.update();
    }

    public void dispatched(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequestData data = monitor.current();
        if (data == null) {
            // will happen in cases where the filter is not active
            return;
        }

        try {
            // do not import these classes, dynamic lookup allows to break the dependency
            // on restconfig at runtime
            Object controllerBean = DispatcherCallback.getControllerBean(handler);
            if (controllerBean instanceof org.geoserver.rest.catalog.AbstractCatalogController
                    || controllerBean instanceof org.geoserver.rest.AbstractGeoServerController) {
                data.setService("RESTConfig");
            }
        } catch (Exception e) {
            // no problem, happens if restconfig is not in the classpath
            LOGGER.log(Level.FINE, "Error finding out if the call is a restconfig one", e);
        }

        monitor.update();
    }
}

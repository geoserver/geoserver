/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestData.Category;
import org.geoserver.rest.BeanDelegatingRestlet;
import org.geoserver.rest.DispatcherCallback;
import org.restlet.Restlet;
import org.restlet.Route;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class RESTMonitorCallback implements DispatcherCallback {

    Monitor monitor;
    
    public RESTMonitorCallback(Monitor monitor) {
        this.monitor = monitor;
    }
    
    public void init(Request request, Response response) {
        RequestData data = monitor.current();
        if (data == null) {
            //will happen in cases where the filter is not active
            return;
        }
        
        data.setCategory(Category.REST);
        if (request.getResourceRef() != null) {
            String resource = request.getResourceRef().getLastSegment();
            resource = FilenameUtils.getBaseName(resource);
            data.getResources().add(resource);
        }
        monitor.update();
    }

    public void dispatched(Request request, Response response, Restlet restlet) {
        RequestData data = monitor.current();
        if (data == null) {
            //will happen in cases where the filter is not active
            return;
        }
        
        if (restlet instanceof Route) {
            restlet = ((Route)restlet).getNext();
        }
        
        if (restlet instanceof BeanDelegatingRestlet) {
            restlet = ((BeanDelegatingRestlet)restlet).getBean();
        }
        
        if (restlet != null) {
            if (restlet.getClass().getPackage().getName().startsWith("org.geoserver.catalog.rest")) {
               data.setService("RESTConfig");
            }
        }
        
        monitor.update();
    }

    public void exception(Request request, Response response, Exception error) {
    }
    
    public void finished(Request request, Response response) {
    }
}

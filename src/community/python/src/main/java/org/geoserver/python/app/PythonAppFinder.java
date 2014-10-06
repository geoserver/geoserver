/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.app;

import java.io.File;
import java.io.IOException;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.python.Python;
import org.geoserver.rest.RestletException;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class PythonAppFinder extends PythonFinder {

    public PythonAppFinder(Python jython, GeoServerResourceLoader resourceLoader) {
        super(jython, resourceLoader);
    }
  
    @Override
    public Resource findTarget(Request request, Response response) {
        String app = (String) request.getAttributes().get("app") + ".py";
        if (app == null) {
            return new PythonListResource(jython, request, response);
        }

        File pyapp;
        try {
            pyapp = resourceLoader.find(jython.getAppRoot(), app);
        } catch (IOException e) {
            throw new RestletException("Error loading app " + app, 
                    Status.SERVER_ERROR_INTERNAL, e); 
        }
        if (pyapp == null) {
            throw new RestletException("No such app " + app, Status.CLIENT_ERROR_NOT_FOUND);
        }
        return new PythonAppResource(jython, pyapp, request, response);
    }
    
}

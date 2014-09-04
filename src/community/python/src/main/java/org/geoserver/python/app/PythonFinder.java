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
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class PythonFinder extends Finder {

    Python jython;
    GeoServerResourceLoader resourceLoader;
    
    public PythonFinder(Python jython, GeoServerResourceLoader resourceLoader) {
        this.jython = jython;
        this.resourceLoader = resourceLoader;
    }
   
    @Override
    public Resource findTarget(Request request, Response response) {
        String script = (String) request.getAttributes().get("script") + ".py";
        if (script == null) {
            return new PythonListResource(jython, request, response);
        }
        
        File pyscript;
        try {
            pyscript = resourceLoader.find(jython.getScriptRoot(), script);
        } catch (IOException e) {
            throw new RestletException("Error loading script " + script, 
                    Status.SERVER_ERROR_INTERNAL, e); 
        }
        if (pyscript == null) {
            throw new RestletException("No such script " + script, Status.CLIENT_ERROR_NOT_FOUND);
        }
        
        return new PythonResource(jython, pyscript, request, response);
    }
}

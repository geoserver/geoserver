/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.app;

import org.geoserver.python.Python;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

public class PythonListResource extends Resource {

    Python jython;
    
    public PythonListResource(Python jython, Request request, Response response) {
        super(null, request, response);
        this.jython = jython;
    }
}

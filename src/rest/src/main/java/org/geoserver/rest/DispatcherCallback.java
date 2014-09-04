/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.restlet.Restlet;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Provides callbacks for the life cycle of a rest request.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface DispatcherCallback {

    /**
     * Called at the start of a request cycle.
     */
    void init(Request request, Response response);

    /**
     * Called once a restlet has been dispatched or routed for a request.
     */
    void dispatched(Request request, Response response, Restlet restlet);
    
    /**
     * Called in the event of an exception occurring during a request. 
     */
    void exception(Request request, Response response, Exception error);
    
    /**
     * Final callback called once a request has been completed. 
     * <p>
     * This method is always called, even in the event of an exception during request processing. 
     * </p>
     */
    void finished(Request request, Response response);
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.events;

import java.util.Iterator;
import java.util.List;

import org.geoserver.rest.DispatcherCallback;
import org.geotools.util.logging.Logging;
import org.restlet.Restlet;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class RestDispatcherCallback implements DispatcherCallback {
    final static java.util.logging.Logger LOGGER = Logging.getLogger(RestDispatcherCallback.class);

    private static final ThreadLocal<List<Parameter>> parameters = new ThreadLocal<List<Parameter>>();

    public static List<Parameter> getParameters() {
        return parameters.get();
    }

    @Override
    public void init(Request request, Response response) {

        if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            final Iterator<Parameter> it = request.getResourceRef().getQueryAsForm().iterator();
            while (it.hasNext()) {
                Parameter p = it.next();
                if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
                    LOGGER.info("Registering incoming parameter: " + p.toString());
                }
            }
        }
        parameters.set(request.getResourceRef().getQueryAsForm());

        // check purge parameter to determine if the underlying file
        // should be deleted
        // boolean purge = (p != null) ? Boolean.parseBoolean(p) : false;
        // catalog.getResourcePool().deleteStyle(s, purge);

        // LOGGER.info( "DELETE style " + style);

    }

    @Override
    public void dispatched(Request request, Response response, Restlet restlet) {

    }

    @Override
    public void exception(Request request, Response response, Exception error) {

    }

    @Override
    public void finished(Request request, Response response) {

    }

}

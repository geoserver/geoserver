/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.rest.BeanDelegatingRestlet;
import org.geoserver.rest.DispatcherCallback;
import org.geoserver.security.AdminRequest;
import org.restlet.Restlet;
import org.restlet.Route;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Rest callback that sets the {@link AdminRequest} thread local.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class AdminRequestCallback implements DispatcherCallback {

    @Override
    public void init(Request request, Response response) {
    }

    @Override
    public void dispatched(Request request, Response response, Restlet restlet) {
        if (unwrap(restlet) instanceof AbstractCatalogFinder) {
            //restconfig request
            AdminRequest.start(this);
        }
    }

    @Override
    public void exception(Request request, Response response, Exception error) {
    }

    @Override
    public void finished(Request request, Response response) {
        AdminRequest.finish();
    }

    /**
     * unwraps the restlet passed to dispatched() to find the actual target.
     */
    Restlet unwrap(Restlet next) {
        if (next instanceof Route) {
            next = ((Route)next).getNext();
        }
        if (next instanceof BeanDelegatingRestlet) {
            next = ((BeanDelegatingRestlet) next).getBean();
        }
        return next;
    }

}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.rest;

import org.geoserver.script.ScriptManager;
import org.geoserver.security.GeoServerSecurityManager;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * Case class for finders that are part of the script rest api.
 * 
 * @author Justin Deoliveira, Boundless
 */
public abstract class FinderSupport extends Finder {

    protected ScriptManager scriptMgr;

    protected FinderSupport(ScriptManager scriptMgr) {
        this.scriptMgr = scriptMgr;
    }

    @Override
    public final Resource findTarget(Request request, Response response) {
        GeoServerSecurityManager secMgr = scriptMgr.getSecurityManager();

        // ensure user authenticated
        if (!secMgr.checkAuthenticationForAdminRole()) {
            response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
            return null;
        }

        // extra step of requiring that teh admin password has been changed
        if (secMgr.checkForDefaultAdminPassword()) {
            response.setStatus(Status.CLIENT_ERROR_FORBIDDEN, "insecure password");
            return null;
        }

        return doFindTarget(request, response);
    }

    protected abstract Resource doFindTarget(Request request, Response response);
}

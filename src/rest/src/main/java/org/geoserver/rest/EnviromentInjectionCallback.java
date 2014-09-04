/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.HashMap;
import java.util.Map;

import org.geotools.filter.function.EnvFunction;
import org.restlet.Restlet;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Injects the environment variables into the {@link EnvFunction} and clears them up at the end
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class EnviromentInjectionCallback implements DispatcherCallback {

    public void init(Request request, Response response) {
        Map<String, Object> envVars = new HashMap<String, Object>();
        
        // TODO: do we want to support a OWS like "env" param here?

        // inject the current user among the env vars
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
            String name = auth.getName();
            envVars.put("GSUSER", name);
        }

        // set it into the EnvFunction
        if (envVars.size() > 0) {
            EnvFunction.setLocalValues(envVars);
        }
    }

    public void dispatched(Request request, Response response, Restlet restlet) {
        // nothing to do
    }

    public void exception(Request request, Response response, Exception error) {
        // nothing to do
    }

    public void finished(Request request, Response response) {
        // clean up when we're done
        EnvFunction.clearLocalValues();
    }

}

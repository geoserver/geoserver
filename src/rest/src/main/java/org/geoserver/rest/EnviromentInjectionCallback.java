/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geotools.filter.function.EnvFunction;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Injects the environment variables into the {@link EnvFunction} and clears them up at the end
 *
 * @author Andrea Aime - GeoSolutions
 */
@Component
public class EnviromentInjectionCallback extends DispatcherCallbackAdapter {

    public void init(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> envVars = new HashMap<>();

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

    public void finished(HttpServletRequest request, HttpServletResponse response) {
        // clean up when we're done
        EnvFunction.clearLocalValues();
    }
}

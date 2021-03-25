/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.HashMap;
import java.util.Map;
import org.geotools.filter.function.EnvFunction;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Injects the enviroment variables into the {@link EnvFunction} and clears them up at the end
 *
 * @author Andrea Aime - GeoSolutions
 */
public class EnviromentInjectionCallback extends AbstractDispatcherCallback {

    @Override
    public Request init(Request request) {
        // see if we have an env map already parsed in the request
        Object obj = request.getKvp().get("env");
        @SuppressWarnings("unchecked")
        Map<String, Object> envVars = obj instanceof Map ? (Map) obj : null;

        // inject the current user in it
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
            String name = auth.getName();
            if (envVars == null) {
                envVars = new HashMap<>();
            }
            envVars.put("GSUSER", name);
        }

        // set it into the EnvFunction
        if (envVars != null) {
            EnvFunction.setLocalValues(envVars);
        }

        return request;
    }

    @Override
    public void finished(Request request) {
        // clean up when we're done
        EnvFunction.clearLocalValues();
    }
}

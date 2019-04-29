/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.RequestCycle;
import org.geotools.filter.function.EnvFunction;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class WicketEnvironmentVariableCallback implements WicketCallback {

    public void onBeginRequest() {
        // inject the current user in it
        Map<String, Object> envVars = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
            String name = auth.getName();
            if (envVars == null) {
                envVars = new HashMap<String, Object>();
            }
            envVars.put("GSUSER", name);
        }

        // set it into the EnvFunction
        if (envVars != null) {
            EnvFunction.setLocalValues(envVars);
        }
    }

    public void onAfterTargetsDetached() {
        // nothing to do
    }

    public void onEndRequest() {
        // clean up when we're done
        EnvFunction.clearLocalValues();
    }

    @Override
    public void onRequestTargetSet(
            RequestCycle cycle, Class<? extends IRequestablePage> requestTarget) {}

    @Override
    public void onRuntimeException(RequestCycle cycle, Exception ex) {
        // nothing to do
    }
}

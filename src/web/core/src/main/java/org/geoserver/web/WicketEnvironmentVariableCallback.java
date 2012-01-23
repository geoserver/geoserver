package org.geoserver.web;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.geotools.filter.function.EnvFunction;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;

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

    public void onRequestTargetSet(IRequestTarget requestTarget) {
        // nothing to do
    }

    public void onRuntimeException(Page page, RuntimeException e) {
        // nothing to do
    }

}

/**
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 *
 * @author David Vick, Boundless 2017
 */
package org.geoserver.script.rest.callback;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.rest.DispatcherCallbackAdapter;
import org.geoserver.rest.RestException;
import org.geoserver.script.ScriptManager;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * This class handles the security checks for the Scripts module it extends the
 * DispatcherCallbackAdapter from the Rest module which gets called for each rest request. The
 * security check is limited to only requests for the scripts module by checking for the following
 * pattern in the RequestURI "geoserver/rest/scripts".
 */
@Component
public class SecurityCallback extends DispatcherCallbackAdapter {

    @Autowired ScriptManager scriptManager;

    public void init(HttpServletRequest request, HttpServletResponse response) {
        GeoServerSecurityManager secMgr = scriptManager.getSecurityManager();
        String path = request.getRequestURI();

        if (path.contains("geoserver/rest/scripts")) {
            if (!secMgr.checkAuthenticationForAdminRole()) {
                throw new RestException("", HttpStatus.UNAUTHORIZED);
            }
            if (secMgr.checkForDefaultAdminPassword()) {
                throw new RestException("insecure password", HttpStatus.FORBIDDEN);
            }
        }
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.jasig.cas.client.session.SessionMappingStorage;

/**
 * Removes HTTP sessions recorded by CAS for a single sign out configuration
 *
 * @author christian
 */
public class SingleSignOutHttpSessionListener implements HttpSessionListener {

    private SessionMappingStorage sessionMappingStorage;

    public void sessionCreated(final HttpSessionEvent event) {
        // nothing to do at the moment
    }

    public void sessionDestroyed(final HttpSessionEvent event) {
        if (sessionMappingStorage == null) {
            sessionMappingStorage =
                    GeoServerCasAuthenticationFilter.getHandler().getSessionMappingStorage();
        }
        final HttpSession session = event.getSession();
        sessionMappingStorage.removeBySessionById(session.getId());
    }
}

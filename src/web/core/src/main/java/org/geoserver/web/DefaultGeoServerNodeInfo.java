/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.geoserver.GeoServerNodeData;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.spring.security.GeoServerSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Default node id customizer, will respond to a system variable, env variable or servlet context
 * variable named GEOSERVER_NODE_OPTS with the following sytanx:
 * <code>id:<theid>;background:<bgcolor>;color=<color><code>
 * The <code>background</code> and <code>color</code> properties are optional, the id can be a fixed
 * string or can contain the <code>$host_ip</code> or <code>$host_name</code> variable that will be
 * expanded to the first non loopback IP address of the machine, or the equivalent host name
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultGeoServerNodeInfo implements GeoServerNodeInfo {

    static final String GEOSERVER_NODE_OPTS = GeoServerNodeData.GEOSERVER_NODE_OPTS;

    static GeoServerNodeData NODE_DATA = null;

    static {
        initializeFromEnviroment();
    }

    @Override
    public GeoServerNodeData getData() {
        return NODE_DATA;
    }

    @Override
    public void customize(WebMarkupContainer container) {
        container.add(
                new AttributeAppender("style", new Model<String>(NODE_DATA.getIdStyle()), ";"));
        container.setVisible(isNodeIdVisible(container));
    }

    protected static void initializeFromEnviroment() {
        NODE_DATA = GeoServerNodeData.createFromEnvironment();
    }

    /** The element is visible if an admin is logged in, and the id is not null */
    protected boolean isNodeIdVisible(WebMarkupContainer parent) {
        if (NODE_DATA.getId() == null) {
            return false;
        }
        // we don't show the node id to all users, only to the admin
        Authentication auth = ((GeoServerSession) parent.getSession()).getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return false;
        } else {
            GeoServerSecurityManager securityManager =
                    GeoServerApplication.get().getSecurityManager();
            return securityManager.checkAuthenticationForAdminRole(auth);
        }
    }
}

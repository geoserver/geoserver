/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jdbcconfig.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.GeoServerHomePageContentProvider;

/** @author Kevin Smith, OpenGeo */
public class JDBCConfigStatusProvider implements GeoServerHomePageContentProvider {

    JDBCConfigProperties config;

    public JDBCConfigStatusProvider(JDBCConfigProperties config) {
        super();
        this.config = config;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        GeoServerSecurityManager secMgr = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        if (secMgr.checkAuthenticationForAdminRole() && config.isEnabled()) {
            return new JDBCConfigStatusPanel(id, config);
        }
        return new WebMarkupContainer(id); // Placeholder
    }
}

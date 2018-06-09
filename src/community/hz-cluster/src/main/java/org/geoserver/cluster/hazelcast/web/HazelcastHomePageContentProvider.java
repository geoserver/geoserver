/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.geoserver.cluster.hazelcast.HzCluster;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.GeoServerHomePageContentProvider;

public class HazelcastHomePageContentProvider implements GeoServerHomePageContentProvider {

    HzCluster cluster;

    public HazelcastHomePageContentProvider(HzCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        GeoServerSecurityManager secMgr = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        if (secMgr.checkAuthenticationForAdminRole() && cluster.isEnabled()) {
            return new NodeLinkPanel(id, cluster);
        }
        return new WebMarkupContainer(id);
    }
}

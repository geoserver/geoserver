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

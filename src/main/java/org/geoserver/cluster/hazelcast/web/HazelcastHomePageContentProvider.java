package org.geoserver.cluster.hazelcast.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.AdminComponentAuthorizer;
import org.geoserver.web.GeoServerHomePageContentProvider;
import org.springframework.security.core.context.SecurityContextHolder;

import com.hazelcast.core.HazelcastInstance;

public class HazelcastHomePageContentProvider implements GeoServerHomePageContentProvider {

    HazelcastInstance hz;

    public HazelcastHomePageContentProvider(HazelcastInstance hz) {
        this.hz = hz;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        GeoServerSecurityManager secMgr = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        if (secMgr.checkAuthenticationForAdminRole()) { 
            return new NodeLinkPanel(id, hz);
        }
        return new WebMarkupContainer(id);
    }

}

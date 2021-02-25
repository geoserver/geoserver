/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Testing class to check the roles involved on catalog and geoserver startup loading process should
 * be ADMIN.
 */
public class GeoServerRootStartupTest extends GeoServerSystemTestSupport {

    private volatile Collection<? extends GrantedAuthority> catalogRoles = null;
    private volatile Collection<? extends GrantedAuthority> geoServerRoles = null;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefault();
        RootStartupListener.setListener(
                new GeoServerLoaderListener() {

                    @Override
                    public void loadGeoServer(GeoServer geoServer, XStreamPersister xp) {
                        catalogRoles = getCurrentRoles();
                    }

                    @Override
                    public void loadCatalog(Catalog catalog, XStreamPersister xp) {
                        geoServerRoles = getCurrentRoles();
                    }
                });
    }

    @After
    public void onEnd() {
        RootStartupListener.setListener(GeoServerLoaderListener.EMPTY_LISTENER);
    }

    @Test
    public void testRootStartupCatalogLoad() {
        assertTrue(hasAdminRole(catalogRoles));
        assertTrue(hasAdminRole(geoServerRoles));
    }

    private boolean hasAdminRole(Collection<? extends GrantedAuthority> roles) {
        if (roles == null) return false;
        for (GrantedAuthority grantedAuthority : roles) {
            if (GeoServerRole.ADMIN_ROLE.equals(grantedAuthority)
                    || GeoServerRole.GROUP_ADMIN_ROLE.equals(grantedAuthority)) return true;
        }
        return false;
    }

    private static Collection<? extends GrantedAuthority> getCurrentRoles() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null || context.getAuthentication() == null) {
            return Collections.emptyList();
        }
        return context.getAuthentication().getAuthorities();
    }
}

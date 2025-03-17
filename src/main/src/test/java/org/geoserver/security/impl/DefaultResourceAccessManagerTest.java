/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geotools.api.filter.Filter;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class DefaultResourceAccessManagerTest {

    @Test
    public void getAccessLimits() {
        DataAccessRuleDAO dataAccessRuleDAO = mock(DataAccessRuleDAO.class);
        Catalog catalog = mock(Catalog.class);
        DefaultResourceAccessManager defaultResourceAccessManager =
                new DefaultResourceAccessManager(dataAccessRuleDAO, catalog);

        Authentication authentication = mock(Authentication.class);
        WorkspaceInfo workspaceInfo = mock(WorkspaceInfo.class);

        Collection<? extends GrantedAuthority> grantedAuthorities =
                Collections.singletonList(new GeoServerRole("ROLE_ADMINISTRATOR"));
        Mockito.<Collection<? extends GrantedAuthority>>when(authentication.getAuthorities())
                .thenReturn(grantedAuthorities);

        WorkspaceAccessLimits workspaceAccessLimits =
                defaultResourceAccessManager.getAccessLimits(authentication, workspaceInfo);

        assertNotNull(workspaceAccessLimits);
    }

    @Test
    public void testSecurityFilterCache() {
        Catalog catalog = mock(Catalog.class);
        Mockito.when(catalog.getWorkspaceByName("ws")).thenReturn(mock(WorkspaceInfo.class));
        LayerInfo layer1 = mock(LayerInfo.class);
        LayerInfo layer2 = mock(LayerInfo.class);
        ResourceInfo res1 = mock(ResourceInfo.class);
        ResourceInfo res2 = mock(ResourceInfo.class);
        Mockito.when(layer1.getResource()).thenReturn(res1);
        Mockito.when(layer2.getResource()).thenReturn(res2);
        Mockito.when(res1.getId()).thenReturn("resource1");
        Mockito.when(res2.getId()).thenReturn("resource2");
        Mockito.when(catalog.getLayerByName("ws:layer1")).thenReturn(layer1);
        Mockito.when(catalog.getLayerByName("ws:layer2")).thenReturn(layer2);

        DataAccessRuleDAO dataAccessRuleDAO = mock(DataAccessRuleDAO.class);

        DataAccessRule rule1 = new DataAccessRule("ws", "layer1", AccessMode.READ, "MY_ROLE");
        DataAccessRule rule2 = new DataAccessRule("ws", "layer2", AccessMode.READ, "OTHER_ROLE");
        Mockito.when(dataAccessRuleDAO.getRules()).thenReturn(Lists.newArrayList(rule1, rule2));

        DefaultResourceAccessManager defaultResourceAccessManager =
                new DefaultResourceAccessManager(dataAccessRuleDAO, catalog);

        Authentication authentication = mock(Authentication.class);
        Collection<? extends GrantedAuthority> grantedAuthorities =
                Collections.singletonList(new GeoServerRole("MY_ROLE"));
        Mockito.<Collection<? extends GrantedAuthority>>when(authentication.getAuthorities())
                .thenReturn(grantedAuthorities);

        Filter fil = defaultResourceAccessManager.getSecurityFilter(authentication, ResourceInfo.class);
        assertEquals(Predicates.not(Predicates.in("id", Collections.singletonList("resource2"))), fil);

        Filter fil2 = defaultResourceAccessManager.getSecurityFilter(authentication, ResourceInfo.class);
        // check if it is the same instance
        assertSame(fil2, fil);

        rule2 = new DataAccessRule("ws", "layer2", AccessMode.READ, "MY_ROLE");
        Mockito.when(dataAccessRuleDAO.getRules()).thenReturn(Lists.newArrayList(rule1, rule2));
        Mockito.when(dataAccessRuleDAO.getLastModified()).thenReturn(System.currentTimeMillis());

        fil = defaultResourceAccessManager.getSecurityFilter(authentication, ResourceInfo.class);
        assertEquals(Predicates.acceptAll(), fil);
    }
}

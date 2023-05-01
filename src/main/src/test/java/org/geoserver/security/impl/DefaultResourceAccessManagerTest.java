/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Collections;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.WorkspaceAccessLimits;
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
}

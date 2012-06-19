package org.geoserver.security;

import java.util.Arrays;
import java.util.List;

import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.authentication.TestingAuthenticationToken;

public class GeoServerSecurityManagerTest extends GeoServerSecurityTestSupport {

    public void testAdminRole() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        TestingAuthenticationToken auth = new TestingAuthenticationToken("admin", "geoserver", 
            (List) Arrays.asList(GeoServerRole.ADMIN_ROLE));
        auth.setAuthenticated(true);
        assertTrue(secMgr.checkAuthenticationForAdminRole(auth));


    }
}

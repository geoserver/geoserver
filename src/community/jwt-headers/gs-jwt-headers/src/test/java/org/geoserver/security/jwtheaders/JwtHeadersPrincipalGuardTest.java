/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilter;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The built-in root account is never taken from the header, admin only when allowed. */
public class JwtHeadersPrincipalGuardTest extends AbstractAuthenticationProviderTest {

    private static final String HEADER = "X-User";

    /** Header set by an authenticating proxy, roles from the default role service. */
    private GeoServerJwtHeadersFilterConfig proxyConfig(String name) {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setName(name);
        config.setClassName(GeoServerJwtHeadersFilter.class.getName());
        JwtConfiguration jwt = config.getJwtConfiguration();
        jwt.setValidateToken(false);
        jwt.setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat.STRING);
        jwt.setUserNameHeaderAttributeName(HEADER);
        config.setRoleSource(JWTHeaderRoleSource.RoleService);
        config.setRoleServiceName("default");
        return config;
    }

    private GeoServerJwtHeadersFilter saved(GeoServerJwtHeadersFilterConfig config) throws Exception {
        getSecurityManager().saveFilter(config);
        return (GeoServerJwtHeadersFilter) getSecurityManager().loadFilter(config.getName());
    }

    private Authentication run(GeoServerJwtHeadersFilter filter, String user) throws Exception {
        MockHttpServletRequest request = createRequest("web/");
        request.addHeader(HEADER, user);
        SecurityContextHolder.clearContext();
        AtomicReference<Authentication> seen = new AtomicReference<>();
        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (rq, rs) -> seen.set(SecurityContextHolder.getContext().getAuthentication()));
        SecurityContextHolder.clearContext();
        return seen.get();
    }

    private static List<String> authorities(Authentication auth) {
        return auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList());
    }

    @Test
    public void testRootIsRefused() throws Exception {
        GeoServerJwtHeadersFilterConfig config = proxyConfig("RootRefused");
        config.setAllowAdminLogin(true);
        GeoServerJwtHeadersFilter filter = saved(config);

        assertNull(run(filter, "root"));
        assertNull(run(filter, " ROOT "));
    }

    @Test
    public void testAdminRefusedByDefaultForNewFilters() throws Exception {
        GeoServerJwtHeadersFilter filter = saved(proxyConfig("AdminRefused"));

        assertNull(run(filter, "admin"));
        assertNull(run(filter, "Admin"));
    }

    @Test
    public void testAdminAllowedWhenEnabled() throws Exception {
        GeoServerJwtHeadersFilterConfig config = proxyConfig("AdminAllowed");
        config.setAllowAdminLogin(true);
        GeoServerJwtHeadersFilter filter = saved(config);

        Authentication auth = run(filter, "admin");
        assertEquals("admin", auth.getPrincipal());
        assertTrue(authorities(auth).contains("ROLE_ADMINISTRATOR"));
    }

    /**
     * A filter stored before the option existed has no allowAdminLogin element, and keeps letting the header assert
     * admin: the new-filter default must not apply on load.
     */
    @Test
    public void testStoredFilterWithoutOptionKeepsAllowingAdmin() throws Exception {
        GeoServerJwtHeadersFilterConfig config = proxyConfig("AdminStoredEarlier");
        config.setAllowAdminLogin(null);
        getSecurityManager().saveFilter(config);

        Resource stored = getSecurityManager().get("security/filter/AdminStoredEarlier/config.xml");
        String xml = new String(stored.getContents());
        assertFalse(xml, xml.contains("allowAdminLogin"));
        assertFalse(xml, xml.contains("trustUnvalidatedRolesHeader"));

        GeoServerJwtHeadersFilterConfig loaded =
                (GeoServerJwtHeadersFilterConfig) getSecurityManager().loadFilterConfig("AdminStoredEarlier", false);
        assertTrue(loaded.getAllowAdminLogin());
        assertFalse(loaded.getTrustUnvalidatedRolesHeader());

        GeoServerJwtHeadersFilter filter =
                (GeoServerJwtHeadersFilter) getSecurityManager().loadFilter("AdminStoredEarlier");
        Authentication auth = run(filter, "admin");
        assertEquals("admin", auth.getPrincipal());
    }

    @Override
    protected List<Filter> getFilters() {
        return new ArrayList<>();
    }
}

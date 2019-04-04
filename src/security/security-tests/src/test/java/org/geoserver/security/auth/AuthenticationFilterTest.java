/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.auth;

import static org.junit.Assert.*;

import java.net.URLEncoder;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.ConstantFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.*;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig.J2EERoleSource;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.filter.*;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.test.RunTestSetup;
import org.geoserver.test.SystemTest;
import org.geotools.data.Base64;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Category(SystemTest.class)
public class AuthenticationFilterTest extends AbstractAuthenticationProviderTest {

    public static final String testFilterName = "basicAuthTestFilter";
    public static final String testFilterName2 = "digestAuthTestFilter";
    public static final String testFilterName3 = "j2eeAuthTestFilter";
    public static final String testFilterName4 = "requestHeaderTestFilter";
    public static final String testFilterName5 = "basicAuthTestFilterWithRememberMe";
    public static final String testFilterName6 = "formLoginTestFilter";
    public static final String testFilterName7 = "formLoginTestFilterWithRememberMe";
    public static final String testFilterName8 = "x509TestFilter";
    public static final String testFilterName9 = "logoutTestFilter";
    public static final String testFilterName10 = "credentialsFromHeaderTestFilter";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        LogoutFilterConfig loConfig = new LogoutFilterConfig();
        loConfig.setClassName(GeoServerLogoutFilter.class.getName());
        loConfig.setName(testFilterName9);
        loConfig.setRedirectURL(GeoServerLogoutFilter.URL_AFTER_LOGOUT);
        getSecurityManager().saveFilter(loConfig);

        BasicAuthenticationFilterConfig bconfig = new BasicAuthenticationFilterConfig();
        bconfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        bconfig.setUseRememberMe(false);
        bconfig.setName(testFilterName);
        getSecurityManager().saveFilter(bconfig);
    }

    @Before
    public void revertFilters() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        if (secMgr.listFilters().contains(testFilterName2)) {
            SecurityFilterConfig config = secMgr.loadFilterConfig(testFilterName2);
            secMgr.removeFilter(config);
        }
    }

    @Test
    public void testBasicAuth() throws Exception {
        //        BasicAuthenticationFilterConfig config = new BasicAuthenticationFilterConfig();
        //        config.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        //        config.setUseRememberMe(false);
        //        config.setName(testFilterName);

        //        getSecurityManager().saveFilter(config);
        prepareFilterChain(pattern, testFilterName);

        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        String tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Basic") != -1);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        modifyChain(pattern, false, true, GeoServerSecurityFilterChain.ROLE_FILTER);
        // check success
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(
                                Base64.encodeBytes(
                                        (testUserName + ":" + testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        String roleString = response.getHeader(GeoServerRoleFilter.DEFAULT_HEADER_ATTRIBUTE);
        assertNotNull(roleString);
        String[] roles = roleString.split(";");
        assertEquals(3, roles.length);
        List<String> roleList = Arrays.asList(roles);
        assertTrue(roleList.contains(GeoServerRole.AUTHENTICATED_ROLE.getAuthority()));
        assertTrue(roleList.contains(rootRole));
        assertTrue(roleList.contains(derivedRole));

        // check wrong password
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(Base64.encodeBytes((testUserName + ":wrongpass").getBytes())));
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Basic") != -1);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check unknown user
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic " + new String(Base64.encodeBytes(("unknwon:" + testPassword).getBytes())));
        request.setMethod("GET");
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Basic") != -1);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check root user
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        // We need to enable Master Root login first
        MasterPasswordProviderConfig masterPasswordConfig =
                getSecurityManager()
                        .loadMasterPassswordProviderConfig(
                                getSecurityManager().getMasterPasswordConfig().getProviderName());
        masterPasswordConfig.setLoginEnabled(true);
        getSecurityManager().saveMasterPasswordProviderConfig(masterPasswordConfig);

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(
                                Base64.encodeBytes(
                                        (GeoServerUser.ROOT_USERNAME + ":" + getMasterPassword())
                                                .getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // checkForAuthenticatedRole(auth);
        assertEquals(GeoServerUser.ROOT_USERNAME, auth.getPrincipal());
        assertTrue(auth.getAuthorities().size() == 1);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        // check root user with wrong password
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(
                                Base64.encodeBytes(
                                        (GeoServerUser.ROOT_USERNAME + ":geoserver1").getBytes())));
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Basic") != -1);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check disabled user, clear cache first
        getSecurityManager().getAuthenticationCache().removeAll();
        updateUser("ug1", testUserName, false);
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(
                                Base64.encodeBytes(
                                        (testUserName + ":" + testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Basic") != -1);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        updateUser("ug1", testUserName, true);

        // Test anonymous
        insertAnonymousFilter();
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    @Test
    public void testCredentialsFromHeader() throws Exception {

        CredentialsFromRequestHeaderFilterConfig config =
                new CredentialsFromRequestHeaderFilterConfig();
        config.setClassName(GeoServerCredentialsFromRequestHeaderFilter.class.getName());
        config.setUserNameHeaderName("X-Credentials");
        config.setPasswordHeaderName("X-Credentials");
        config.setUserNameRegex("private-user=([^&]*)");
        config.setPasswordRegex("private-pw=([^&]*)");
        config.setParseAsUriComponents(true);
        config.setName(testFilterName10);
        getSecurityManager().saveFilter(config);

        prepareFilterChain(pattern, testFilterName10);
        modifyChain(pattern, false, true, null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check success
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addHeader(
                "X-Credentials", "private-user=" + testUserName + "&private-pw=" + testPassword);

        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        // check wrong password
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader(
                "X-Credentials", "private-user=" + testUserName + "&private-pw=wrongpass");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check unknown user
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addHeader("X-Credentials", "private-user=wronguser&private-pw=" + testPassword);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check root user
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        String masterPassword = URLEncoder.encode(getMasterPassword(), "UTF-8");
        request.addHeader(
                "X-Credentials",
                "private-user=" + GeoServerUser.ROOT_USERNAME + "&private-pw=" + masterPassword);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // checkForAuthenticatedRole(auth);
        assertEquals(GeoServerUser.ROOT_USERNAME, auth.getPrincipal());
        assertTrue(auth.getAuthorities().size() == 2);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        // check root user with wrong password
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader(
                "X-Credentials",
                "private-user=" + GeoServerUser.ROOT_USERNAME + "&private-pw=geoserver1");
        getProxy().doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check disabled user, clear cache first
        getSecurityManager().getAuthenticationCache().removeAll();
        updateUser("ug1", testUserName, false);
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addHeader(
                "X-Credentials", "private-user=" + testUserName + "&private-pw=" + testPassword);

        getProxy().doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        updateUser("ug1", testUserName, true);

        // Test anonymous
        insertAnonymousFilter();
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    @Test
    public void testJ2eeProxy() throws Exception {

        J2eeAuthenticationFilterConfig config = new J2eeAuthenticationFilterConfig();
        config.setClassName(GeoServerJ2eeAuthenticationFilter.class.getName());
        config.setName(testFilterName3);
        config.setRoleSource(J2EERoleSource.J2EE);
        config.setRoleServiceName("rs1");
        config.setUserGroupServiceName("ug1");
        config.setRolesHeaderAttribute("roles");
        getSecurityManager().saveFilter(config);

        prepareFilterChain(pattern, testFilterName3);

        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        Authentication auth;

        for (J2EERoleSource rs : J2EERoleSource.values()) {
            config.setRoleSource(rs);
            getSecurityManager().saveFilter(config);
            // test preauthenticated with various role sources
            request = createRequest("/foo/bar");
            request.setMethod("GET");
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();
            request.setUserPrincipal(
                    new Principal() {
                        @Override
                        public String getName() {
                            return testUserName;
                        }
                    });
            if (rs == J2EERoleSource.Header) {
                request.addHeader("roles", derivedRole + ";" + rootRole);
            }
            if (rs == J2EERoleSource.J2EE) {
                if (true) {
                    request.addUserRole(derivedRole);
                }
                if (false) {
                    request.addUserRole(rootRole);
                }
            }

            getProxy().doFilter(request, response, chain);

            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            ctx =
                    (SecurityContext)
                            request.getSession(true)
                                    .getAttribute(
                                            HttpSessionSecurityContextRepository
                                                    .SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(ctx);
            auth = ctx.getAuthentication();
            assertNotNull(auth);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            checkForAuthenticatedRole(auth);
            assertEquals(testUserName, auth.getPrincipal());
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        }
        // test root
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setUserPrincipal(
                new Principal() {
                    @Override
                    public String getName() {
                        return GeoServerUser.ROOT_USERNAME;
                    }
                });
        getProxy().doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // checkForAuthenticatedRole(auth);
        assertEquals(GeoServerUser.ROOT_USERNAME, auth.getPrincipal());
        assertTrue(auth.getAuthorities().size() == 1);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        config.setRoleServiceName(null);
        getSecurityManager().saveFilter(config);

        // test preauthenticated with active role service
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setUserPrincipal(
                new Principal() {
                    @Override
                    public String getName() {
                        return testUserName;
                    }
                });
        if (true) {
            request.addUserRole(derivedRole);
        }
        if (false) {
            request.addUserRole(rootRole);
        }
        getProxy().doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        // Test anonymous
        insertAnonymousFilter();
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    @Test
    public void testRequestHeaderProxy() throws Exception {

        RequestHeaderAuthenticationFilterConfig config =
                new RequestHeaderAuthenticationFilterConfig();
        config.setClassName(GeoServerRequestHeaderAuthenticationFilter.class.getName());
        config.setName(testFilterName4);
        config.setRoleServiceName("rs1");
        config.setPrincipalHeaderAttribute("principal");
        config.setRoleSource(PreAuthenticatedUserNameRoleSource.RoleService);
        config.setUserGroupServiceName("ug1");
        config.setPrincipalHeaderAttribute("principal");
        config.setRolesHeaderAttribute("roles");
        ;
        getSecurityManager().saveFilter(config);

        prepareFilterChain(pattern, testFilterName4);

        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        for (PreAuthenticatedUserNameRoleSource rs : PreAuthenticatedUserNameRoleSource.values()) {
            config.setRoleSource(rs);
            getSecurityManager().saveFilter(config);
            request = createRequest("/foo/bar");
            request.setMethod("GET");
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();
            request.addHeader("principal", testUserName);
            if (rs.equals(PreAuthenticatedUserNameRoleSource.Header)) {
                request.addHeader("roles", derivedRole + ";" + rootRole);
            }
            getProxy().doFilter(request, response, chain);
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            ctx =
                    (SecurityContext)
                            request.getSession(true)
                                    .getAttribute(
                                            HttpSessionSecurityContextRepository
                                                    .SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(ctx);
            Authentication auth = ctx.getAuthentication();
            assertNotNull(auth);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            checkForAuthenticatedRole(auth);
            assertEquals(testUserName, auth.getPrincipal());
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        }

        // unknown user
        for (PreAuthenticatedUserNameRoleSource rs : PreAuthenticatedUserNameRoleSource.values()) {
            config.setRoleSource(rs);
            getSecurityManager().saveFilter(config);

            config.setRoleSource(rs);
            request = createRequest("/foo/bar");
            request.setMethod("GET");
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();
            request.addHeader("principal", "unknwon");
            getProxy().doFilter(request, response, chain);
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            ctx =
                    (SecurityContext)
                            request.getSession(true)
                                    .getAttribute(
                                            HttpSessionSecurityContextRepository
                                                    .SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(ctx);
            Authentication auth = ctx.getAuthentication();
            assertNotNull(auth);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            checkForAuthenticatedRole(auth);
            assertEquals("unknwon", auth.getPrincipal());
        }

        // test disabled user
        updateUser("ug1", testUserName, false);
        config.setRoleSource(PreAuthenticatedUserNameRoleSource.UserGroupService);
        getSecurityManager().saveFilter(config);
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        request.addHeader("principal", testUserName);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        updateUser("ug1", testUserName, true);

        // Test anonymous
        insertAnonymousFilter();
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    @Test
    public void testDigestAuth() throws Exception {

        DigestAuthenticationFilterConfig config = new DigestAuthenticationFilterConfig();
        config.setClassName(GeoServerDigestAuthenticationFilter.class.getName());
        config.setName(testFilterName2);
        config.setUserGroupServiceName("ug1");

        getSecurityManager().saveFilter(config);
        prepareFilterChain(pattern, testFilterName2);
        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        String tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Digest") != -1);
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // test successful login
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        String headerValue =
                clientDigestString(tmp, testUserName, testPassword, request.getMethod());
        request.addHeader("Authorization", headerValue);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        // check wrong password
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        headerValue = clientDigestString(tmp, testUserName, "wrongpass", request.getMethod());
        request.addHeader("Authorization", headerValue);
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Digest") != -1);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check unknown user
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        headerValue = clientDigestString(tmp, "unknown", testPassword, request.getMethod());
        request.addHeader("Authorization", headerValue);
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Digest") != -1);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check root user
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        // We need to enable Master Root login first
        MasterPasswordProviderConfig masterPasswordConfig =
                getSecurityManager()
                        .loadMasterPassswordProviderConfig(
                                getSecurityManager().getMasterPasswordConfig().getProviderName());
        masterPasswordConfig.setLoginEnabled(true);
        getSecurityManager().saveMasterPasswordProviderConfig(masterPasswordConfig);

        headerValue =
                clientDigestString(
                        tmp, GeoServerUser.ROOT_USERNAME, getMasterPassword(), request.getMethod());
        request.addHeader("Authorization", headerValue);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // checkForAuthenticatedRole(auth);
        assertEquals(
                GeoServerUser.ROOT_USERNAME, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().size() == 1);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        // check root user with wrong password
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        headerValue =
                clientDigestString(
                        tmp, GeoServerUser.ROOT_USERNAME, "geoserver1", request.getMethod());
        request.addHeader("Authorization", headerValue);
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Digest") != -1);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check disabled user
        updateUser("ug1", testUserName, false);
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        headerValue = clientDigestString(tmp, "unknown", testPassword, request.getMethod());
        request.addHeader("Authorization", headerValue);
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Digest") != -1);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        updateUser("ug1", testUserName, true);

        // Test anonymous
        insertAnonymousFilter();
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    @Test
    public void testBasicAuthWithRememberMe() throws Exception {

        BasicAuthenticationFilterConfig config = new BasicAuthenticationFilterConfig();
        config.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        config.setUseRememberMe(true);
        config.setName(testFilterName5);

        getSecurityManager().saveFilter(config);
        prepareFilterChain(
                pattern, testFilterName5, GeoServerSecurityFilterChain.REMEMBER_ME_FILTER);

        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        request.addParameter("_spring_security_remember_me", "yes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        assertEquals(0, response.getCookies().length);
        String tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);

        // check success
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        request.addParameter("_spring_security_remember_me", "yes");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic " + new String(Base64.encodeBytes(("abc@xyz.com:abc").getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(1, response.getCookies().length);
        Cookie cookie = (Cookie) response.getCookies()[0];

        request = createRequest("/foo/bar");
        request.setMethod("GET");
        request.addParameter("_spring_security_remember_me", "yes");
        request.setCookies(cookie);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals("abc@xyz.com", ((UserDetails) auth.getPrincipal()).getUsername());
        //        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        //        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        // send cookie + auth header
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        request.addParameter("_spring_security_remember_me", "yes");
        request.setCookies(cookie);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addHeader(
                "Authorization",
                "Basic " + new String(Base64.encodeBytes(("abc@xyz.com:abc").getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals("abc@xyz.com", ((UserDetails) auth.getPrincipal()).getUsername());

        // check no remember me for root user
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        request.addParameter("_spring_security_remember_me", "yes");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        // We need to enable Master Root login first
        MasterPasswordProviderConfig masterPasswordConfig =
                getSecurityManager()
                        .loadMasterPassswordProviderConfig(
                                getSecurityManager().getMasterPasswordConfig().getProviderName());
        masterPasswordConfig.setLoginEnabled(true);
        getSecurityManager().saveMasterPasswordProviderConfig(masterPasswordConfig);

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(
                                Base64.encodeBytes(
                                        (GeoServerUser.ROOT_USERNAME + ":" + getMasterPassword())
                                                .getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // checkForAuthenticatedRole(auth);
        // no cookie for root user
        assertEquals(0, response.getCookies().length);

        // check disabled user
        updateUser("ug1", "abc@xyz.com", false);

        request = createRequest("/foo/bar");
        request.setMethod("GET");
        request.addParameter("_spring_security_remember_me", "yes");
        request.setCookies(cookie);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        // check for cancel cookie
        assertEquals(1, response.getCookies().length);
        Cookie cancelCookie = (Cookie) response.getCookies()[0];
        assertNull(cancelCookie.getValue());
        updateUser("ug1", "abc@xyz.com", true);
    }

    @Test
    public void testFormLogin() throws Exception {

        UsernamePasswordAuthenticationFilterConfig config =
                new UsernamePasswordAuthenticationFilterConfig();
        config.setClassName(GeoServerUserNamePasswordAuthenticationFilter.class.getName());
        config.setUsernameParameterName("username");
        config.setPasswordParameterName("password");
        config.setName(testFilterName6);
        getSecurityManager().saveFilter(config);

        //        LogoutFilterConfig loConfig = new LogoutFilterConfig();
        //        loConfig.setClassName(GeoServerLogoutFilter.class.getName());
        //        loConfig.setName(testFilterName9);
        //        getSecurityManager().saveFilter(loConfig);

        prepareFilterChain(pattern, GeoServerSecurityFilterChain.FORM_LOGIN_FILTER);

        modifyChain(pattern, false, true, null);

        prepareFilterChain(
                ConstantFilterChain.class, "/j_spring_security_check_foo/", testFilterName6);
        modifyChain("/j_spring_security_check_foo/", false, true, null);

        //        prepareFilterChain(LogoutFilterChain.class,"/j_spring_security_logout_foo",
        //                GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER,
        //                testFilterName9);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        String tmp = response.getHeader("Location");
        assertTrue(tmp.endsWith(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_FORM));
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check success
        request = createRequest("/j_spring_security_check_foo");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setMethod("POST");
        request.addParameter(config.getUsernameParameterName(), testUserName);
        request.addParameter(config.getPasswordParameterName(), testPassword);
        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        assertTrue(
                response.getHeader("Location")
                        .endsWith(
                                GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_SUCCCESS));
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        // Test logout

        GeoServerLogoutFilter logoutFilter =
                (GeoServerLogoutFilter)
                        getSecurityManager()
                                .loadFilter(GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        request = createRequest("/j_spring_security_logout_foo");
        request.setMethod("GET");
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
        SecurityContextHolder.getContext().setAuthentication(auth);

        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        // getProxy().doFilter(request, response, chain);
        logoutFilter.doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        tmp = response.getHeader("Location");
        assertNotNull(tmp);
        assertTrue(tmp.endsWith(GeoServerLogoutFilter.URL_AFTER_LOGOUT));
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // test invalid password
        request = createRequest("/j_spring_security_check_foo");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setMethod("POST");
        request.addParameter(config.getUsernameParameterName(), testUserName);
        request.addParameter(config.getPasswordParameterName(), "wrongpass");
        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        assertTrue(
                response.getHeader("Location")
                        .endsWith(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_FAILURE));

        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check unknown user
        request = createRequest("/j_spring_security_check_foo");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setMethod("POST");
        request.addParameter(config.getUsernameParameterName(), "unknwon");
        request.addParameter(config.getPasswordParameterName(), testPassword);
        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        assertTrue(
                response.getHeader("Location")
                        .endsWith(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_FAILURE));

        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check root user
        request = createRequest("/j_spring_security_check_foo");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setMethod("POST");
        request.addParameter(config.getUsernameParameterName(), GeoServerUser.ROOT_USERNAME);
        request.addParameter(config.getPasswordParameterName(), getMasterPassword());
        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        assertTrue(
                response.getHeader("Location")
                        .endsWith(
                                GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_SUCCCESS));
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // checkForAuthenticatedRole(auth);
        assertEquals(GeoServerUser.ROOT_USERNAME, auth.getPrincipal());
        assertTrue(auth.getAuthorities().size() == 1);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        // check root user with wrong password
        request = createRequest("/j_spring_security_check_foo");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setMethod("POST");
        request.addParameter(config.getUsernameParameterName(), GeoServerUser.ROOT_USERNAME);
        request.addParameter(config.getPasswordParameterName(), "geoserver1");
        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        assertTrue(
                response.getHeader("Location")
                        .endsWith(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_FAILURE));
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check disabled user
        updateUser("ug1", testUserName, false);
        request = createRequest("/j_spring_security_check_foo");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setMethod("POST");
        request.addParameter(config.getUsernameParameterName(), testUserName);
        request.addParameter(config.getPasswordParameterName(), testPassword);
        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        assertTrue(
                response.getHeader("Location")
                        .endsWith(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_FAILURE));
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        updateUser("ug1", testUserName, true);

        // Test anonymous
        insertAnonymousFilter();
        request = createRequest("foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    @Test
    public void testFormLoginWithRememberMe() throws Exception {

        UsernamePasswordAuthenticationFilterConfig config =
                new UsernamePasswordAuthenticationFilterConfig();
        config.setClassName(GeoServerUserNamePasswordAuthenticationFilter.class.getName());
        config.setUsernameParameterName("username");
        config.setPasswordParameterName("password");
        config.setName(testFilterName7);
        getSecurityManager().saveFilter(config);

        //        LogoutFilterConfig loConfig = new LogoutFilterConfig();
        //        loConfig.setClassName(GeoServerLogoutFilter.class.getName());
        //        loConfig.setName(testFilterName9);
        //        getSecurityManager().saveFilter(loConfig);

        prepareFilterChain(
                pattern,
                GeoServerSecurityFilterChain.REMEMBER_ME_FILTER,
                GeoServerSecurityFilterChain.FORM_LOGIN_FILTER);
        modifyChain(pattern, false, true, null);

        prepareFilterChain("/j_spring_security_check_foo/", testFilterName7);
        modifyChain("/j_spring_security_check_foo/", false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        request.addParameter("_spring_security_remember_me", "yes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        String tmp = response.getHeader("Location");
        assertTrue(tmp.endsWith(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_FORM));
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check success
        request = createRequest("/j_spring_security_check_foo");
        request.addParameter("_spring_security_remember_me", "yes");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setMethod("POST");
        request.addParameter(config.getUsernameParameterName(), testUserName);
        request.addParameter(config.getPasswordParameterName(), testPassword);
        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        assertTrue(
                response.getHeader("Location")
                        .endsWith(
                                GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_SUCCCESS));
        HttpSession session = request.getSession(true);
        ctx =
                (SecurityContext)
                        session.getAttribute(
                                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        assertEquals(1, response.getCookies().length);
        Cookie cookie = (Cookie) response.getCookies()[0];
        assertNotNull(cookie.getValue());

        // check logout
        GeoServerLogoutFilter logoutFilter =
                (GeoServerLogoutFilter)
                        getSecurityManager()
                                .loadFilter(GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        request = createRequest("/j_spring_security_logout_foo");
        request.setMethod("GET");
        session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
        SecurityContextHolder.getContext().setAuthentication(auth);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        // getProxy().doFilter(request, response, chain);
        logoutFilter.doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        tmp = response.getHeader("Location");
        assertNotNull(tmp);
        assertTrue(tmp.endsWith(GeoServerLogoutFilter.URL_AFTER_LOGOUT));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        Cookie cancelCookie = (Cookie) response.getCookies()[0];
        assertNull(cancelCookie.getValue());

        // check no remember me for root user
        request = createRequest("/j_spring_security_check_foo");
        request.addParameter("_spring_security_remember_me", "yes");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        // We need to enable Master Root login first
        MasterPasswordProviderConfig masterPasswordConfig =
                getSecurityManager()
                        .loadMasterPassswordProviderConfig(
                                getSecurityManager().getMasterPasswordConfig().getProviderName());
        masterPasswordConfig.setLoginEnabled(true);
        getSecurityManager().saveMasterPasswordProviderConfig(masterPasswordConfig);

        request.setMethod("POST");
        request.addParameter(config.getUsernameParameterName(), GeoServerUser.ROOT_USERNAME);
        request.addParameter(config.getPasswordParameterName(), getMasterPassword());
        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        assertTrue(
                response.getHeader("Location")
                        .endsWith(
                                GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_SUCCCESS));
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        // checkForAuthenticatedRole(auth);
        assertEquals(GeoServerUser.ROOT_USERNAME, auth.getPrincipal());
        assertEquals(0, response.getCookies().length);

        // check disabled user
        updateUser("ug1", testUserName, false);

        request = createRequest("/foo/bar");
        request.setMethod("GET");
        request.setCookies(cookie);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        tmp = response.getHeader("Location");
        assertTrue(tmp.endsWith(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_FORM));
        // check for cancel cookie
        assertEquals(1, response.getCookies().length);
        cancelCookie = (Cookie) response.getCookies()[0];
        assertNull(cancelCookie.getValue());
        updateUser("ug1", testUserName, true);
    }

    @Test
    public void testX509Auth() throws Exception {

        X509CertificateAuthenticationFilterConfig config =
                new X509CertificateAuthenticationFilterConfig();
        config.setClassName(GeoServerX509CertificateAuthenticationFilter.class.getName());
        config.setName(testFilterName8);
        config.setRoleServiceName("rs1");
        config.setRoleSource(J2EERoleSource.RoleService);
        config.setUserGroupServiceName("ug1");
        config.setRolesHeaderAttribute("roles");
        getSecurityManager().saveFilter(config);

        prepareFilterChain(pattern, testFilterName8);

        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        for (J2EERoleSource rs : J2EERoleSource.values()) {
            config.setRoleSource(rs);
            getSecurityManager().saveFilter(config);
            request = createRequest("/foo/bar");
            request.setMethod("GET");
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();
            if (rs == J2EERoleSource.Header) {
                request.addHeader("roles", derivedRole + ";" + rootRole);
            }
            if (rs == J2EERoleSource.J2EE) {
                if (true) {
                    request.addUserRole(derivedRole);
                }
                if (false) {
                    request.addUserRole(rootRole);
                }
            }

            setCertifacteForUser(testUserName, request);
            getProxy().doFilter(request, response, chain);
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            ctx =
                    (SecurityContext)
                            request.getSession(true)
                                    .getAttribute(
                                            HttpSessionSecurityContextRepository
                                                    .SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(ctx);
            Authentication auth = ctx.getAuthentication();
            assertNotNull(auth);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            checkForAuthenticatedRole(auth);
            assertEquals(testUserName, auth.getPrincipal());
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        }

        // unknown user
        for (J2EERoleSource rs : J2EERoleSource.values()) {
            config.setRoleSource(rs);
            getSecurityManager().saveFilter(config);

            config.setRoleSource(rs);
            request = createRequest("/foo/bar");
            request.setMethod("GET");
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();
            if (rs == J2EERoleSource.J2EE) {
                if (false) {
                    request.addUserRole(derivedRole);
                }
                if (false) {
                    request.addUserRole(rootRole);
                }
            }
            // TODO
            setCertifacteForUser("unknown", request);
            getProxy().doFilter(request, response, chain);
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            ctx =
                    (SecurityContext)
                            request.getSession(true)
                                    .getAttribute(
                                            HttpSessionSecurityContextRepository
                                                    .SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(ctx);
            Authentication auth = ctx.getAuthentication();
            assertNotNull(auth);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            checkForAuthenticatedRole(auth);
            assertEquals("unknown", auth.getPrincipal());
        }

        // test disabled user
        updateUser("ug1", testUserName, false);
        config.setRoleSource(J2EERoleSource.UserGroupService);
        getSecurityManager().saveFilter(config);
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        setCertifacteForUser(testUserName, request);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        updateUser("ug1", testUserName, true);

        // Test anonymous
        insertAnonymousFilter();
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    @Test
    @RunTestSetup
    public void testCascadingFilters() throws Exception {

        //        BasicAuthenticationFilterConfig bconfig = new BasicAuthenticationFilterConfig();
        //        bconfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        //        bconfig.setUseRememberMe(false);
        //        bconfig.setName(testFilterName);
        //        getSecurityManager().saveFilter(bconfig);

        DigestAuthenticationFilterConfig config = new DigestAuthenticationFilterConfig();
        config.setClassName(GeoServerDigestAuthenticationFilter.class.getName());
        config.setName(testFilterName2);
        config.setUserGroupServiceName("ug1");

        getSecurityManager().saveFilter(config);
        prepareFilterChain(pattern, testFilterName, testFilterName2);

        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point, must be digest
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        String tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert (tmp.indexOf(GeoServerSecurityManager.REALM) != -1);
        assert (tmp.indexOf("Digest") != -1);
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // test successful login for digest
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        String headerValue =
                clientDigestString(tmp, testUserName, testPassword, request.getMethod());
        request.addHeader("Authorization", headerValue);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        // check success for basic authentication
        request = createRequest("/foo/bar");
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(
                                Base64.encodeBytes(
                                        (testUserName + ":" + testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
    }

    @Test
    @Ignore // disabled, builds locally but not onmaster
    public void testSSL() throws Exception {

        prepareFilterChain(pattern, GeoServerSecurityFilterChain.ANONYMOUS_FILTER);
        modifyChain(pattern, false, true, null);

        SecurityManagerConfig secConfig = getSecurityManager().getSecurityConfig();
        RequestFilterChain chain = secConfig.getFilterChain().getRequestChainByName("testChain");
        chain.setRequireSSL(true);
        getSecurityManager().saveSecurityConfig(secConfig);

        MockHttpServletRequest request = createRequest("/foo/bar?request=getCapabilities&a=b");
        request.setMethod("GET");
        request.setProtocol("https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain authchain = new MockFilterChain();
        getProxy().doFilter(request, response, authchain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        request = createRequest("/foo/bar?request=getCapabilities&a=b");
        request.setMethod("GET");
        response = new MockHttpServletResponse();

        authchain = new MockFilterChain();
        getProxy().doFilter(request, response, authchain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        String urlString = response.getHeader("Location");
        assertNotNull(urlString);
        assertTrue(urlString.startsWith("https"));
        assertTrue(urlString.indexOf("a=b") != -1);
        assertTrue(urlString.indexOf("443") != -1);

        chain.setRequireSSL(false);
        getSecurityManager().saveSecurityConfig(secConfig);
    }
}

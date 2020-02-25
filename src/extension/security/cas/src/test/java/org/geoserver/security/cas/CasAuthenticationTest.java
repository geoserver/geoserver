/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AbstractSecurityServiceTest;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.LogoutFilterChain;
import org.geoserver.security.ServiceLoginFilterChain;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.auth.TestingAuthenticationCache;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * A running cas server is needed
 *
 * <p>To activate the test a file ".geoserver/cas.properties" in the home directory is needed.
 *
 * <p>Content # Fixture for cas # casserverurlprefix=https://ux-server02.mc-home.local:8443/cas
 * service=https://ux-desktop03.mc-home.local:4711/geoserver/j_spring_cas_security_check
 * proxycallbackurlprefix=https://ux-desktop03.mc-home.local:4711/geoserver/
 *
 * <p>Client ssl configuration: Create a keystore keystore.jks in home_dir/.geoserver with key store
 * key password "changeit"
 *
 * <p>Create self signing certificate keytool -genkey -alias mc-home.local -keystore rsa-keystore
 * -keyalg RSA -sigalg MD5withRSA -validity 365000
 *
 * <p>Only the cn must be set to the full server name "ux-desktop03.mc-home.local"
 *
 * <p>Export the certificate keytool -export -alias mc-home.local -keystore keystore.jks -file
 * ux-desktop03.crt
 *
 * <p>For the cas server copy ux-desktop03.crt to the server
 *
 * <p>Find cacerts file for the virtual machine running cas
 *
 * <p>Import the certificate
 *
 * <p>keytool -import -trustcacerts -alias mc-home.local -file ux-desktop03.crt \ -keystore
 * /usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/security/cacerts
 *
 * <p>The keystore password for cacerts is "changeit"
 *
 * <p>Next, export the certificate of tomcat and import it into the cacerts of your java sdk
 *
 * @author christian
 */
public class CasAuthenticationTest extends AbstractAuthenticationProviderTest {

    static URL casServerURLPrefix;

    static URL serviceUrl;

    static URL loginUrl;

    static URL proxyCallbackUrlPrefix;

    static HttpsServer httpsServer;

    public class HttpsProxyCallBackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            URI uri = ex.getRequestURI();
            ex.getRequestBody().close();
            LOGGER.info("Cas proxy callback: " + uri.toString());
            String query = uri.getQuery();

            MockHttpServletRequest request =
                    createRequest(GeoServerCasConstants.CAS_PROXY_RECEPTOR_PATTERN);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            // CAS sends the callback twice, the first time without parameters
            if (query != null) {
                request.setQueryString(query);
                String[] kvps = query.split("&");
                for (String kvp : kvps) {
                    String[] tmp = kvp.split("=");
                    request.addParameter(tmp[0], tmp[1]);
                }
            }

            try {
                getProxy().doFilter(request, response, chain);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());

            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().close();
        }
    }

    public class SingleSignOutHandler implements HttpHandler {

        private String service;

        public String getService() {
            return service;
        }

        public SingleSignOutHandler(String servicePath) {
            service = servicePath;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {

            BufferedReader in = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
            String line = "";
            StringBuffer buff = new StringBuffer();
            while ((line = in.readLine()) != null) {
                buff.append(line);
            }
            in.close();

            MockHttpServletRequest request = createRequest(service);
            request.setMethod("POST");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            String paramValue = URLDecoder.decode(buff.toString(), "utf-8");
            request.addParameter(
                    "logoutRequest", paramValue.substring(paramValue.indexOf("=") + 1));
            try {
                GeoServerSecurityFilterChainProxy proxy = getProxy();
                //                System.out.println("SERVCIE: " + service);
                //                System.out.println("URL: " + request.getRequestURL().toString());
                //                for (SecurityFilterChain c : proxy.getFilterChains()) {
                //                    System.out.println(c.toString());
                //                }
                proxy.doFilter(request, response, chain);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());

            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().close();
        }
    }

    @Before
    public void checkOnline() {
        Assume.assumeTrue(getTestData().isTestDataAvailable());
    }

    @Override
    protected SystemTestData createTestData() throws Exception {
        return new LiveCasData(AbstractSecurityServiceTest.unpackTestDataDir());
    }

    @Override
    protected void onSetUp(org.geoserver.data.test.SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        LiveCasData td = (LiveCasData) getTestData();
        casServerURLPrefix = td.getServerURLPrefix();
        loginUrl = td.getLoginURL();
        serviceUrl = td.getServiceURL();
        proxyCallbackUrlPrefix = td.getProxyCallbackURLPrefix();
        if (httpsServer == null) {
            httpsServer = createAndStartHttpsServer();
            td.checkSSLServer();
        }
    }

    protected HttpsServer createAndStartHttpsServer() throws Exception {
        HttpsServer httpsServer = ((LiveCasData) getTestData()).createSSLServer();
        URL callbackUrl =
                new URL(
                        GeoServerCasConstants.createProxyCallBackURl(
                                proxyCallbackUrlPrefix.toString()));
        httpsServer.createContext(callbackUrl.getPath(), new HttpsProxyCallBackHandler());

        httpsServer.createContext(
                createRequest("/j_spring_cas_security_check").getRequestURI(),
                new SingleSignOutHandler("/j_spring_cas_security_check"));
        httpsServer.createContext(
                createRequest("/wms").getRequestURI(), new SingleSignOutHandler("/wms"));
        httpsServer.start();
        return httpsServer;
    }

    protected String getResponseHeaderValue(HttpURLConnection conn, String name) {
        for (int i = 0; ; i++) {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);

            if (headerName == null && headerValue == null) {
                // No more headers
                break;
            }
            if (name.equalsIgnoreCase(headerName)) {
                return headerValue;
            }
        }
        return null;
    }

    @Test
    public void testCASLogin() throws Exception {

        String casFilterName = "testCasFilter1";
        CasAuthenticationFilterConfig config = new CasAuthenticationFilterConfig();
        config.setClassName(GeoServerCasAuthenticationFilter.class.getName());

        config.setCasServerUrlPrefix(casServerURLPrefix.toString());
        config.setName(casFilterName);
        config.setRoleSource(PreAuthenticatedUserNameRoleSource.UserGroupService);
        config.setUserGroupServiceName("ug1");
        config.setSingleSignOut(true);

        getSecurityManager().saveFilter(config);

        prepareFilterChain(pattern, casFilterName);
        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);

        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        String redirectURL = response.getHeader("Location");
        assertTrue(redirectURL.contains(GeoServerCasConstants.LOGIN_URI));
        assertTrue(redirectURL.endsWith("bar"));

        // test success
        String username = "castest";
        String password = username;
        CasFormAuthenticationHelper helper =
                new CasFormAuthenticationHelper(casServerURLPrefix, username, password);
        helper.ssoLogin();

        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        String ticket = loginUsingTicket(helper, request, response, chain);
        assertFalse(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);

        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(false)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(username, auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        assertNotNull(
                GeoServerCasAuthenticationFilter.getHandler()
                        .getSessionMappingStorage()
                        .removeSessionByMappingId(ticket));
        helper.ssoLogout();

        // check unknown user
        username = "unknown";
        password = username;
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, password);
        helper.ssoLogin();
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = loginUsingTicket(helper, request, response, chain);
        assertFalse(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);

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
        checkForAuthenticatedRole(ctx.getAuthentication());
        assertEquals(username, auth.getPrincipal());
        assertEquals(1, auth.getAuthorities().size());
        assertNotNull(
                GeoServerCasAuthenticationFilter.getHandler()
                        .getSessionMappingStorage()
                        .removeSessionByMappingId(ticket));
        helper.ssoLogout();

        // test root user
        username = GeoServerUser.ROOT_USERNAME;
        password = username;
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, password);
        helper.ssoLogin();

        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = loginUsingTicket(helper, request, response, chain);
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertFalse(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // checkForAuthenticatedRole(auth);
        assertEquals(GeoServerUser.ROOT_USERNAME, auth.getPrincipal());
        assertTrue(auth.getAuthorities().size() == 1);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));
        assertNotNull(
                GeoServerCasAuthenticationFilter.getHandler()
                        .getSessionMappingStorage()
                        .removeSessionByMappingId(ticket));
        helper.ssoLogout();

        // check disabled user
        username = "castest";
        password = username;
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, password);
        helper.ssoLogin();
        updateUser("ug1", username, false);
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = loginUsingTicket(helper, request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        redirectURL = response.getHeader("Location");
        assertTrue(redirectURL.contains("login"));
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(
                GeoServerCasAuthenticationFilter.getHandler()
                        .getSessionMappingStorage()
                        .removeSessionByMappingId(ticket));
        updateUser("ug1", username, true);
        helper.ssoLogout();

        insertAnonymousFilter();
        request = createRequest("foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();

        // test invalid ticket

        username = "castest";
        password = username;
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, password);
        helper.ssoLogin();

        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = helper.getServiceTicket(new URL(request.getRequestURL().toString()));
        ticket += "ST-A";
        request.addParameter("ticket", ticket);
        request.setQueryString("ticket=" + ticket);

        getProxy().doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        redirectURL = response.getHeader("Location");
        assertTrue(redirectURL.contains(GeoServerCasConstants.LOGIN_URI));
        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(
                GeoServerCasAuthenticationFilter.getHandler()
                        .getSessionMappingStorage()
                        .removeSessionByMappingId(ticket));
        helper.ssoLogout();

        // test success with proxy granting ticket
        config.setProxyCallbackUrlPrefix(proxyCallbackUrlPrefix.toString());
        getSecurityManager().saveFilter(config);

        username = "castest";
        password = username;
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, password);
        helper.ssoLogin();

        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = helper.getServiceTicket(new URL(request.getRequestURL().toString()));
        request.addParameter("ticket", ticket);
        request.setQueryString("ticket=" + ticket);
        getProxy().doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        //        assertTrue(response.wasRedirectSent());
        //        redirectUrl = response.getHeader("Location");
        //        assertNotNull(redirectUrl);

        ctx =
                (SecurityContext)
                        request.getSession(true)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        PreAuthenticatedAuthenticationToken casAuth =
                (PreAuthenticatedAuthenticationToken) ctx.getAuthentication();
        assertNotNull(casAuth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(casAuth);
        assertEquals(username, casAuth.getPrincipal());
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        Assertion ass =
                (Assertion)
                        request.getSession(true)
                                .getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY);
        assertNotNull(ass);
        String proxyTicket = ass.getPrincipal().getProxyTicketFor("http://localhost/blabla");
        assertNotNull(proxyTicket);
        assertNotNull(
                GeoServerCasAuthenticationFilter.getHandler()
                        .getSessionMappingStorage()
                        .removeSessionByMappingId(ticket));
        helper.ssoLogout();
    }

    @Test
    public void testLogout() throws Exception {

        LogoutFilterChain logoutchain =
                (LogoutFilterChain)
                        getSecurityManager()
                                .getSecurityConfig()
                                .getFilterChain()
                                .getRequestChainByName("webLogout");

        String casFilterName = "testCasFilter2";
        CasAuthenticationFilterConfig config = new CasAuthenticationFilterConfig();
        config.setClassName(GeoServerCasAuthenticationFilter.class.getName());
        config.setCasServerUrlPrefix(casServerURLPrefix.toString());
        config.setName(casFilterName);
        config.setRoleSource(PreAuthenticatedUserNameRoleSource.UserGroupService);
        config.setUserGroupServiceName("ug1");
        config.setSingleSignOut(true);
        getSecurityManager().saveFilter(config);

        // put a CAS filter on an active chain
        prepareFilterChain(pattern, casFilterName);
        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);
        getCache().removeAll();

        // login
        String username = "castest";
        String password = username;
        CasFormAuthenticationHelper helper =
                new CasFormAuthenticationHelper(casServerURLPrefix, username, password);
        helper.ssoLogin();

        MockHttpServletRequest request = createRequest(pattern);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        loginUsingTicket(helper, request, response, chain);
        assertFalse(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);

        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(false)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        MockHttpSession session = (MockHttpSession) request.getSession(false);
        assertNotNull(session);
        assertFalse(session.isInvalid());

        // logout triggered by geoserver
        request = createRequest(logoutchain.getPatterns().get(0));
        // request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
        SecurityContextHolder.setContext(ctx);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        // getProxy().doFilter(request, response, chain);
        GeoServerLogoutFilter logoutFilter =
                (GeoServerLogoutFilter)
                        getSecurityManager()
                                .loadFilter(GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        logoutFilter.doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        String redirectUrl = response.getHeader("Location");
        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains(GeoServerCasConstants.LOGOUT_URI));
        session = (MockHttpSession) request.getSession(false);

        // login
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, password);
        helper.ssoLogin();

        request = createRequest(pattern);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        String ticket = loginUsingTicket(helper, request, response, chain);
        assertFalse(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);

        ctx =
                (SecurityContext)
                        request.getSession(false)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        session = (MockHttpSession) request.getSession(false);
        assertNotNull(session);
        assertFalse(session.isInvalid());

        // logout triggered by cas server
        request = createRequest(pattern);
        // request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
        SecurityContextHolder.setContext(ctx);
        request.setMethod("POST");
        request.setSession(session);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addParameter("logoutRequest", getBodyForLogoutRequest(ticket));
        GeoServerCasAuthenticationFilter casFilter =
                (GeoServerCasAuthenticationFilter) getSecurityManager().loadFilter(casFilterName);
        // getProxy().doFilter(request, response, chain);
        casFilter.doFilter(request, response, chain);
        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        redirectUrl = response.getHeader("Location");
        assertNotNull(redirectUrl);
        assertFalse(redirectUrl.contains(GeoServerCasConstants.LOGOUT_URI));
    }

    protected Assertion authenticateWithPGT(CasFormAuthenticationHelper helper) throws Exception {
        helper.ssoLogin();

        String ticket = helper.getServiceTicket(serviceUrl);

        Cas20ProxyTicketValidator validator =
                new Cas20ProxyTicketValidator(casServerURLPrefix.toString());
        validator.setAcceptAnyProxy(true);
        validator.setProxyCallbackUrl(
                GeoServerCasConstants.createProxyCallBackURl(
                        proxyCallbackUrlPrefix.toExternalForm()));
        validator.setProxyGrantingTicketStorage(
                GeoServerExtensions.bean(ProxyGrantingTicketStorage.class));

        Assertion result = validator.validate(ticket, serviceUrl.toExternalForm());

        assertNotNull(result);
        return result;
    }

    @Test
    public void testAuthWithServiceTicket() throws Exception {

        pattern = "/wms/**";
        String casProxyFilterName = "testCasProxyFilter1";
        CasAuthenticationFilterConfig pconfig1 = new CasAuthenticationFilterConfig();
        pconfig1.setClassName(GeoServerCasAuthenticationFilter.class.getName());
        pconfig1.setName(casProxyFilterName);
        pconfig1.setCasServerUrlPrefix(casServerURLPrefix.toString());
        pconfig1.setRoleSource(PreAuthenticatedUserNameRoleSource.UserGroupService);
        pconfig1.setUserGroupServiceName("ug1");
        pconfig1.setSingleSignOut(true);
        getSecurityManager().saveFilter(pconfig1);

        prepareFilterChain(ServiceLoginFilterChain.class, pattern, casProxyFilterName);

        SecurityContextHolder.getContext().setAuthentication(null);

        // test entry point
        MockHttpServletRequest request = createRequest("wms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        request.addParameter("ticket", "ST-blabla");
        request.setQueryString("ticket=ST-blabla");
        request.addHeader(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

        // test successful
        getCache().removeAll();
        String username = "castest";
        CasFormAuthenticationHelper helper =
                new CasFormAuthenticationHelper(casServerURLPrefix, username, username);
        helper.ssoLogin();

        request = createRequest("wms");
        request.setQueryString("request=getCapabilities");
        request.addHeader(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
        String ticket =
                helper.getServiceTicket(
                        new URL(
                                request.getRequestURL().toString()
                                        + "?"
                                        + request.getQueryString()));
        assertNotNull(ticket);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addParameter("ticket", ticket);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        TestingAuthenticationCache cache = getCache();
        Authentication casAuth = cache.get(casProxyFilterName, username);
        assertNotNull(casAuth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        checkForAuthenticatedRole(casAuth);
        assertEquals(username, casAuth.getPrincipal());
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        assertNotNull(request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY));

        assertNull(
                GeoServerCasAuthenticationFilter.getHandler()
                        .getSessionMappingStorage()
                        .removeSessionByMappingId(ticket));
        helper.ssoLogout();

        // check unknown user

        username = "unknown";
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, username);
        helper.ssoLogin();

        request = createRequest("wms");
        ticket = helper.getServiceTicket(new URL(request.getRequestURL().toString()));
        assertNotNull(ticket);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addParameter("ticket", ticket);
        request.setQueryString("ticket=" + ticket);
        request.addHeader(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        cache = getCache();
        casAuth = cache.get(casProxyFilterName, username);
        assertNotNull(casAuth);
        assertNotNull(casAuth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        checkForAuthenticatedRole(casAuth);
        assertEquals(username, casAuth.getPrincipal());
        assertEquals(1, casAuth.getAuthorities().size());
        assertNotNull(request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY));

        // check for disabled user
        getCache().removeAll();
        updateUser("ug1", "castest", false);

        username = "castest";
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, username);
        helper.ssoLogin();

        request = createRequest("wms");
        ticket = helper.getServiceTicket(new URL(request.getRequestURL().toString()));
        assertNotNull(ticket);

        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addParameter("ticket", ticket);
        request.setQueryString("ticket=" + ticket);
        request.addHeader(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        cache = getCache();
        casAuth = cache.get(casProxyFilterName, ticket);
        assertNull(casAuth);
        assertNull(request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY));
        assertNull(request.getSession(false));

        updateUser("ug1", "castest", true);
        helper.ssoLogout();

        // Test anonymous
        insertAnonymousFilter();
        request = createRequest("wms");
        request.addHeader(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();

        // test proxy granting ticket

        pconfig1.setProxyCallbackUrlPrefix(proxyCallbackUrlPrefix.toString());
        getSecurityManager().saveFilter(pconfig1);

        getCache().removeAll();
        username = "castest";
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, username);
        authenticateWithPGT(helper);
        request = createRequest("wms");
        ticket = helper.getServiceTicket(new URL(request.getRequestURL().toString()));
        request.addHeader(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
        assertNotNull(ticket);

        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addParameter("ticket", ticket);
        getProxy().doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        cache = getCache();
        casAuth = cache.get(casProxyFilterName, username);
        assertNotNull(casAuth);
        assertNotNull(casAuth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        checkForAuthenticatedRole(casAuth);
        assertEquals(username, casAuth.getPrincipal());
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        String proxyTicket =
                ((Assertion) request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY))
                        .getPrincipal()
                        .getProxyTicketFor("http://localhost/blabla");

        assertNotNull(proxyTicket);
        helper.ssoLogout();
    }

    @Test
    public void testAuthWithProxyTicket() throws Exception {

        pattern = "/wms/**";

        String casProxyFilterName = "testCasProxyFilter2";
        CasAuthenticationFilterConfig pconfig1 = new CasAuthenticationFilterConfig();
        pconfig1.setClassName(GeoServerCasAuthenticationFilter.class.getName());
        pconfig1.setName(casProxyFilterName);
        pconfig1.setCasServerUrlPrefix(casServerURLPrefix.toString());
        pconfig1.setRoleSource(PreAuthenticatedUserNameRoleSource.UserGroupService);
        pconfig1.setUserGroupServiceName("ug1");
        getSecurityManager().saveFilter(pconfig1);

        prepareFilterChain(ServiceLoginFilterChain.class, pattern, casProxyFilterName);

        // prepareFilterChain(GeoServerCasConstants.CAS_PROXY_RECEPTOR_PATTERN,
        // casFilterName);

        // prepareFilterChain("/j_spring_cas_security_check",
        // GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER,
        // casFilterName);

        SecurityContextHolder.getContext().setAuthentication(null);

        // test entry point with header attribute
        MockHttpServletRequest request = createRequest("wms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        request.addParameter("ticket", "ST-blabla");
        request.setQueryString("ticket=ST-blabla");
        request.addHeader(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

        // test entry point with url param
        request = createRequest("wms");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addParameter("ticket", "ST-blabla");
        request.addParameter(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
        request.setQueryString(
                "ticket=ST-blabla&" + GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT + "=false");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

        // test successful

        getCache().removeAll();
        String username = "castest";
        CasFormAuthenticationHelper helper =
                new CasFormAuthenticationHelper(casServerURLPrefix, username, username);
        Assertion ass = authenticateWithPGT(helper);
        String proxyTicket = null;
        for (int i = 0; i < 2; i++) {
            request = createRequest("wms");
            request.setQueryString("request=getCapabilities");
            proxyTicket =
                    ass.getPrincipal()
                            .getProxyTicketFor(
                                    request.getRequestURL().toString()
                                            + "?"
                                            + request.getQueryString());
            assertNotNull(proxyTicket);
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();
            request.addParameter("ticket", proxyTicket);
            if (i == 0) {
                request.addParameter(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
                request.setQueryString(
                        request.getQueryString()
                                + "&ticket="
                                + proxyTicket
                                + "&"
                                + GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT
                                + "=false");
            } else {
                request.addHeader(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
                request.setQueryString(request.getQueryString() + "&ticket=" + proxyTicket);
            }
            getProxy().doFilter(request, response, chain);
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            TestingAuthenticationCache cache = getCache();
            Authentication casAuth = cache.get(casProxyFilterName, username);
            assertNotNull(casAuth);
            checkForAuthenticatedRole(casAuth);
            assertEquals(username, casAuth.getPrincipal());
            assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(rootRole)));
            assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(derivedRole)));
            assertNotNull(request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY));
            assertNull(request.getSession(false));
        }
        assertNull(
                GeoServerCasAuthenticationFilter.getHandler()
                        .getSessionMappingStorage()
                        .removeSessionByMappingId(proxyTicket));
        helper.ssoLogout();

        // check unknown user

        username = "unknown";
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, username);
        ass = authenticateWithPGT(helper);
        for (int i = 0; i < 2; i++) {
            request = createRequest("wms");
            request.setQueryString("request=getCapabilities");
            proxyTicket =
                    ass.getPrincipal()
                            .getProxyTicketFor(
                                    request.getRequestURL().toString()
                                            + "?"
                                            + request.getQueryString());
            assertNotNull(proxyTicket);
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();
            request.addParameter("ticket", proxyTicket);
            if (i == 0) {
                request.addParameter(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
                request.setQueryString(
                        request.getQueryString()
                                + "&ticket="
                                + proxyTicket
                                + "&"
                                + GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT
                                + "=false");
            } else {
                request.addHeader(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
                request.setQueryString(request.getQueryString() + "&ticket=" + proxyTicket);
            }
            getProxy().doFilter(request, response, chain);
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            TestingAuthenticationCache cache = getCache();
            Authentication casAuth = cache.get(casProxyFilterName, username);
            assertNotNull(casAuth);
            checkForAuthenticatedRole(casAuth);
            assertEquals(username, casAuth.getPrincipal());
            assertEquals(1, casAuth.getAuthorities().size());
            assertNotNull(request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY));
            assertNull(request.getSession(false));
        }
        helper.ssoLogout();

        // check for disabled user
        getCache().removeAll();
        updateUser("ug1", "castest", false);

        username = "castest";
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, username);
        ass = authenticateWithPGT(helper);
        request = createRequest("wms");
        proxyTicket = ass.getPrincipal().getProxyTicketFor(request.getRequestURL().toString());
        assertNotNull(proxyTicket);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addParameter("ticket", proxyTicket);
        request.addParameter(GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT, "false");
        request.setQueryString(
                "ticket="
                        + proxyTicket
                        + "&"
                        + GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT
                        + "=false");

        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        TestingAuthenticationCache cache = getCache();
        Authentication casAuth = cache.get(casProxyFilterName, proxyTicket);
        assertNull(casAuth);
        assertNull(request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY));
        assertNull(request.getSession(false));

        updateUser("ug1", "castest", true);
        helper.ssoLogout();

        // Test anonymous
        insertAnonymousFilter();
        request = createRequest("wms");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();

        // test proxy granting ticket in proxied auth filter

        pconfig1.setProxyCallbackUrlPrefix(proxyCallbackUrlPrefix.toString());
        getSecurityManager().saveFilter(pconfig1);

        getCache().removeAll();
        username = "castest";
        helper = new CasFormAuthenticationHelper(casServerURLPrefix, username, username);
        ass = authenticateWithPGT(helper);
        request = createRequest("wms");
        proxyTicket = ass.getPrincipal().getProxyTicketFor(request.getRequestURL().toString());
        assertNotNull(proxyTicket);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addParameter("ticket", proxyTicket);
        getProxy().doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        cache = getCache();
        casAuth = cache.get(casProxyFilterName, username);
        assertNotNull(casAuth);
        checkForAuthenticatedRole(casAuth);
        assertEquals(username, casAuth.getPrincipal());
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        proxyTicket =
                ((Assertion) request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY))
                        .getPrincipal()
                        .getProxyTicketFor("http://localhost/blabla");

        assertNotNull(proxyTicket);
        helper.ssoLogout();
    }

    // protected MockHttpServletRequest createRequest(String url) {
    // MockHttpServletRequest request = super.createRequest(url);
    // request.setProtocol(serviceUrl.getProtocol());
    // request.setScheme(serviceUrl.getProtocol());
    // request.setServerName(serviceUrl.getHost());
    // request.setServerPort(serviceUrl.getPort());
    // return request;
    // }

    @Test
    public void testCasAuthenticationHelper() throws Exception {

        CasFormAuthenticationHelper helper =
                new CasFormAuthenticationHelper(casServerURLPrefix, "fail", "abc");
        assertFalse(helper.ssoLogin());

        helper = new CasFormAuthenticationHelper(casServerURLPrefix, "success", "success");
        assertTrue(helper.ssoLogin());
        assertNotNull(helper.getTicketGrantingCookie());
        LOGGER.info("TGC after login : " + helper.getTicketGrantingCookie());

        assertTrue(helper.ssoLogout());
        assertNotNull(helper.getTicketGrantingCookie());
        LOGGER.info("TGC after logout : " + helper.getTicketGrantingCookie());

        assertTrue(helper.ssoLogin());
        assertNotNull(helper.getTicketGrantingCookie());
        String ticket = helper.getServiceTicket(serviceUrl);
        assertNotNull(ticket);
        assertTrue(ticket.startsWith("ST-"));
        LOGGER.info("ST : " + ticket);
        helper.ssoLogout();
    }

    protected String getBodyForLogoutRequest(String ticket) {
        String template =
                "<LogoutRequest ID=\"[RANDOM ID]\" Version=\"2.0\" IssueInstant=\"[CURRENT DATE/TIME]\">"
                        + "<NameID>@NOT_USED@</NameID>"
                        + "<SessionIndex>[SESSION IDENTIFIER]</SessionIndex>"
                        + "</LogoutRequest>";

        return template.replace("[SESSION IDENTIFIER]", ticket);
    }

    protected String loginUsingTicket(
            CasFormAuthenticationHelper helper,
            MockHttpServletRequest request,
            MockHttpServletResponse response,
            MockFilterChain chain)
            throws Exception {
        String ticket = helper.getServiceTicket(new URL(request.getRequestURL().toString()));
        request.setQueryString("ticket=" + ticket);
        request.addParameter("ticket", ticket);
        getProxy().doFilter(request, response, chain);
        return ticket;
    }
}

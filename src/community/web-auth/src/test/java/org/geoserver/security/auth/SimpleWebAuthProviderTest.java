/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.security.Principal;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.auth.web.SimpleWebAuthenticationConfig;
import org.geoserver.security.auth.web.SimpleWebServiceAuthenticationProvider;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.data.Base64;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMethod;

@Ignore
public class SimpleWebAuthProviderTest extends AbstractAuthenticationProviderTest {
    public static final String testFilterName = "basicAuthTestFilter";

    private static final String WebAuthProviderName = "webAuthProvider";
    private static final String webAuthProviderUseHeader = "webAuthProviderUseHeader";
    private static final String webAuthProviderWithRoleService = "webAuthProviderWithRoleService";

    protected MockHttpClient webAuthHttpClientClient;

    protected static URL baseURL;
    protected static URL authURL;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // configure the basic filter username password filter
        BasicAuthenticationFilterConfig config = new BasicAuthenticationFilterConfig();
        config.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        config.setUseRememberMe(false);
        config.setName(testFilterName);

        getSecurityManager().saveFilter(config);

        String response = "\"user\":\"" + testUserName + "\",\"roles\":\"WEB_SERVICE_ROLE\"";

        webAuthHttpClientClient = new MockHttpClient();
        baseURL = new URL(TestHttpClientProvider.MOCKSERVER + "/webAuth");
        authURL = new URL(baseURL + "?user=" + testUserName + "&pass=" + testPassword);
        webAuthHttpClientClient.expectGet(
                authURL, new MockHttpResponse(response.getBytes(), "text/plain"));
        webAuthHttpClientClient.expectGet(
                baseURL, new MockHttpResponse(response.getBytes(), "text/plain"));
        TestHttpClientProvider.bind(
                webAuthHttpClientClient, baseURL + "?user={user}&pass={password}");
        TestHttpClientProvider.bind(webAuthHttpClientClient, authURL);
        // for header
        TestHttpClientProvider.bind(webAuthHttpClientClient, baseURL);
    }

    @Test
    public void testWebAuthWithRoleInResponse() throws Exception {

        SimpleWebAuthenticationConfig config = new SimpleWebAuthenticationConfig();
        config.setClassName(SimpleWebServiceAuthenticationProvider.class.getName());
        config.setName(WebAuthProviderName);
        config.setConnectionURL(baseURL + "?user={user}&pass={password}");
        config.setRoleRegex("^.*?\"roles\"\\s*:\\s*\"([^\"]+)\".*$");

        config.setAuthorizationOption(SimpleWebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_WEB);

        getSecurityManager().saveAuthenticationProvider(config);

        prepareFilterChain(pattern, testFilterName);
        prepareAuthProviders(WebAuthProviderName);
        // SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod(RequestMethod.GET.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(
                                Base64.encodeBytes(
                                        (testUserName + ":" + testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        Authentication auth = getAuth(testFilterName, testUserName, null, null);

        // role from default service
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        // role from Auth provider
        assertTrue(auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE));

        // role from Web Response
        assertTrue(auth.getAuthorities().contains(new GeoServerRole("ROLE_WEB_SERVICE_ROLE")));
    }

    @Test
    public void testWebAuthWithCredentialsInHeader() throws Exception {

        SimpleWebAuthenticationConfig config = new SimpleWebAuthenticationConfig();
        config.setClassName(SimpleWebServiceAuthenticationProvider.class.getName());
        config.setName(webAuthProviderUseHeader);
        config.setConnectionURL(baseURL + "?user={user}&pass={password}");
        config.setRoleRegex("^.*?\"roles\"\\s*:\\s*\"([^\"]+)\".*$");

        config.setAuthorizationOption(SimpleWebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_WEB);
        config.setUseHeader(true);

        getSecurityManager().saveAuthenticationProvider(config);

        prepareFilterChain(pattern, testFilterName);
        prepareAuthProviders(webAuthProviderUseHeader);
        // SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod(RequestMethod.GET.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(
                                Base64.encodeBytes(
                                        (testUserName + ":" + testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        Authentication auth = getAuth(testFilterName, testUserName, null, null);

        // role from default service
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        // role from Auth provider
        assertTrue(auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE));

        // role from Web Response
        assertTrue(auth.getAuthorities().contains(new GeoServerRole("ROLE_WEB_SERVICE_ROLE")));
    }

    @Test
    public void testWebAuthWithRoleService() throws Exception {

        SimpleWebAuthenticationConfig config = new SimpleWebAuthenticationConfig();
        config.setClassName(SimpleWebServiceAuthenticationProvider.class.getName());
        config.setName(webAuthProviderWithRoleService);
        config.setConnectionURL(baseURL + "?user={user}&pass={password}");
        config.setAuthorizationOption(
                SimpleWebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_SERVICE);
        config.setRoleServiceName(getSecurityManager().getActiveRoleService().getName());
        getSecurityManager().saveAuthenticationProvider(config);

        prepareFilterChain(pattern, testFilterName);
        prepareAuthProviders(webAuthProviderWithRoleService);
        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod(RequestMethod.GET.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        chain = new MockFilterChain();

        request.addHeader(
                "Authorization",
                "Basic "
                        + new String(
                                Base64.encodeBytes(
                                        (testUserName + ":" + testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        Authentication auth = getAuth(testFilterName, testUserName, null, null);

        // role from selected service
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        // role from Auth provider
        assertTrue(auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE));
    }

    Authentication getAuth(String filterName, String user, Integer idleTime, Integer liveTime) {

        Map<String, byte[]> map = getCache().cache.get(filterName);
        if (map == null) return null;
        Authentication result = null;
        String cacheKey = null;
        for (Entry<String, byte[]> entry : map.entrySet()) {
            Authentication auth = getCache().deserializeAuthentication(entry.getValue());
            Object o = auth.getPrincipal();

            if (o instanceof UserDetails) {
                if (user.equals(((UserDetails) o).getUsername())) {
                    result = auth;
                    cacheKey = entry.getKey();
                    break;
                }
            }
            if (o instanceof Principal) {
                if (user.equals(((Principal) o).getName())) {
                    result = auth;
                    cacheKey = entry.getKey();
                    break;
                }
            }
            if (o instanceof String) {
                if (user.equals(((String) o))) {
                    result = auth;
                    cacheKey = entry.getKey();
                    break;
                }
            }
        }

        if (result != null) {
            Integer[] seconds = getCache().getExpireTimes(filterName, cacheKey);
            if (idleTime == null)
                assertEquals(TestingAuthenticationCache.DEFAULT_IDLE_SECS, seconds[0]);
            else assertEquals(idleTime, seconds[0]);

            if (liveTime == null)
                assertEquals(TestingAuthenticationCache.DEFAULT_LIVE_SECS, seconds[1]);
            else assertEquals(liveTime, seconds[1]);
        }

        return result;
    }
}

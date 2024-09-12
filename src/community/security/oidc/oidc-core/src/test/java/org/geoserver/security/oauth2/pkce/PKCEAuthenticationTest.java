/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.pkce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

public class PKCEAuthenticationTest extends GeoServerSystemTestSupport {

    private OpenIdConnectFilterConfig config;

    @Before
    public void setup() {
        config = new OpenIdConnectFilterConfig();
        config.setName("testOIDC");
        config.setAccessTokenUri("https://www.connectid/fake/test");
        config.setUserAuthorizationUri("https://www.connectid/fake/test");
        config.setCheckTokenEndpointUrl("https://www.connectid/fake/test");
        config.setCliendId("test");
        config.setEnableRedirectAuthenticationEntryPoint(true);
        config.setLoginEndpoint("/j_spring_oauth2_openid_connect_login");
        config.setLoginEndpoint("/j_spring_oauth2_openid_connect_logout");
        config.setUsePKCE(true);
    }

    @Test
    public void testAuthenticationEntryPoint() throws Exception {
        PKCEAuthenticationEntryPoint entryPoint = new PKCEAuthenticationEntryPoint(config);

        MockHttpServletRequest request = createRequest("web/", true);

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, null);

        assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
        String url = response.getHeader(HttpHeaders.LOCATION);
        assertTrue(url.contains("code_challenge="));
        assertTrue(url.contains("code_challenge_method=S256"));

        HttpSession session = request.getSession();
        String codeVerifier = (String) session.getAttribute("OIDC_CODE_VERIFIER");
        assertNotNull(codeVerifier);
    }

    @Test
    public void testPKCERequestEnhancer() throws Exception {
        PKCERequestEnhancer enhancer = new PKCERequestEnhancer(config);

        AccessTokenRequest request = new DefaultAccessTokenRequest();
        String validator = "1234";
        request.set(PkceParameterNames.CODE_VERIFIER, validator);

        MultiValueMap<String, String> form =
                new MultiValueMapAdapter(new HashMap<String, String>());
        enhancer.enhance(request, null, form, null);

        assertEquals(validator, form.getFirst(PkceParameterNames.CODE_VERIFIER));
    }
}

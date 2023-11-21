package org.geoserver.security.oauth2.pkce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class PKCEAuthenticationEntryPointTest extends GeoServerSystemTestSupport {

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
        request.getSession().setAttribute("OIDC_CODE_VERIFIER", "1234");

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
}

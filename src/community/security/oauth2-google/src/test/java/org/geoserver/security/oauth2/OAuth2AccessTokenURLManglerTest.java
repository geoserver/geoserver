/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.geoserver.ows.URLMangler.URLType;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public class OAuth2AccessTokenURLManglerTest extends AbstractOAuth2RestTemplateTest {
    
    @Test
    public void testBearerAccessTokenURLMangler() {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
        token.setTokenType("access_token");
        token.setTokenType(OAuth2AccessToken.BEARER_TYPE);
        restTemplate.getOAuth2ClientContext().setAccessToken(token);
        authenticator.authenticate(resource, restTemplate.getOAuth2ClientContext(), request);
        String auth = request.getHeaders().getFirst("Authorization");

        assertTrue(auth.startsWith(OAuth2AccessToken.BEARER_TYPE));

        OAuth2AccessTokenURLMangler urlMangler = new OAuth2AccessTokenURLMangler(getSecurityManager());
        urlMangler.geoServerOauth2RestTemplate = restTemplate;

        assertNotNull(urlMangler);
        
        Authentication user = new UsernamePasswordAuthenticationToken("admin", "geoserver", Arrays.asList(
                new GrantedAuthority[] { new SimpleGrantedAuthority("ROLE_ADMINISTRATOR") } ));
        SecurityContextHolder.getContext().setAuthentication(user);

        StringBuilder baseURL = new StringBuilder("http://test.geoserver-org/wms");
        StringBuilder path = new StringBuilder();
        Map<String, String> kvp = new HashMap<String, String>();
        kvp.put("request", "GetCapabilities");
        
        urlMangler.mangleURL(baseURL, path, kvp, URLType.SERVICE);
        
        assertTrue(kvp.containsKey("access_token"));
        assertTrue("12345".equals(kvp.get("access_token")));
    }

}

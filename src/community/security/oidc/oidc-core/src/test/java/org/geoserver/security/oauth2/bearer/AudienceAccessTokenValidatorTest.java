/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.bearer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class AudienceAccessTokenValidatorTest {

    String clientId = "MYCLIENTID";

    public AudienceAccessTokenValidator getValidator() {
        AudienceAccessTokenValidator validator = new AudienceAccessTokenValidator();
        return validator;
    }

    public OpenIdConnectFilterConfig getConfig() {
        OpenIdConnectFilterConfig result = new OpenIdConnectFilterConfig();
        result.setCliendId(clientId);
        return result;
    }

    @Test
    public void testAzureGood() throws Exception {
        Map claims = new HashMap();
        claims.put("appid", clientId);

        AudienceAccessTokenValidator validator = getValidator();
        OpenIdConnectFilterConfig config = getConfig();

        validator.verifyToken(config, claims, null);
    }

    @Test
    public void testKeyCloakGood() throws Exception {
        Map claims = new HashMap();
        claims.put("azp", clientId);

        AudienceAccessTokenValidator validator = getValidator();
        OpenIdConnectFilterConfig config = getConfig();

        validator.verifyToken(config, claims, null);
    }

    @Test
    public void testSelfCreatedGood() throws Exception {
        Map claims = new HashMap();
        claims.put("aud", clientId);

        AudienceAccessTokenValidator validator = getValidator();
        OpenIdConnectFilterConfig config = getConfig();

        validator.verifyToken(config, claims, null);
    }

    @Test
    public void testAudListGood() throws Exception {
        Map claims = new HashMap();
        claims.put("aud", Arrays.asList(clientId, "other-client"));

        AudienceAccessTokenValidator validator = getValidator();
        OpenIdConnectFilterConfig config = getConfig();

        validator.verifyToken(config, claims, null);
    }

    @Test(expected = Exception.class)
    public void testBad1() throws Exception {
        Map claims = new HashMap();
        claims.put("aud", "badaud");

        AudienceAccessTokenValidator validator = getValidator();
        OpenIdConnectFilterConfig config = getConfig();

        validator.verifyToken(config, claims, null);
    }

    @Test(expected = Exception.class)
    public void testBad2() throws Exception {
        Map claims = new HashMap();
        claims.put("azp", "badaud");

        AudienceAccessTokenValidator validator = getValidator();
        OpenIdConnectFilterConfig config = getConfig();

        validator.verifyToken(config, claims, null);
    }

    @Test(expected = Exception.class)
    public void testBad3() throws Exception {
        Map claims = new HashMap();
        claims.put("appid", "badaud");

        AudienceAccessTokenValidator validator = getValidator();
        OpenIdConnectFilterConfig config = getConfig();

        validator.verifyToken(config, claims, null);
    }
}

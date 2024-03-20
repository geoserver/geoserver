/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.username.JwtHeaderUserNameExtractorTest;
import org.junit.Test;

/** tests the TokenAudienceValidator class */
public class TokenAudienceValidatorTest {

    /**
     * simple test - the access token has an "azp" claim that is "live-key2". - We test that the
     * token has this value.
     *
     * @throws Exception
     */
    @Test
    public void test_simple() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenAudience(true);
        config.setValidateTokenAudienceClaimName("azp");
        config.setValidateTokenAudienceClaimValue("live-key2"); // this is correct

        TokenValidator validator = new TokenValidator(config);

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * simple test - the access token has an "azp" claim that is "live-key2". - We test that the
     * token has a different value (i.e. fail). - We expect an exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void test_simple_fail() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenAudience(true);
        config.setValidateTokenAudienceClaimName("azp");
        config.setValidateTokenAudienceClaimValue("ABCD"); // doesnt match

        TokenValidator validator = new TokenValidator(config);

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }
}

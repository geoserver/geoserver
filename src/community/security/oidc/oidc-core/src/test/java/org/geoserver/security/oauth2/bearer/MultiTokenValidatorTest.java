/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.bearer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.junit.Test;

public class MultiTokenValidatorTest {

    TokenValidator alwaysThrows =
            new TokenValidator() {
                @Override
                public void verifyToken(
                        OpenIdConnectFilterConfig config, Map accessTokenClaims, Map userInfoClaims)
                        throws Exception {
                    throw new Exception("bad");
                }
            };

    TokenValidator neverThrows =
            new TokenValidator() {
                @Override
                public void verifyToken(
                        OpenIdConnectFilterConfig config, Map accessTokenClaims, Map userInfoClaims)
                        throws Exception {}
            };

    // null - should run fine
    @Test
    public void testNull() throws Exception {
        MultiTokenValidator validator = new MultiTokenValidator(null);
        validator.verifyToken(null, null, null);
    }

    // empty - should run fine
    @Test
    public void testEmpty() throws Exception {
        MultiTokenValidator validator = new MultiTokenValidator(new ArrayList<>());
        validator.verifyToken(null, null, null);
    }

    // doesn't throw
    @Test
    public void testRunsGood() throws Exception {
        List<TokenValidator> validators =
                Arrays.asList(new TokenValidator[] {neverThrows, neverThrows});
        MultiTokenValidator validator = new MultiTokenValidator(validators);
        validator.verifyToken(null, null, null);
    }

    //  throws on first
    @Test(expected = Exception.class)
    public void testRunsBad1() throws Exception {
        List<TokenValidator> validators =
                Arrays.asList(new TokenValidator[] {alwaysThrows, neverThrows});
        MultiTokenValidator validator = new MultiTokenValidator(validators);
        validator.verifyToken(null, null, null);
    }

    //  throws on last
    @Test(expected = Exception.class)
    public void testRunsBad2() throws Exception {
        List<TokenValidator> validators =
                Arrays.asList(new TokenValidator[] {neverThrows, alwaysThrows});
        MultiTokenValidator validator = new MultiTokenValidator(validators);
        validator.verifyToken(null, null, null);
    }
}

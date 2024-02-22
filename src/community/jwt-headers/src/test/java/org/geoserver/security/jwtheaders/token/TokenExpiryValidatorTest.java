/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import java.util.Calendar;
import java.util.Map;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.username.JwtHeaderUserNameExtractorTest;
import org.junit.Test;

/** tests TokenExpiryValidator class */
public class TokenExpiryValidatorTest {

    /**
     * uses an old access token that has already expired. should throw.
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void test_already_expired() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenExpiry(true);

        TokenValidator validator = new TokenValidator(config);

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * we create a new access token that expires tomorrow - this should pass.
     *
     * <p>NOTE: the token's signature will fail validation.
     *
     * @throws Exception
     */
    @Test
    public void test_not_expired() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenExpiry(true);

        TokenValidator validator = new TokenValidator(config);

        String token = getNotExpiredToken(JwtHeaderUserNameExtractorTest.accessToken);
        validator.validate(token);
    }

    /**
     * creates a token that expires tomorrow. NOTE: signature will be invalid.
     *
     * @return
     * @throws Exception
     */
    public static String getNotExpiredToken(String token) throws Exception {
        // this one is expired

        JWSObject jwsToken_expired = JWSObject.parse(token);

        Payload payload = jwsToken_expired.getPayload();
        Map<String, Object> claims = payload.toJSONObject();

        // expire tomorrow
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        cal.getTime().toInstant().getEpochSecond();
        claims.put("exp", cal.getTime().toInstant().getEpochSecond());

        payload = new Payload(claims);

        return jwsToken_expired.getHeader().toBase64URL()
                + "."
                + payload.toBase64URL()
                + "."
                + jwsToken_expired.getParsedParts()[2];
    }
}

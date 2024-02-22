/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import static org.mockito.Mockito.*;

import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.username.JwtHeaderUserNameExtractorTest;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class TokenValidatorTest {

    /**
     * quick test - make sure that the individual validators are being called
     *
     * @throws Exception
     */
    @Test
    public void testValidator() throws Exception {

        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        // mock the actual validators
        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        // make sure they are being called

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);

        verify(validator.tokenSignatureValidator).validate(ArgumentMatchers.any());
        verify(validator.tokenExpiryValidator).validate(ArgumentMatchers.any());
        verify(validator.tokenEndpointValidator).validate(ArgumentMatchers.any());
        verify(validator.tokenAudienceValidator).validate(ArgumentMatchers.any());
    }

    /**
     * quick test - make sure that when an individual validator failed. - tokenSignatureValidator
     * will fail --> throw exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testValidator_fail_tokenSignatureValidator() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        // mock the actual validators
        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        doThrow(new Exception("boom"))
                .when(validator.tokenSignatureValidator)
                .validate(ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * quick test - make sure that when an individual validator failed. - tokenExpiryValidator will
     * fail --> throw exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testValidator_fail_tokenExpiryValidator() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        // mock the actual validators
        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        doThrow(new Exception("boom"))
                .when(validator.tokenExpiryValidator)
                .validate(ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * quick test - make sure that when an individual validator failed. - tokenEndpointValidator
     * will fail --> throw exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testValidator_fail_tokenEndpointValidator() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        // mock the actual validators
        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        doThrow(new Exception("boom"))
                .when(validator.tokenEndpointValidator)
                .validate(ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * quick test - make sure that when an individual validator failed. - tokenAudienceValidator
     * will fail --> throw exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testValidator_fail_tokenAudienceValidator() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        // mock the actual validators
        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        doThrow(new Exception("boom"))
                .when(validator.tokenAudienceValidator)
                .validate(ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }
}

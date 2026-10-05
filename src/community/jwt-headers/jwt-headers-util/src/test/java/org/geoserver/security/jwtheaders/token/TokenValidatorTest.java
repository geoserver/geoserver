/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import org.geoserver.security.jwtheaders.JwtConfiguration;
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

        JwtConfiguration config = new JwtConfiguration();
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

    /** The endpoint is not called for a token the local checks already rejected. */
    @Test
    public void testValidator_localChecksBeforeEndpoint() throws Exception {
        JwtConfiguration config = new JwtConfiguration();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        doThrow(new Exception("boom")).when(validator.tokenAudienceValidator).validate(ArgumentMatchers.any());

        Exception thrown =
                assertThrows(Exception.class, () -> validator.validate(JwtHeaderUserNameExtractorTest.accessToken));
        assertEquals("boom", thrown.getMessage());

        verify(validator.tokenEndpointValidator, never()).validate(ArgumentMatchers.any());
    }

    /** With validation disabled no claims are returned, since none of them can be trusted. */
    @Test
    public void testValidateAndParse_disabled() throws Exception {
        JwtConfiguration config = new JwtConfiguration();
        config.setValidateToken(false);

        assertNull(new TokenValidator(config).validateAndParse(JwtHeaderUserNameExtractorTest.accessToken));
    }

    /**
     * quick test - make sure that when an individual validator failed. - tokenSignatureValidator will fail --> throw
     * exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testValidator_fail_tokenSignatureValidator() throws Exception {
        JwtConfiguration config = new JwtConfiguration();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        // mock the actual validators
        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        doThrow(new Exception("boom")).when(validator.tokenSignatureValidator).validate(ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * quick test - make sure that when an individual validator failed. - tokenExpiryValidator will fail --> throw
     * exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testValidator_fail_tokenExpiryValidator() throws Exception {
        JwtConfiguration config = new JwtConfiguration();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        // mock the actual validators
        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        doThrow(new Exception("boom")).when(validator.tokenExpiryValidator).validate(ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * quick test - make sure that when an individual validator failed. - tokenEndpointValidator will fail --> throw
     * exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testValidator_fail_tokenEndpointValidator() throws Exception {
        JwtConfiguration config = new JwtConfiguration();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        // mock the actual validators
        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        doThrow(new Exception("boom")).when(validator.tokenEndpointValidator).validate(ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * quick test - make sure that when an individual validator failed. - tokenAudienceValidator will fail --> throw
     * exception
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testValidator_fail_tokenAudienceValidator() throws Exception {
        JwtConfiguration config = new JwtConfiguration();
        config.setValidateToken(true);

        TokenValidator validator = new TokenValidator(config);

        // mock the actual validators
        validator.tokenSignatureValidator = Mockito.mock(TokenSignatureValidator.class);
        validator.tokenExpiryValidator = Mockito.mock(TokenExpiryValidator.class);
        validator.tokenEndpointValidator = Mockito.mock(TokenEndpointValidator.class);
        validator.tokenAudienceValidator = Mockito.mock(TokenAudienceValidator.class);

        doThrow(new Exception("boom")).when(validator.tokenAudienceValidator).validate(ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }
}

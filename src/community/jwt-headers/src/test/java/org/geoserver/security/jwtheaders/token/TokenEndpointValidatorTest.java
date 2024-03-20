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

/** tests TokenEndpointValidator class */
public class TokenEndpointValidatorTest {

    /**
     * tests a good endpoint - we use a mock to return this json; {
     * "sub":"ea33e3cc-f0e1-4218-89cb-8d48c27eee3d" }
     *
     * @throws Exception
     */
    @Test
    public void testGoodEndpoint() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenAgainstURL(true);
        config.setValidateTokenAgainstURLEndpoint("http://myendpointurl.com");

        TokenValidator validator = new TokenValidator(config);

        validator.tokenEndpointValidator = spy(validator.tokenEndpointValidator);

        doReturn(" {\"sub\":\"ea33e3cc-f0e1-4218-89cb-8d48c27eee3d\"}")
                .when(validator.tokenEndpointValidator)
                .download(ArgumentMatchers.any(), ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * tests a bad endpoint - we use a mock to return empty.
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testBadEndpoint() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenAgainstURL(true);
        config.setValidateTokenAgainstURLEndpoint("http://myendpointurl.com");

        TokenValidator validator = new TokenValidator(config);

        validator.tokenEndpointValidator = spy(validator.tokenEndpointValidator);

        doReturn(null)
                .when(validator.tokenEndpointValidator)
                .download(ArgumentMatchers.any(), ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * tests a good endpoint - we use a mock to return this json; {
     * "sub":"ea33e3cc-f0e1-4218-89cb-8d48c27eee3d" } The access token also has this as the subject
     * (sub), so its good!
     *
     * @throws Exception
     */
    @Test
    public void testGoodEndpointGoodSubject() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenAgainstURL(true);
        config.setValidateSubjectWithEndpoint(true);
        config.setValidateTokenAgainstURLEndpoint("http://myendpointurl.com");

        TokenValidator validator = new TokenValidator(config);

        validator.tokenEndpointValidator = spy(validator.tokenEndpointValidator);

        doReturn(" {\"sub\":\"ea33e3cc-f0e1-4218-89cb-8d48c27eee3d\"}")
                .when(validator.tokenEndpointValidator)
                .download(ArgumentMatchers.any(), ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }

    /**
     * tests a good endpoint - we use a mock to return this json; { "sub":"BAD-SUBJECT" } The access
     * token has a different subject, so this should throw
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testGoodEndpointBadSubject() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenAgainstURL(true);
        config.setValidateSubjectWithEndpoint(true);
        config.setValidateTokenAgainstURLEndpoint("http://myendpointurl.com");

        TokenValidator validator = new TokenValidator(config);

        validator.tokenEndpointValidator = spy(validator.tokenEndpointValidator);

        doReturn(" {\"sub\":\"BAD-SUBJECT\"}")
                .when(validator.tokenEndpointValidator)
                .download(ArgumentMatchers.any(), ArgumentMatchers.any());

        String token = JwtHeaderUserNameExtractorTest.accessToken;
        validator.validate(token);
    }
}

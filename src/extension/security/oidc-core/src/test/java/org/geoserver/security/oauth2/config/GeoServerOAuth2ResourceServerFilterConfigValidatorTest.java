/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.geoserver.security.GeoServerSecurityManager;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link GeoServerOAuth2ResourceServerFilterConfigValidator}. */
public class GeoServerOAuth2ResourceServerFilterConfigValidatorTest {

    private GeoServerOAuth2ResourceServerFilterConfigValidator validator;
    private GeoServerSecurityManager securityManager;

    @Before
    public void setUp() {
        securityManager = mock(GeoServerSecurityManager.class);
        validator = new GeoServerOAuth2ResourceServerFilterConfigValidator(securityManager);
    }

    @Test
    public void testConstructor() {
        assertNotNull(validator);
    }

    @Test
    public void testCreateFilterException() {
        String errorId = "TEST_ERROR_ID";
        Object arg1 = "arg1";
        Object arg2 = "arg2";

        OAuth2FilterConfigException exception = validator.createFilterException(errorId, arg1, arg2);

        assertNotNull(exception);
        assertEquals(errorId, exception.getId());
        assertEquals(2, exception.getArgs().length);
        assertEquals(arg1, exception.getArgs()[0]);
        assertEquals(arg2, exception.getArgs()[1]);
    }

    @Test
    public void testCreateFilterExceptionNoArgs() {
        String errorId = "TEST_ERROR_ID_NO_ARGS";

        OAuth2FilterConfigException exception = validator.createFilterException(errorId);

        assertNotNull(exception);
        assertEquals(errorId, exception.getId());
        assertEquals(0, exception.getArgs().length);
    }

    @Test
    public void testCreateFilterExceptionReturnsCorrectType() {
        OAuth2FilterConfigException exception = validator.createFilterException("ERROR");

        assertTrue(exception instanceof OAuth2FilterConfigException);
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;

public class AuthenticationKeyFilterConfigValidatorTest extends GeoServerMockTestSupport {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    AuthenticationKeyFilterConfigValidator validator;

    @Before
    public void setValidator() {
        validator = new AuthenticationKeyFilterConfigValidator(getSecurityManager());
    }

    @Test
    public void testCasFilterConfigValidation() throws Exception {
        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName("testAuthKey");

        check(config);
        //        validator.validateFilterConfig(config);
    }

    public void check(AuthenticationKeyFilterConfig config) throws Exception {

        boolean failed = false;

        try {
            validator.validateFilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.USER_GROUP_SERVICE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());

            failed = true;
        }
        assertTrue(failed);

        config.setUserGroupServiceName("blabla");
        failed = false;
        try {
            validator.validateFilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_USER_GROUP_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);
        config.setUserGroupServiceName(XMLUserGroupService.DEFAULT_NAME);

        config.setAuthKeyParamName(null);

        failed = false;
        try {
            validator.validateFilterConfig(config);
        } catch (AuthenticationKeyFilterConfigException ex) {
            assertEquals(
                    AuthenticationKeyFilterConfigException.AUTH_KEY_PARAM_NAME_REQUIRED,
                    ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setAuthKeyParamName("authkey");
        failed = false;
        try {
            validator.validateFilterConfig(config);
        } catch (AuthenticationKeyFilterConfigException ex) {
            assertEquals(
                    AuthenticationKeyFilterConfigException.AUTH_KEY_MAPPER_NAME_REQUIRED,
                    ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);
        config.setAuthKeyMapperName("blabla");

        failed = false;
        try {
            validator.validateFilterConfig(config);
        } catch (AuthenticationKeyFilterConfigException ex) {
            assertEquals(
                    AuthenticationKeyFilterConfigException.AUTH_KEY_MAPPER_NOT_FOUND_$1,
                    ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        //        AuthenticationKeyMapper mapper=
        // GeoServerExtensions.extensions(AuthenticationKeyMapper.class).get(0);
        //        config.setAuthKeyMapperName(mapper.getBeanName());
        //        validator.validateFilterConfig(config);

    }
}

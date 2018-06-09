/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;

public class CasFilterConfigValidatorTest extends GeoServerMockTestSupport {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    CasFilterConfigValidator validator;

    @Before
    public void setValidator() {
        validator = new CasFilterConfigValidator(getSecurityManager());
    }

    @Test
    public void testCasFilterConfigValidation() throws Exception {
        CasAuthenticationFilterConfig config = new CasAuthenticationFilterConfig();
        config.setClassName(GeoServerCasAuthenticationFilter.class.getName());
        config.setName("testCAS");

        check(config);
        validator.validateCASFilterConfig(config);
    }

    public void check(CasAuthenticationFilterConfig config) throws Exception {

        boolean failed = false;
        try {
            validator.validateCASFilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.ROLE_SOURCE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());

            failed = true;
        }
        assertTrue(failed);

        config.setRoleSource(PreAuthenticatedUserNameRoleSource.UserGroupService);
        failed = false;
        try {
            validator.validateCASFilterConfig(config);
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
            validator.validateCASFilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_USER_GROUP_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setUserGroupServiceName(XMLUserGroupService.DEFAULT_NAME);

        config.setRoleSource(PreAuthenticatedUserNameRoleSource.RoleService);
        config.setRoleServiceName("blabla");
        failed = false;
        try {
            validator.validateCASFilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setRoleServiceName(XMLRoleService.DEFAULT_NAME);

        config.setRoleSource(PreAuthenticatedUserNameRoleSource.Header);
        failed = false;
        try {
            validator.validateCASFilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.ROLES_HEADER_ATTRIBUTE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());

            failed = true;
        }
        assertTrue(failed);
        config.setRolesHeaderAttribute("roles");

        config.setRoleConverterName("unknown");
        failed = false;
        try {
            validator.validateCASFilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_CONVERTER, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("unknown", ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setRoleConverterName(null);

        config.setCasServerUrlPrefix(null);

        failed = false;
        try {
            validator.validateCASFilterConfig(config);
        } catch (CasFilterConfigException ex) {
            assertEquals(CasFilterConfigException.CAS_SERVER_URL_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setCasServerUrlPrefix("blabal");
        failed = false;
        try {
            validator.validateCASFilterConfig(config);
        } catch (CasFilterConfigException ex) {
            assertEquals(CasFilterConfigException.CAS_SERVER_URL_MALFORMED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);
        config.setCasServerUrlPrefix("http://casserver/case");

        config.setUrlInCasLogoutPage("blbla");
        failed = false;
        try {
            validator.validateCASFilterConfig(config);
        } catch (CasFilterConfigException ex) {
            assertEquals(CasFilterConfigException.CAS_URL_IN_LOGOUT_PAGE_MALFORMED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);
        config.setUrlInCasLogoutPage("http://localhost/gesoerver");

        config.setProxyCallbackUrlPrefix("blabal");
        failed = false;
        try {
            validator.validateCASFilterConfig(config);
        } catch (CasFilterConfigException ex) {
            assertEquals(CasFilterConfigException.CAS_PROXYCALLBACK_MALFORMED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setProxyCallbackUrlPrefix("http://localhost/callback");
        failed = false;
        try {
            validator.validateCASFilterConfig(config);
        } catch (CasFilterConfigException ex) {
            assertEquals(CasFilterConfigException.CAS_PROXYCALLBACK_NOT_HTTPS, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setProxyCallbackUrlPrefix("https://localhost/callback");
        validator.validateCASFilterConfig(config);
    }
}

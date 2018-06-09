/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.*;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig.J2EERoleSource;
import org.geoserver.security.filter.*;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Test;

public class FilterConfigValidatorTest extends GeoServerMockTestSupport {

    @Test
    public void testDigestConfigValidation() throws Exception {
        DigestAuthenticationFilterConfig config = new DigestAuthenticationFilterConfig();
        config.setClassName(GeoServerDigestAuthenticationFilter.class.getName());
        config.setName("testDigest");

        GeoServerSecurityManager secMgr = getSecurityManager();

        FilterConfigValidator validator = new FilterConfigValidator(secMgr);

        try {
            validator.validateFilterConfig(config);
            fail("no user group service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.USER_GROUP_SERVICE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUserGroupServiceName("blabla");
        try {
            validator.validateFilterConfig(config);
            fail("unknown user group service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_USER_GROUP_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
        }

        config.setUserGroupServiceName(XMLUserGroupService.DEFAULT_NAME);
        config.setNonceValiditySeconds(-1);

        try {
            validator.validateFilterConfig(config);
            fail("invalid nonce should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.INVALID_SECONDS, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setNonceValiditySeconds(100);
        validator.validateFilterConfig(config);
    }

    @Test
    public void testRoleFilterConfigValidation() throws Exception {
        RoleFilterConfig config = new RoleFilterConfig();
        config.setClassName(GeoServerRoleFilter.class.getName());
        config.setName("testRoleFilter");

        GeoServerSecurityManager secMgr = getSecurityManager();
        FilterConfigValidator validator = new FilterConfigValidator(secMgr);
        try {
            validator.validateFilterConfig(config);
            fail("no header attribute should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.HEADER_ATTRIBUTE_NAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }
        config.setHttpResponseHeaderAttrForIncludedRoles("roles");
        config.setRoleConverterName("unknown");

        try {
            validator.validateFilterConfig(config);
            fail("unkonwn role converter should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_CONVERTER, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("unknown", ex.getArgs()[0]);
        }

        config.setRoleConverterName(null);
        validator.validateFilterConfig(config);
    }

    @Test
    public void testSecurityInterceptorFilterConfigValidation() throws Exception {
        SecurityInterceptorFilterConfig config = new SecurityInterceptorFilterConfig();
        config.setClassName(GeoServerSecurityInterceptorFilter.class.getName());
        config.setName("testInterceptFilter");

        GeoServerSecurityManager secMgr = getSecurityManager();
        FilterConfigValidator validator = new FilterConfigValidator(secMgr);
        try {
            validator.validateFilterConfig(config);
            fail("no metadata source should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.SECURITY_METADATA_SOURCE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setSecurityMetadataSource("unknown");
        try {
            validator.validateFilterConfig(config);
            fail("unknown metadata source should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_SECURITY_METADATA_SOURCE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("unknown", ex.getArgs()[0]);
        }
    }

    @Test
    public void testX509FilterConfigValidation() throws Exception {
        X509CertificateAuthenticationFilterConfig config =
                new X509CertificateAuthenticationFilterConfig();
        config.setClassName(GeoServerX509CertificateAuthenticationFilter.class.getName());
        config.setName("testX509");

        check((J2eeAuthenticationBaseFilterConfig) config);
    }

    @Test
    public void testUsernamePasswordFilterConfigValidation() throws Exception {
        UsernamePasswordAuthenticationFilterConfig config =
                new UsernamePasswordAuthenticationFilterConfig();
        config.setClassName(GeoServerUserNamePasswordAuthenticationFilter.class.getName());
        config.setName("testUsernamePassword");

        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());
        try {
            validator.validateFilterConfig(config);
            fail("no user should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.USER_PARAMETER_NAME_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUsernameParameterName("user");
        try {
            validator.validateFilterConfig(config);
            fail("no password should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.PASSWORD_PARAMETER_NAME_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setPasswordParameterName("password");
        validator.validateFilterConfig(config);
    }

    @Test
    public void testJ2eeFilterConfigValidation() throws Exception {
        J2eeAuthenticationFilterConfig config = new J2eeAuthenticationFilterConfig();
        config.setClassName(GeoServerJ2eeAuthenticationFilter.class.getName());
        config.setName("testJ2ee");

        check((J2eeAuthenticationBaseFilterConfig) config);
    }

    @Test
    public void testExceptionTranslationFilterConfigValidation() throws Exception {
        ExceptionTranslationFilterConfig config = new ExceptionTranslationFilterConfig();
        config.setClassName(GeoServerExceptionTranslationFilter.class.getName());
        config.setName("testEx");

        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());
        config.setAuthenticationFilterName("unknown");

        try {
            validator.validateFilterConfig(config);
            fail("invalid entry point should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.INVALID_ENTRY_POINT, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("unknown", ex.getArgs()[0]);
        }

        config.setAuthenticationFilterName(
                GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);

        try {
            validator.validateFilterConfig(config);
            fail("no auth entry point should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.NO_AUTH_ENTRY_POINT, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals(GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR, ex.getArgs()[0]);
        }

        config.setAuthenticationFilterName(null);
        validator.validateFilterConfig(config);
    }

    public void check(PreAuthenticatedUserNameFilterConfig config) throws Exception {

        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());
        try {
            validator.validateFilterConfig(config);
            fail("no role source should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.ROLE_SOURCE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setRoleSource(
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource
                        .UserGroupService);
        try {
            validator.validateFilterConfig(config);
            fail("no user group service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.USER_GROUP_SERVICE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUserGroupServiceName("blabla");
        try {
            validator.validateFilterConfig(config);
            fail("unknown group service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_USER_GROUP_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
        }

        config.setUserGroupServiceName(XMLUserGroupService.DEFAULT_NAME);

        config.setRoleSource(
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource
                        .RoleService);
        config.setRoleServiceName("blabla");
        try {
            validator.validateFilterConfig(config);
            fail("unknown role service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
        }

        config.setRoleServiceName(XMLRoleService.DEFAULT_NAME);
        config.setRoleSource(
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.Header);

        try {
            validator.validateFilterConfig(config);
            fail("no roles header attribute should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.ROLES_HEADER_ATTRIBUTE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setRolesHeaderAttribute("roles");
        config.setRoleConverterName("unknown");

        try {
            validator.validateFilterConfig(config);
            fail("unknown role converter should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_CONVERTER, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("unknown", ex.getArgs()[0]);
        }

        config.setRoleConverterName(null);
        validator.validateFilterConfig(config);
    }

    public void check(J2eeAuthenticationBaseFilterConfig config) throws Exception {
        check((PreAuthenticatedUserNameFilterConfig) config);
        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());

        config.setRoleSource(J2EERoleSource.J2EE);
        config.setRoleServiceName("blabla");
        try {
            validator.validateFilterConfig(config);
            fail("unknown role service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
        }

        config.setRoleServiceName(XMLRoleService.DEFAULT_NAME);
    }

    @Test
    public void testRequestHeaderFilterConfigValidation() throws Exception {
        RequestHeaderAuthenticationFilterConfig config =
                new RequestHeaderAuthenticationFilterConfig();
        config.setClassName(GeoServerRequestHeaderAuthenticationFilter.class.getName());
        config.setName("testRequestHeader");

        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());
        try {
            validator.validateFilterConfig(config);
            fail("no principal header attribute should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.PRINCIPAL_HEADER_ATTRIBUTE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setPrincipalHeaderAttribute("user");
        check((PreAuthenticatedUserNameFilterConfig) config);
    }
}

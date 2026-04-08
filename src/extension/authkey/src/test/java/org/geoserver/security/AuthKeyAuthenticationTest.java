/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.http.AbstractHttpClient;
import org.geotools.http.HTTPResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

public class AuthKeyAuthenticationTest extends AbstractAuthenticationProviderTest {

    static class TestHttpClient extends AbstractHttpClient {

        private String authkey;

        private String response;

        public TestHttpClient(String authkey, String response) {
            super();
            this.authkey = authkey;
            this.response = response;
        }

        @Override
        public HTTPResponse get(final URL url) throws IOException {

            return new HTTPResponse() {

                @Override
                public InputStream getResponseStream() throws IOException {
                    if (url.getPath().substring(1).equals(authkey)) {
                        return new ByteArrayInputStream(response.getBytes());
                    }
                    return new ByteArrayInputStream("".getBytes());
                }

                @Override
                public String getResponseHeader(String arg0) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getResponseCharset() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getContentType() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public void dispose() {
                    // TODO Auto-generated method stub

                }
            };
        }

        @Override
        public HTTPResponse post(URL url, InputStream in, String arg) throws IOException {
            return null;
        }
    }

    @BeforeClass
    public static void setupClass() {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("ALLOW_ENV_PARAMETRIZATION");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    @Override
    protected void onSetUp(org.geoserver.data.test.SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Test
    public void testMapperParameters() throws Exception {
        String authKeyUrlParam = "myAuthKeyParams";
        String filterName = "testAuthKeyParams1";

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("fakeMapper");

        Map<String, String> mapperParams = new HashMap<>();
        mapperParams.put("param1", "value1");
        mapperParams.put("param2", "value2");
        config.setMapperParameters(mapperParams);

        getSecurityManager().saveFilter(config);

        GeoServerAuthenticationKeyFilter filter =
                (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);
        assertTrue(filter.getMapper() instanceof FakeMapper);
        FakeMapper fakeMapper = (FakeMapper) filter.getMapper();
        assertEquals("value1", fakeMapper.getMapperParameter("param1"));
        assertEquals("value2", fakeMapper.getMapperParameter("param2"));
    }

    @Test
    public void testMapperParametersFromEnv() throws Exception {
        String authKeyUrlParam = "myAuthKeyParams";
        String filterName = "testAuthKeyParams2";

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("fakeMapper");

        System.setProperty("authkey_param1", "value1");
        System.setProperty("authkey_param2", "value2");
        try {
            Map<String, String> mapperParams = new HashMap<>();
            mapperParams.put("param1", "${authkey_param1}");
            mapperParams.put("param2", "${authkey_param2}");
            config.setMapperParameters(mapperParams);

            getSecurityManager().saveFilter(config);

            GeoServerAuthenticationKeyFilter filter =
                    (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);
            assertTrue(filter.getMapper() instanceof FakeMapper);
            FakeMapper fakeMapper = (FakeMapper) filter.getMapper();
            assertEquals("value1", fakeMapper.getMapperParameter("param1"));
            assertEquals("value2", fakeMapper.getMapperParameter("param2"));
        } finally {
            System.clearProperty("authkey_param1");
            System.clearProperty("authkey_param2");
        }
    }

    @Test
    public void testMapperParametersFromEnvWhenDisabled() throws Exception {
        String authKeyUrlParam = "myAuthKeyParams";
        String filterName = "testAuthKeyParams3";

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("fakeMapper");

        System.setProperty("authkey_param1", "value1");
        System.setProperty("authkey_param2", "value2");
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "false");
        GeoServerEnvironment.reloadAllowEnvParametrization();
        try {
            Map<String, String> mapperParams = new HashMap<>();
            mapperParams.put("param1", "${authkey_param1}");
            mapperParams.put("param2", "${authkey_param2}");
            config.setMapperParameters(mapperParams);

            getSecurityManager().saveFilter(config);

            GeoServerAuthenticationKeyFilter filter =
                    (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);
            assertTrue(filter.getMapper() instanceof FakeMapper);
            FakeMapper fakeMapper = (FakeMapper) filter.getMapper();
            assertEquals("${authkey_param1}", fakeMapper.getMapperParameter("param1"));
            assertEquals("${authkey_param2}", fakeMapper.getMapperParameter("param2"));
        } finally {
            System.clearProperty("authkey_param1");
            System.clearProperty("authkey_param2");
            System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
            GeoServerEnvironment.reloadAllowEnvParametrization();
        }
    }

    @Test
    public void testMapperParamsFilterConfigValidation() throws Exception {

        AuthenticationKeyFilterConfigValidator validator =
                new AuthenticationKeyFilterConfigValidator(getSecurityManager());

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName("fakeFilter");
        config.setUserGroupServiceName(XMLUserGroupService.DEFAULT_NAME);
        config.setAuthKeyParamName("authkey");
        config.setAuthKeyMapperName("fakeMapper");

        Map<String, String> mapperParams = new HashMap<>();
        mapperParams.put("param1", "value1");
        mapperParams.put("param2", "value2");

        config.setMapperParameters(mapperParams);

        boolean failed = false;
        try {
            validator.validateFilterConfig(config);
        } catch (FilterConfigException ex) {
            failed = true;
        }

        assertFalse(failed);

        mapperParams.put("param3", "value3");

        try {
            validator.validateFilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(AuthenticationKeyFilterConfigException.INVALID_AUTH_KEY_MAPPER_PARAMETER_$3, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("param3", ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);
    }

    @Test
    public void testFileBasedWithSessionEnabled() throws Exception {

        String authKeyUrlParam = "myAuthKey";
        String filterName = "testAuthKeyFilter1Enabled";

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("propertyMapper");

        // Let's make sure the internal user cache is disabled
        Map<String, String> mapperParams = new HashMap<>();
        mapperParams.put("cacheTtlSeconds", "0");
        config.setMapperParameters(mapperParams);
        getSecurityManager().saveFilter(config);

        GeoServerAuthenticationKeyFilter filter =
                (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);

        PropertyAuthenticationKeyMapper mapper = (PropertyAuthenticationKeyMapper) filter.getMapper();
        mapper.synchronize();

        prepareFilterChain(pattern, filterName);
        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);
        getSecurityManager().getAuthenticationCache().removeAll();

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        // Force to reload the property file
        mapper.synchronize();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());

        // test success
        String authKey = null;
        for (Entry<Object, Object> entry : mapper.authKeyProps.entrySet()) {
            if (testUserName.equals(entry.getValue())) {
                authKey = (String) entry.getKey();
                break;
            }
        }

        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setQueryString(authKeyUrlParam + "=" + authKey);
        request.addParameter(authKeyUrlParam, authKey);
        getProxy().doFilter(request, response, chain);
        assertNotEquals(response.getStatus(), MockHttpServletResponse.SC_MOVED_TEMPORARILY);

        SecurityContext ctx = (SecurityContext) request.getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, auth.getPrincipal());

        // check unknown user
        username = "unknown";
        password = username;
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.setQueryString(authKeyUrlParam + "=abc");
        request.addParameter(authKeyUrlParam, "abc");
        // Force to reload the property file
        mapper.synchronize();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testFileBasedWithSessionDisabled() throws Exception {

        String authKeyUrlParam = "myAuthKey";
        String filterName = "testAuthKeyFilter1Disabled";

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("propertyMapper");

        // Let's make sure the internal user cache is disabled
        Map<String, String> mapperParams = new HashMap<>();
        mapperParams.put("cacheTtlSeconds", "0");
        config.setMapperParameters(mapperParams);
        getSecurityManager().saveFilter(config);

        GeoServerAuthenticationKeyFilter filter =
                (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);

        PropertyAuthenticationKeyMapper mapper = (PropertyAuthenticationKeyMapper) filter.getMapper();
        mapper.synchronize();

        prepareFilterChain(pattern, filterName);
        modifyChain(pattern, false, true, null);

        SecurityContextHolder.getContext().setAuthentication(null);
        getSecurityManager().getAuthenticationCache().removeAll();

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        // Force to reload the property file
        mapper.synchronize();

        // test success
        String authKey = null;
        for (Entry<Object, Object> entry : mapper.authKeyProps.entrySet()) {
            if (testUserName.equals(entry.getValue())) {
                authKey = (String) entry.getKey();
                break;
            }
        }

        // check disabled user
        username = testUserName;
        password = username;
        updateUser("ug1", username, false);

        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.setQueryString(authKeyUrlParam + "=" + authKey);
        request.addParameter(authKeyUrlParam, authKey);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        updateUser("ug1", username, true);
        // Force to reload the property file
        mapper.synchronize();

        SecurityContextHolder.clearContext();
        getSecurityManager().getAuthenticationCache().removeAll();

        insertAnonymousFilter();
        request = createRequest("foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    @Test
    public void testUserPropertyWithCacheEnabled() throws Exception {

        String authKeyUrlParam = "myAuthKey";
        String filterName = "testAuthKeyFilter2Enabled";

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("userPropertyMapper");

        // Let's make sure the internal user cache is disabled
        Map<String, String> mapperParams = new HashMap<>();
        mapperParams.put("cacheTtlSeconds", "0");
        config.setMapperParameters(mapperParams);
        getSecurityManager().saveFilter(config);

        GeoServerAuthenticationKeyFilter filter =
                (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);

        UserPropertyAuthenticationKeyMapper mapper = (UserPropertyAuthenticationKeyMapper) filter.getMapper();
        // Force to reload the property file
        mapper.synchronize();

        prepareFilterChain(pattern, filterName);
        modifyChain(pattern, false, false, null);

        SecurityContextHolder.getContext().setAuthentication(null);
        getSecurityManager().getAuthenticationCache().removeAll();

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());

        // test success
        GeoServerUser user =
                (GeoServerUser) getSecurityManager().loadUserGroupService("ug1").loadUserByUsername(testUserName);
        // Force to reload the property file
        mapper.synchronize();
        // Make sure the cache is cleared
        mapper.resetUserCache();
        String authKey = user.getProperties().getProperty(mapper.getUserPropertyName());
        assertNotNull(authKey);

        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.setQueryString(authKeyUrlParam + "=" + authKey);
        request.addParameter(authKeyUrlParam, authKey);
        // Force to reload the property file
        mapper.synchronize();

        SecurityContextHolder.clearContext();
        getSecurityManager().getAuthenticationCache().removeAll();

        getProxy().doFilter(request, response, chain);
        assertNotEquals(MockHttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
        assertNull(request.getSession(false));

        // check unknown user
        username = "unknown";
        password = username;
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.setQueryString(authKeyUrlParam + "=abc");
        request.addParameter(authKeyUrlParam, "abc");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testUserPropertyWithCacheDisabled() throws Exception {

        String authKeyUrlParam = "myAuthKey";
        String filterName = "testAuthKeyFilter2Disabled";

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("userPropertyMapper");

        // Let's make sure the internal user cache is disabled
        Map<String, String> mapperParams = new HashMap<>();
        mapperParams.put("cacheTtlSeconds", "0");
        config.setMapperParameters(mapperParams);
        getSecurityManager().saveFilter(config);

        GeoServerAuthenticationKeyFilter filter =
                (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);

        UserPropertyAuthenticationKeyMapper mapper = (UserPropertyAuthenticationKeyMapper) filter.getMapper();
        // Force to reload the property file
        mapper.synchronize();

        prepareFilterChain(pattern, filterName);
        modifyChain(pattern, false, false, null);

        SecurityContextHolder.getContext().setAuthentication(null);
        getSecurityManager().getAuthenticationCache().removeAll();

        // check disabled user
        username = testUserName;
        password = username;
        updateUser("ug1", username, false);

        // Force to reload the property file
        mapper.synchronize();
        // Make sure the cache is cleared
        mapper.resetUserCache();

        // test success
        GeoServerUser user =
                (GeoServerUser) getSecurityManager().loadUserGroupService("ug1").loadUserByUsername(testUserName);

        String authKey = user.getProperties().getProperty(mapper.getUserPropertyName());
        assertNotNull(authKey);

        SecurityContextHolder.clearContext();
        getSecurityManager().getAuthenticationCache().removeAll();

        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        request.setQueryString(authKeyUrlParam + "=" + authKey);
        request.addParameter(authKeyUrlParam, authKey);
        getProxy().doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(getSecurityManager().getAuthenticationCache().get(filterName, authKey));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        updateUser("ug1", username, true);

        insertAnonymousFilter();
        request = createRequest("foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    @Test
    public void testWebServiceAuthKeyMapper() throws Exception {
        GeoServerUserGroupService ugservice = createUserGroupService("testWebServiceAuthKey");
        GeoServerUserGroupStore ugstore = ugservice.createStore();
        GeoServerUser u1 = ugstore.createUserObject("user1", "passwd1", true);
        ugstore.addUser(u1);
        GeoServerUser u2 = ugstore.createUserObject("user2", "passwd2", true);
        ugstore.addUser(u2);
        ugstore.store();

        WebServiceAuthenticationKeyMapper propMapper = GeoServerExtensions.extensions(
                        WebServiceAuthenticationKeyMapper.class)
                .iterator()
                .next();
        propMapper.setUserGroupServiceName("testWebServiceAuthKey");
        propMapper.setSecurityManager(getSecurityManager());
        propMapper.setWebServiceUrl("http://service/{key}");
        propMapper.setHttpClient(new TestHttpClient("testkey", "user1"));
        GeoServerUser user = propMapper.getUser("testkey");
        assertNotNull(user);
        assertEquals("user1", user.getUsername());
        boolean error = false;
        try {
            user = propMapper.getUser("wrongkey");
        } catch (UsernameNotFoundException e) {
            error = true;
        }
        assertTrue(error);
    }

    @Test
    public void testWebServiceAuthKeyMapperSearchUser() throws Exception {
        GeoServerUserGroupService ugservice = createUserGroupService("testWebServiceAuthKey2");
        GeoServerUserGroupStore ugstore = ugservice.createStore();
        GeoServerUser u1 = ugstore.createUserObject("user1", "passwd1", true);
        ugstore.addUser(u1);
        GeoServerUser u2 = ugstore.createUserObject("user2", "passwd2", true);
        ugstore.addUser(u2);
        ugstore.store();

        WebServiceAuthenticationKeyMapper propMapper = GeoServerExtensions.extensions(
                        WebServiceAuthenticationKeyMapper.class)
                .iterator()
                .next();
        propMapper.setUserGroupServiceName("testWebServiceAuthKey2");
        propMapper.setSecurityManager(getSecurityManager());
        propMapper.setWebServiceUrl("http://service/{key}");
        propMapper.setSearchUser("^.*?\"user\"\\s*:\\s*\"([^\"]+)\".*$");
        propMapper.setHttpClient(
                new TestHttpClient("testkey", "{\n    \"user\": \"user1\", \"detail\": \"mydetail\"\n   }"));
        GeoServerUser user = propMapper.getUser("testkey");
        assertNotNull(user);
        assertEquals("user1", user.getUsername());

        propMapper.setSearchUser("^.*?<username>(.*?)</username>.*$");
        propMapper.setHttpClient(new TestHttpClient(
                "testkey", "<root>\n<userdetail>\n<username>user1</username>\n</userdetail>\n</root>"));
        user = propMapper.getUser("testkey");
        assertNotNull(user);
        assertEquals("user1", user.getUsername());

        user = propMapper.getUser("wrongkey");
        assertNull(user);
    }

    @Test
    public void testWebServiceAuthKeyBodyResponseUGS() throws Exception {
        WebServiceBodyResponseUserGroupServiceConfig config = new WebServiceBodyResponseUserGroupServiceConfig();
        config.setName("testWebServiceAuthKey3");
        config.setClassName(WebServiceBodyResponseUserGroupService.class.getName());
        config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        config.setSearchRoles("^.*?\"roles\"\\s*:\\s*\"([^\"]+)\".*$");
        config.setAvailableGroups("GROUP_MYROLE_1, GROUP_MYROLE_2");

        getSecurityManager().saveUserGroupService(config /*,isNewUGService(name)*/);
        GeoServerUserGroupService webServiceAuthKeyBodyResponseUGS =
                getSecurityManager().loadUserGroupService("testWebServiceAuthKey3");

        assertNotNull(webServiceAuthKeyBodyResponseUGS);

        WebServiceAuthenticationKeyMapper propMapper = GeoServerExtensions.extensions(
                        WebServiceAuthenticationKeyMapper.class)
                .iterator()
                .next();
        propMapper.setUserGroupServiceName("testWebServiceAuthKey3");
        propMapper.setSecurityManager(getSecurityManager());
        propMapper.setWebServiceUrl("http://service/{key}");
        propMapper.setSearchUser("^.*?\"user\"\\s*:\\s*\"([^\"]+)\".*$");
        propMapper.setHttpClient(new TestHttpClient(
                "testkey",
                "{\n    \"user\": \"user1\", \"detail\": \"mydetail\", \"roles\": \"myrole_1, myrole_2\"\n   }"));
        GeoServerUser user = propMapper.getUser("testkey");
        assertNotNull(user);
        assertEquals("user1", user.getUsername());

        assertNotNull(user.getAuthorities());
        assertEquals(2, user.getAuthorities().size());
        assertTrue(user.getAuthorities().contains(new GeoServerRole("ROLE_MYROLE_1")));
        assertTrue(user.getAuthorities().contains(new GeoServerRole("ROLE_MYROLE_2")));

        assertEquals(2, webServiceAuthKeyBodyResponseUGS.getGroupCount());
        assertEquals(2, webServiceAuthKeyBodyResponseUGS.getUserGroups().size());
        assertEquals(
                webServiceAuthKeyBodyResponseUGS.getUserGroups(),
                webServiceAuthKeyBodyResponseUGS.getGroupsForUser(user));
    }

    @Test
    public void testAuthKeyMapperSynchronize() throws Exception {

        GeoServerUserGroupService ugservice = createUserGroupService("testAuthKey");
        GeoServerUserGroupStore ugstore = ugservice.createStore();
        GeoServerUser u1 = ugstore.createUserObject("user1", "passwd1", true);
        ugstore.addUser(u1);
        GeoServerUser u2 = ugstore.createUserObject("user2", "passwd2", true);
        ugstore.addUser(u2);
        ugstore.store();

        PropertyAuthenticationKeyMapper propMapper = GeoServerExtensions.extensions(
                        PropertyAuthenticationKeyMapper.class)
                .iterator()
                .next();

        UserPropertyAuthenticationKeyMapper userpropMapper = GeoServerExtensions.extensions(
                        UserPropertyAuthenticationKeyMapper.class)
                .iterator()
                .next();

        propMapper.setSecurityManager(getSecurityManager());
        propMapper.setUserGroupServiceName("testAuthKey");

        userpropMapper.setSecurityManager(getSecurityManager());
        userpropMapper.setUserGroupServiceName("testAuthKey");

        // File Property Mapper
        assertEquals(2, propMapper.synchronize());

        File authKeyFile = new File(getSecurityManager().userGroup().dir(), "testAuthKey");
        authKeyFile = new File(authKeyFile, "authkeys.properties");
        assertTrue(authKeyFile.exists());

        Properties props = new Properties();
        loadPropFile(authKeyFile, props);
        assertEquals(2, props.size());

        String user1KeyA = null, user2KeyA = null, user3KeyA = null;

        for (Entry<Object, Object> entry : props.entrySet()) {
            if ("user1".equals(entry.getValue())) user1KeyA = (String) entry.getKey();
            if ("user2".equals(entry.getValue())) user2KeyA = (String) entry.getKey();
        }
        assertNotNull(user1KeyA);
        assertNotNull(user2KeyA);

        assertEquals(u1, propMapper.getUser(user1KeyA));
        assertEquals(u2, propMapper.getUser(user2KeyA));
        assertNull(propMapper.getUser("blblal"));

        // user property mapper
        assertEquals(2, userpropMapper.synchronize());
        u1 = (GeoServerUser) ugservice.loadUserByUsername("user1");
        String user1KeyB = u1.getProperties().getProperty(userpropMapper.getUserPropertyName());
        u2 = (GeoServerUser) ugservice.loadUserByUsername("user2");
        String user2KeyB = u2.getProperties().getProperty(userpropMapper.getUserPropertyName());

        assertEquals(u1, userpropMapper.getUser(user1KeyB));
        assertEquals(u2, userpropMapper.getUser(user2KeyB));
        assertNull(userpropMapper.getUser("blblal"));

        // modify user/group database

        ugstore = ugservice.createStore();
        GeoServerUser u3 = ugstore.createUserObject("user3", "passwd3", true);
        ugstore.addUser(u3);
        ugstore.removeUser(u1);
        ugstore.store();

        assertEquals(1, propMapper.synchronize());

        props = new Properties();
        loadPropFile(authKeyFile, props);
        assertEquals(2, props.size());

        for (Entry<Object, Object> entry : props.entrySet()) {
            if ("user2".equals(entry.getValue())) assertEquals(user2KeyA, entry.getKey());
            if ("user3".equals(entry.getValue())) user3KeyA = (String) entry.getKey();
        }
        assertNotNull(user3KeyA);

        assertNull(propMapper.getUser(user1KeyA));
        assertEquals(u2, propMapper.getUser(user2KeyA));
        assertEquals(u3, propMapper.getUser(user3KeyA));

        // user property mapper
        assertEquals(1, userpropMapper.synchronize());
        u2 = (GeoServerUser) ugservice.loadUserByUsername("user2");
        assertEquals(user2KeyB, u2.getProperties().getProperty(userpropMapper.getUserPropertyName()));
        u3 = (GeoServerUser) ugservice.loadUserByUsername("user3");
        String user3KeyB = u3.getProperties().getProperty(userpropMapper.getUserPropertyName());

        assertNull(userpropMapper.getUser(user1KeyB));
        assertEquals(u2, userpropMapper.getUser(user2KeyB));
        assertEquals(u3, userpropMapper.getUser(user3KeyB));

        // test disabled user

        ugstore = ugservice.createStore();
        u2 = (GeoServerUser) ugstore.loadUserByUsername("user2");
        u2.setEnabled(false);
        ugstore.updateUser(u2);
        ugstore.store();

        // Force cache expiration
        propMapper.resetUserCache();
        userpropMapper.resetUserCache();

        // Ensure the users are not present anymore
        assertNull(propMapper.getUser(user2KeyA));
        assertNull(userpropMapper.getUser(user2KeyB));
    }

    @Test
    public void testAuthKeyMapperAutoSynchronize() throws Exception {
        String authKeyUrlParam = "myAuthKey";
        String filterName = "testAuthKeyFilterAuto1";

        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setAllowMapperKeysAutoSync(true);
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("propertyMapper");

        getSecurityManager().saveFilter(config);

        // File Property Mapper
        File authKeyFile = new File(getSecurityManager().userGroup().dir(), "testAuthKey");
        authKeyFile = new File(authKeyFile, "authkeys.properties");

        GeoServerAuthenticationKeyProvider geoServerAuthenticationKeyProvider =
                new GeoServerAuthenticationKeyProvider(getSecurityManager(), 2);
        assertNotNull(geoServerAuthenticationKeyProvider.getScheduler());
        assertFalse(geoServerAuthenticationKeyProvider.getScheduler().isTerminated());
        assertFalse(geoServerAuthenticationKeyProvider.getScheduler().isShutdown());
        assertEquals(2, geoServerAuthenticationKeyProvider.getAutoSyncDelaySeconds());

        // Wait up to 10 seconds for the executor to be ready for the next reload.
        Properties props = new Properties();
        for (int i = 0; i < 400 && props.isEmpty(); i++) {
            try {
                Thread.sleep(25);
                loadPropFile(authKeyFile, props);
            } catch (InterruptedException e) {
            }
        }
        assertTrue(authKeyFile.exists());
        assertFalse(props.isEmpty());
    }

    @Test
    public void testWebServiceAuthKeyBodyResponseNoRoleMatchingRegex() throws Exception {
        WebServiceBodyResponseUserGroupServiceConfig config = new WebServiceBodyResponseUserGroupServiceConfig();
        config.setName("testWebServiceAuthKey4");
        config.setClassName(WebServiceBodyResponseUserGroupService.class.getName());
        config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        config.setSearchRoles("wrong_regex");
        config.setAvailableGroups("GROUP_MYROLE_1, GROUP_MYROLE_2");

        getSecurityManager().saveUserGroupService(config /*,isNewUGService(name)*/);
        GeoServerUserGroupService webServiceAuthKeyBodyResponseUGS =
                getSecurityManager().loadUserGroupService("testWebServiceAuthKey4");

        assertNotNull(webServiceAuthKeyBodyResponseUGS);

        WebServiceAuthenticationKeyMapper propMapper = GeoServerExtensions.extensions(
                        WebServiceAuthenticationKeyMapper.class)
                .iterator()
                .next();
        propMapper.setUserGroupServiceName("testWebServiceAuthKey4");
        propMapper.setSecurityManager(getSecurityManager());
        propMapper.setWebServiceUrl("http://service/{key}");
        propMapper.setSearchUser("^.*?\"user\"\\s*:\\s*\"([^\"]+)\".*$");
        propMapper.setHttpClient(new TestHttpClient(
                "testkey",
                "{\n    \"user\": \"user1\", \"detail\": \"mydetail\", \"roles\": \"myrole_1, myrole_2\"\n   }"));
        GeoServerUser user = propMapper.getUser("testkey");
        assertNotNull(user);
        assertEquals("user1", user.getUsername());

        assertNotNull(user.getAuthorities());
        assertEquals(1, user.getAuthorities().size());
        assertTrue(user.getAuthorities().contains(new GeoServerRole("ROLE_ANONYMOUS")));
    }

    @Test
    public void testAllowChallengeAnonymousSessionsBehavior() throws Exception {
        String authKeyUrlParam = "myAuthKey";
        String filterName = "testAllowChallengeAnonymousSessionsBehavior";

        // Configure the filter
        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());
        config.setName(filterName);
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("userPropertyMapper");

        // Save initial filter configuration with allowChallengeAnonymousSessions = true
        config.setAllowChallengeAnonymousSessions(true);

        // Let's make sure the internal user cache is disabled
        Map<String, String> mapperParams = new HashMap<>();
        mapperParams.put("cacheTtlSeconds", "0");
        config.setMapperParameters(mapperParams);
        getSecurityManager().saveFilter(config);

        GeoServerAuthenticationKeyFilter filter =
                (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);

        // Configure the mapper to return a user for a specific auth key
        UserPropertyAuthenticationKeyMapper mapper = (UserPropertyAuthenticationKeyMapper) filter.getMapper();
        mapper.synchronize();

        // Prepare the filter chain
        prepareFilterChain(pattern, filterName);
        modifyChain(pattern, false, false, null);

        // test success
        GeoServerUser user =
                (GeoServerUser) getSecurityManager().loadUserGroupService("ug1").loadUserByUsername(testUserName);
        // Make sure the cache is cleared
        mapper.resetUserCache();
        String authKey = user.getProperties().getProperty(mapper.getUserPropertyName());
        assertNotNull(authKey);

        // Test when allowChallengeAnonymousSessions = true (always use authKey for authentication)
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setQueryString(authKeyUrlParam + "=" + authKey);
        request.addParameter(authKeyUrlParam, authKey);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Set a valid existing authentication in the SecurityContext
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken("test", "anonymous", authorities));

        // Perform the request
        getProxy().doFilter(request, response, chain);

        // Verify that the filter ignored the existing authentication and retrieved the user using
        // authKey
        Authentication resultAuth =
                getSecurityManager().getAuthenticationCache().get(filterName, authKey);
        resultAuth = resultAuth != null
                ? resultAuth
                : SecurityContextHolder.getContext().getAuthentication();
        if (resultAuth != null) {
            assertEquals("user1", resultAuth.getPrincipal()); // Assuming "user1" is returned by the mapper

            // Reconfigure the filter with allowChallengeAnonymousSessions = false
            config.setAllowChallengeAnonymousSessions(false);
            getSecurityManager().saveFilter(config);

            filter = (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);

            // Simulate another request
            request = createRequest("/foo/bar");
            request.setQueryString(authKeyUrlParam + "=" + authKey);
            request.addParameter(authKeyUrlParam, authKey);
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();

            // Set an existing valid authentication in the SecurityContext
            getSecurityManager().getAuthenticationCache().removeAll();
            SecurityContextHolder.getContext()
                    .setAuthentication(new AnonymousAuthenticationToken("test", "validUser", authorities));

            // Perform the request
            getProxy().doFilter(request, response, chain);

            // Verify that the filter used the existing authentication and did not retrieve the user
            // using authKey
            resultAuth = getSecurityManager().getAuthenticationCache().get(filterName, authKey);
            resultAuth = resultAuth != null
                    ? resultAuth
                    : SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(resultAuth);
            assertEquals("user1", resultAuth.getPrincipal());
        }
    }

    private void loadPropFile(File authKeyFile, Properties props) throws FileNotFoundException, IOException {
        try (FileInputStream propFile = new FileInputStream(authKeyFile)) {
            props.load(propFile);
        }
    }

    @Override
    protected GeoServerSecurityManager getSecurityManager() {
        return getProxy().securityManager;
    }
}

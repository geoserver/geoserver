/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.impl.GeoServerUser;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class AuthKeyAuthenticationTest extends AbstractAuthenticationProviderTest {

    
   
    @Override
    protected void onSetUp(org.geoserver.data.test.SystemTestData testData) throws Exception {
        super.onSetUp(testData);

    }

    
    @Test
    public void testFileBasedWithSession() throws Exception {

        String authKeyUrlParam = "myAuthKey";
        String filterName = "testAuthKeyFilter1";
        
        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();         
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());                
        config.setName(filterName);        
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("propertyMapper");
                 
        getSecurityManager().saveFilter(config);
        
        GeoServerAuthenticationKeyFilter filter = 
                (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);
        
        PropertyAuthenticationKeyMapper mapper = 
                (PropertyAuthenticationKeyMapper) filter.getMapper(); 
        mapper.synchronize();
                
        

        prepareFilterChain(pattern,filterName);
        modifyChain(pattern, false, true, null);
        
        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getErrorCode());

        // test success
        String authKey=null;
        for (Entry<Object,Object> entry : mapper.authKeyProps.entrySet()) {
            if (testUserName.equals(entry.getValue())) {
                authKey=(String)entry.getKey();
                break;
            }
        }
        
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();        
        request.setQueryString(authKeyUrlParam+"=" + authKey);        
        request.setupAddParameter(authKeyUrlParam, authKey);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertFalse(response.wasRedirectSent());


        SecurityContext ctx = (SecurityContext) request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
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

        request.setQueryString(authKeyUrlParam+"=abc");        
        request.setupAddParameter(authKeyUrlParam, "abc");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getErrorCode());
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        

        // check disabled user
        username = testUserName;
        password = username;
        updateUser("ug1", username, false);
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        
        request.setQueryString(authKeyUrlParam+"=" + authKey);        
        request.setupAddParameter(authKeyUrlParam, authKey);
        getProxy().doFilter(request, response, chain);
        
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getErrorCode());
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        updateUser("ug1", username, true);

        insertAnonymousFilter();
        request = createRequest("foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();



    }
    
    @Test
    public void testUserPropertyWithCache() throws Exception {

        String authKeyUrlParam = "myAuthKey";
        String filterName = "testAuthKeyFilter2";
        
        AuthenticationKeyFilterConfig config = new AuthenticationKeyFilterConfig();         
        config.setClassName(GeoServerAuthenticationKeyFilter.class.getName());                
        config.setName(filterName);        
        config.setUserGroupServiceName("ug1");
        config.setAuthKeyParamName(authKeyUrlParam);
        config.setAuthKeyMapperName("userPropertyMapper");
                 
        getSecurityManager().saveFilter(config);
        
        GeoServerAuthenticationKeyFilter filter = 
                (GeoServerAuthenticationKeyFilter) getSecurityManager().loadFilter(filterName);
        
        UserPropertyAuthenticationKeyMapper mapper = 
                (UserPropertyAuthenticationKeyMapper) filter.getMapper(); 
        mapper.synchronize();
                
        

        prepareFilterChain(pattern,filterName);
        modifyChain(pattern, false, false, null);
        
        SecurityContextHolder.getContext().setAuthentication(null);

        // Test entry point
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getErrorCode());

        // test success
        GeoServerUser user= (GeoServerUser) getSecurityManager().loadUserGroupService("ug1").loadUserByUsername(testUserName);
        String authKey=user.getProperties().getProperty(mapper.getUserPropertyName());
        assertNotNull(authKey);
        
                
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();        
        request.setQueryString(authKeyUrlParam+"=" + authKey);        
        request.setupAddParameter(authKeyUrlParam, authKey);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertFalse(response.wasRedirectSent());

        Authentication auth = (Authentication) getCache().get(filterName,authKey);
        assertNotNull(auth);
        assertNull(request.getSession(false));
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, auth.getPrincipal());

        
        // check unknown user
        username = "unknown";
        password = username;
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.setQueryString(authKeyUrlParam+"=abc");        
        request.setupAddParameter(authKeyUrlParam, "abc");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getErrorCode());
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        
        getCache().removeAll();
        
        // check disabled user
        username = testUserName;
        password = username;
        updateUser("ug1", username, false);
        request = createRequest("/foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        
        request.setQueryString(authKeyUrlParam+"=" + authKey);        
        request.setupAddParameter(authKeyUrlParam, authKey);
        getProxy().doFilter(request, response, chain);
        
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getErrorCode());
        assertNull(getCache().get(filterName, authKey));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        updateUser("ug1", username, true);

        insertAnonymousFilter();
        request = createRequest("foo/bar");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();

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
        
        PropertyAuthenticationKeyMapper propMapper =
                GeoServerExtensions.extensions(PropertyAuthenticationKeyMapper.class).iterator().next();

        UserPropertyAuthenticationKeyMapper userpropMapper =
                GeoServerExtensions.extensions(UserPropertyAuthenticationKeyMapper.class).iterator().next();
        
        propMapper.setSecurityManager(getSecurityManager());
        propMapper.setUserGroupServiceName("testAuthKey");
        
        userpropMapper.setSecurityManager(getSecurityManager());
        userpropMapper.setUserGroupServiceName("testAuthKey");

        // File Property Mapper
        assertEquals(2,propMapper.synchronize());
                
        File authKeyFile = new File(getSecurityManager().getUserGroupRoot(),"testAuthKey");
        authKeyFile=new File(authKeyFile,"authkeys.properties");
        assertTrue(authKeyFile.exists());
        
        Properties props = new Properties();
        props.load(new FileInputStream(authKeyFile));
        assertEquals(2, props.size());
        
        String user1KeyA=null,user2KeyA=null,user3KeyA=null,
                user1KeyB=null,user2KeyB=null,user3KeyB=null;
        
        for (Entry<Object, Object> entry : props.entrySet()) {
            if ("user1".equals(entry.getValue()))
                user1KeyA=(String) entry.getKey();
            if ("user2".equals(entry.getValue()))
                user2KeyA=(String) entry.getKey();
        }
        assertNotNull(user1KeyA);
        assertNotNull(user2KeyA);
        
        assertEquals(u1, propMapper.getUser(user1KeyA));
        assertEquals(u2, propMapper.getUser(user2KeyA));
        assertNull(propMapper.getUser("blblal"));
        
        
        
        // user property mapper
        assertEquals(2,userpropMapper.synchronize());
        u1 = (GeoServerUser) ugservice.loadUserByUsername("user1");
        user1KeyB=u1.getProperties().getProperty(userpropMapper.getUserPropertyName());
        u2 = (GeoServerUser) ugservice.loadUserByUsername("user2");
        user2KeyB=u2.getProperties().getProperty(userpropMapper.getUserPropertyName());
        
        assertEquals(u1, userpropMapper.getUser(user1KeyB));
        assertEquals(u2, userpropMapper.getUser(user2KeyB));
        assertNull(userpropMapper.getUser("blblal"));

        // modify user/group database
        
        ugstore = ugservice.createStore();
        GeoServerUser u3 = ugstore.createUserObject("user3", "passwd3", true);
        ugstore.addUser(u3);
        ugstore.removeUser(u1);        
        ugstore.store();
        
        assertEquals(1,propMapper.synchronize());
        
        
        props = new Properties();
        props.load(new FileInputStream(authKeyFile));
        assertEquals(2, props.size());
        
        
        for (Entry<Object, Object> entry : props.entrySet()) {
            if ("user2".equals(entry.getValue()))
                assertEquals(user2KeyA,(String) entry.getKey());
            if ("user3".equals(entry.getValue()))
                user3KeyA=(String) entry.getKey();
        }
        assertNotNull(user3KeyA);
        
        
        assertNull(propMapper.getUser(user1KeyA));
        assertEquals(u2, propMapper.getUser(user2KeyA));
        assertEquals(u3,propMapper.getUser(user3KeyA));
        
        
        
        // user property mapper
        assertEquals(1,userpropMapper.synchronize());
        u2 = (GeoServerUser) ugservice.loadUserByUsername("user2");
        assertEquals(user2KeyB,u2.getProperties().getProperty(userpropMapper.getUserPropertyName()));
        u3 = (GeoServerUser) ugservice.loadUserByUsername("user3");
        user3KeyB=u3.getProperties().getProperty(userpropMapper.getUserPropertyName());
        
        assertNull( userpropMapper.getUser(user1KeyB));
        assertEquals(u2, userpropMapper.getUser(user2KeyB));
        assertEquals(u3, userpropMapper.getUser(user3KeyB));
        
        // test disabled user
        
        ugstore = ugservice.createStore();
        u2 = (GeoServerUser) ugstore.loadUserByUsername("user2");
        u2.setEnabled(false);
        ugstore.updateUser(u2);
        ugstore.store();

        assertNull( propMapper.getUser(user2KeyA));
        assertNull( userpropMapper.getUser(user2KeyB));
        
        

    }

  

}

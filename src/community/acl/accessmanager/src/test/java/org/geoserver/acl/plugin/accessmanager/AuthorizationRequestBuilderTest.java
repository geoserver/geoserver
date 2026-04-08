/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Set;
import org.geoserver.acl.authorization.AccessRequest;
import org.geoserver.ows.Request;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AuthorizationRequestBuilderTest {

    @Test
    public void testIPv6Address() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("[0:0:0:0:0:0:0:1]");
        String sourceAddress = AuthorizationRequestBuilder.getSourceAddress(request);
        // assert that address does not contain any brackets
        assertFalse(sourceAddress.matches("\\[(.*)\\]"));
    }

    @Test
    public void testBuildFromAuthentication() {
        AccessRequest request = AuthorizationRequestBuilder.data()
                .user(getAuthentication())
                .request(getRequest())
                .layer("states")
                .workspace("topp")
                .build();

        assertEquals("testuser", request.getUser());
        assertEquals(Set.of("ROLE_ONE", "ROLE_TWO"), request.getRoles());
        assertEquals(getAuthentication().getName(), request.getUser());
        assertEquals("WMS", request.getService());
        assertEquals("GETMAP", request.getRequest());
        assertEquals("topp", request.getWorkspace());
        assertEquals("states", request.getLayer());
        assertNull(request.getSourceAddress());
        assertNull(request.getSubfield());
    }

    @Test
    public void testDefaults() {
        AccessRequest request = AuthorizationRequestBuilder.data()
                .user(getAuthentication())
                .request(getRequest())
                .build();

        assertEquals(Set.of("ROLE_ONE", "ROLE_TWO"), request.getRoles());
        assertEquals(getAuthentication().getName(), request.getUser());
        assertEquals("WMS", request.getService());
        assertEquals("GETMAP", request.getRequest());
        // no value provided should be set to default null
        assertNull(request.getLayer());
        assertNull(request.getWorkspace());
    }

    private Request getRequest() {
        Request request = new Request();
        request.setService("WMS");
        request.setRequest("GETMAP");
        return request;
    }

    private Authentication getAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ONE"), new SimpleGrantedAuthority("ROLE_TWO")));
        return authentication;
    }
}

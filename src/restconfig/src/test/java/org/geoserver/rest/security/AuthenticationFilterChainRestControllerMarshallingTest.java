/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.logging.Level;
import junit.framework.TestCase;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthenticationFilterChainRestControllerMarshallingTest extends GeoServerSystemTestSupport {

    private static final String BASEPATH = RestBaseController.ROOT_PATH;

    @Test
    public void testList_XML() {
        setUser();
        try {
            getAsDOM(BASEPATH + "/security/filterChains.xml", 200);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testList_NotAuthorised() {
        try {
            getAsDOM(BASEPATH + "/security/filterChains.xml", 403);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testList_JSON() {
        setUser();
        try {
            getAsJSON(BASEPATH + "/security/filterChains.json", 200);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testView_XML() {
        setUser();
        try {
            getAsDOM(BASEPATH + "/security/filterChains/web.xml", 200);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testView_JSON() {
        setUser();
        try {
            getAsJSON(BASEPATH + "/security/filterChains/web", 200);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testPost() {
        setUser();
        try {
            String json = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web");
            deleteAsServletResponse(RestBaseController.ROOT_PATH + "/security/filterChains/web");
            MockHttpServletResponse response =
                    postAsServletResponse(BASEPATH + "/security/filterChains", json, "application/json");
            TestCase.assertEquals(201, response.getStatus());
            assertEquals("text/plain", response.getContentType());
            String location = response.getHeader("Location");
            assertTrue(location.endsWith("/security/filterChains/web"));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testPut_JSON() {
        setUser();
        try {
            String json = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web");
            MockHttpServletResponse response =
                    putAsServletResponse(BASEPATH + "/security/filterChains/web", json, "application/json");
            TestCase.assertEquals(200, response.getStatus());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void setUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

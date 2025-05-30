package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;

import java.util.logging.Level;
import junit.framework.TestCase;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class AuthenticationFilterChainRestControllerMarshallingTest extends GeoServerSystemTestSupport {

    private static String BASEPATH = RestBaseController.ROOT_PATH;

    @Test
    public void testList_XML() {
        try {
            getAsDOM(BASEPATH + "/security/filterChains.xml", 200);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testList_JSON() {
        try {
            getAsJSON(BASEPATH + "/security/filterChains.json", 200);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testView_XML() {
        try {
            getAsDOM(BASEPATH + "/security/filterChains/web.xml", 200);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testView_JSON() {
        try {
            getAsJSON(BASEPATH + "/security/filterChains/web.json", 200);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testPost_JSON() {
        try {
            String json = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web.json");
            deleteAsServletResponse(RestBaseController.ROOT_PATH + "/security/filterChains/web");
            MockHttpServletResponse response =
                    postAsServletResponse(BASEPATH + "/security/filterChains.json", json, "application/json");
            TestCase.assertEquals(201, response.getStatus());
            assertEquals("application/json", response.getContentType());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testPut_JSON() {
        try {
            String json = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web");
            MockHttpServletResponse response =
                    putAsServletResponse(BASEPATH + "/security/filterChains/web", json, "application/json");
            TestCase.assertEquals(200, response.getStatus());
            assertEquals("application/json", response.getContentType());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        }
    }
}

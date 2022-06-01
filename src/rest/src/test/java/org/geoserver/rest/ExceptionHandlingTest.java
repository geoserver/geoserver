/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.junit.Assert.assertEquals;

import java.util.logging.Level;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ExceptionHandlingTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed for this test
    }

    /*
     * Override getAsServletResponse(path) to quiet RestControllerAdvice logger.
     */
    @Override
    protected MockHttpServletResponse getAsServletResponse(String path) throws Exception {
        Level level = RestControllerAdvice.LOGGER.getLevel();
        RestControllerAdvice.LOGGER.setLevel(Level.OFF);
        try {
            return super.getAsServletResponse(path);
        } finally {
            RestControllerAdvice.LOGGER.setLevel(level);
        }
    }

    @Test
    public void testRestException() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH + "/exception?code=400&message=error");
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String txt = response.getContentAsString();
        assertEquals("error", txt);
    }

    @Test
    public void testInternalError() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/error");
        assertEquals(500, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String txt = response.getContentAsString();
        assertEquals("An internal error occurred", txt);
    }

    @Test
    public void testNotFound() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/notfound");
        assertEquals(404, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String txt = response.getContentAsString();
        assertEquals("I'm not there", txt);
    }

    @Test
    public void testNullPointerException() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/npe");
        assertEquals(500, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String txt = response.getContentAsString();
        assertEquals("", txt);
    }
}

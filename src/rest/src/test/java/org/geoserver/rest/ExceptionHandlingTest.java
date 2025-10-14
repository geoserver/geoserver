/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ExceptionHandlingTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed for this test
    }

    private Level level;

    @Before
    public void disableRestControllerAdviceLogging() {
        level = RestControllerAdvice.LOGGER.getLevel();
        RestControllerAdvice.LOGGER.setLevel(Level.OFF);
    }

    @After
    public void enableRestControllerAdviceLogging() {
        RestControllerAdvice.LOGGER.setLevel(level);
    }

    @After
    public void resetQuietOnNotFountConfig() {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().getMetadata().remove(RESTUtils.QUIET_ON_NOT_FOUND_KEY);
        getGeoServer().save(global);
    }

    @Test
    public void testRestException() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/exception?code=400&message=error");
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String txt = response.getContentAsString();
        assertEquals("error", txt);
    }

    @Test
    public void testInternalError() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(ROOT_PATH + "/error");
        assertEquals(500, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String txt = response.getContentAsString();
        assertEquals("An internal error occurred", txt);
    }

    @Test
    public void testNotFound() throws Exception {
        MockHttpServletResponse response = assertNotFound(ROOT_PATH + "/notfound");
        assertEquals("text/plain", response.getContentType());
        String txt = response.getContentAsString();
        assertEquals("I'm not there", txt);
    }

    @Test
    public void testQuietOnNotFoundLogging_default() throws Exception {
        String path = ROOT_PATH + "/notfound";
        List<String> loggingMessages = captureNotFoundLogs(path);
        assertThat(loggingMessages, is(not(empty())));
    }

    @Test
    public void testQuietOnNotFoundLogging_queryParam() throws Exception {
        String path = ROOT_PATH + "/notfound?quietOnNotFound=true";
        List<String> loggingMessages = captureNotFoundLogs(path);
        assertThat(loggingMessages, is(empty()));

        path = ROOT_PATH + "/notfound?quietOnNotFound=false";
        loggingMessages = captureNotFoundLogs(path);
        assertThat(loggingMessages, is(not(empty())));
    }

    @Test
    public void testQuietOnNotFoundLogging_globalSettings() throws Exception {
        GeoServer geoserver = super.getGeoServer();

        final String path = ROOT_PATH + "/notfound";
        List<String> loggingMessages;

        loggingMessages = captureNotFoundLogs(path);
        assertThat(loggingMessages, is(not(empty())));

        GeoServerInfo global;
        global = geoserver.getGlobal();
        global.getSettings().getMetadata().put(RESTUtils.QUIET_ON_NOT_FOUND_KEY, false);
        geoserver.save(global);

        loggingMessages = captureNotFoundLogs(path);
        assertThat(loggingMessages, is(not(empty())));

        global = geoserver.getGlobal();
        global.getSettings().getMetadata().put(RESTUtils.QUIET_ON_NOT_FOUND_KEY, true);
        geoserver.save(global);
        loggingMessages = captureNotFoundLogs(path);
        assertThat(loggingMessages, is(empty()));
    }

    private List<String> captureNotFoundLogs(String path) throws Exception {
        Logger logger = RestControllerAdvice.LOGGER;
        logger.setLevel(Level.SEVERE);
        LoggingCapturer capturer = new LoggingCapturer(RestControllerAdvice.LOGGER).start();
        try {
            assertNotFound(path);
        } finally {
            capturer.stop();
        }
        return capturer.getMessages();
    }

    private MockHttpServletResponse assertNotFound(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(404, response.getStatus());
        return response;
    }

    @Test
    public void testNullPointerException() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(ROOT_PATH + "/npe");
        assertEquals(500, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String txt = response.getContentAsString();
        assertEquals("", txt);
    }
}

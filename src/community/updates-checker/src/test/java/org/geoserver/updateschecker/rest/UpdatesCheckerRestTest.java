/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.Filter;
import java.util.Collections;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.updateschecker.TestFeedServer;
import org.geoserver.updateschecker.TestFeeds;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The REST endpoint as a client reaches it: through the dispatcher and the Spring Security filters. Which roles may
 * call {@code /rest} is the standard {@code rest.properties} rule set, and the test data directory opens it to
 * everyone, so that is not asserted here.
 */
public class UpdatesCheckerRestTest extends GeoServerSystemTestSupport {

    private static final String PATH = "rest/updates-checker/check";

    private static TestFeedServer feedServer;

    /** Runs the requests through the Spring Security filter chain, which the test support skips by default. */
    @Override
    protected List<Filter> getFilters() {
        return Collections.singletonList((Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @BeforeClass
    public static void startFeedServer() throws Exception {
        System.setProperty(UpdatesCheckerService.FORCE_VERSION_PROPERTY, "2.28.3");
        feedServer = TestFeedServer.startForApplication(TestFeeds.feed(
                "",
                TestFeeds.series("3.0.x", "stable", "3.0.1", false),
                TestFeeds.series("2.28.x", "maintenance", "2.28.5", false)));
    }

    @AfterClass
    public static void stopFeedServer() {
        feedServer.stop();
        System.clearProperty(UpdatesCheckerService.FORCE_VERSION_PROPERTY);
    }

    @Test
    public void anAdministratorGetsTheCheckResultAsJson() throws Exception {
        setRequestAuth("admin", "geoserver");
        MockHttpServletResponse response = postAsServletResponse(PATH, "");
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType(), response.getContentType().startsWith("application/json"));
        JSONObject json = new JSONObject(response.getContentAsString());
        assertEquals("UPDATE_AVAILABLE", json.getString("status"));
        assertEquals("2.28.3", json.getString("installedVersion"));
        assertEquals("2.28.5", json.getString("newerVersion"));
        assertEquals(
                "PATCH_AVAILABLE",
                json.getJSONArray("notifications").getJSONObject(0).getString("type"));
    }

    @Test
    public void wrongCredentialsAreRefused() throws Exception {
        setRequestAuth("admin", "not-the-password");
        assertEquals(401, postAsServletResponse(PATH, "").getStatus());
    }

    @Test
    public void onlyPostIsAccepted() throws Exception {
        setRequestAuth("admin", "geoserver");
        assertEquals(405, getAsServletResponse(PATH).getStatus());
    }
}

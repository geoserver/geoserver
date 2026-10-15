/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.updateschecker.TestFeedServer;
import org.geoserver.updateschecker.TestFeeds;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.geoserver.updateschecker.UpdatesCheckerSuppression;
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

    private static final String FEED = TestFeeds.feed(
            "",
            TestFeeds.series("3.0.x", "stable", "3.0.1", false),
            TestFeeds.series("2.28.x", "maintenance", "2.28.5", false));

    private static TestFeedServer feedServer;

    /** Runs the requests through the Spring Security filter chain, which the test support skips by default. */
    @Override
    protected List<Filter> getFilters() {
        return Collections.singletonList((Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @BeforeClass
    public static void startFeedServer() throws Exception {
        System.setProperty(UpdatesCheckerService.FORCE_VERSION_PROPERTY, "2.28.3");
        feedServer = TestFeedServer.startForApplication(FEED);
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
    // ---- suppressed advisories ----------------------------------------------------------------------------------

    private static final String ADVISORY_FEED = "{\"schema_version\": 1, \"series\": ["
            + "{\"series\": \"2.28.x\", \"phase\": \"maintenance\", \"latest_version\": \"2.28.5\"}],"
            + "\"advisories\": [{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\", \"cve_id\": \"CVE-2026-0001\","
            + "\"severity\": \"critical\", \"summary\": \"Affects 2.28.3\","
            + "\"vulnerable_versions\": [\"< 2.28.4\"], \"patched_versions\": {\"2.28.x\": \"2.28.4\"}}]}";

    private static final String SUPPRESS_ALL = "{\"suppressedCves\": [\"CVE-2026-0001\"]}";

    private JSONObject check(String body) throws Exception {
        setRequestAuth("admin", "geoserver");
        MockHttpServletResponse response = postAsServletResponse(PATH, body, "application/json");
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        return new JSONObject(response.getContentAsString());
    }

    private static List<String> noticeTypes(JSONObject json) {
        List<String> types = new ArrayList<>();
        for (int i = 0; i < json.getJSONArray("notifications").length(); i++) {
            types.add(json.getJSONArray("notifications").getJSONObject(i).getString("type"));
        }
        return types;
    }

    @Test
    public void aRequestCanNameTheAdvisoriesToSuppress() throws Exception {
        feedServer.setBody(ADVISORY_FEED);
        try {
            JSONObject all = check("");
            assertTrue(noticeTypes(all).contains("CVE_CONFIRMED"));
            assertEquals(0, all.getJSONArray("suppressed").length());
            assertTrue(all.isNull("suppressedBy"));

            JSONObject json = check(SUPPRESS_ALL);
            assertEquals("UPDATE_AVAILABLE", json.getString("status"));
            assertFalse(noticeTypes(json).contains("CVE_CONFIRMED"));
            assertEquals("CVE-2026-0001", json.getJSONArray("suppressed").getString(0));
            assertEquals("request", json.getString("suppressedBy"));
        } finally {
            feedServer.setBody(FEED);
        }
    }

    @Test
    public void theServersListAppliesUnlessTheRequestSuppliesItsOwn() throws Exception {
        feedServer.setBody(ADVISORY_FEED);
        System.setProperty(UpdatesCheckerSuppression.PROPERTY, "CVE-2026-0001");
        try {
            for (String body : new String[] {"", "{}", "{\"suppressedCves\": null}"}) {
                JSONObject json = check(body);
                assertFalse(body, noticeTypes(json).contains("CVE_CONFIRMED"));
                assertEquals(body, "environment", json.getString("suppressedBy"));
            }
            JSONObject replaced = check("{\"suppressedCves\": []}");
            assertTrue(noticeTypes(replaced).contains("CVE_CONFIRMED"));
            assertTrue(replaced.isNull("suppressedBy"));
        } finally {
            System.clearProperty(UpdatesCheckerSuppression.PROPERTY);
            feedServer.setBody(FEED);
        }
    }

    @Test
    public void aBodyThatIsNotUsableIsRefused() throws Exception {
        setRequestAuth("admin", "geoserver");
        for (String body : new String[] {
            "not json", "[1]", "{\"suppressedCves\": \"CVE-2026-0001\"}", "{\"suppressedCves\": [\"x\"]}"
        }) {
            assertEquals(
                    body,
                    400,
                    postAsServletResponse(PATH, body, "application/json").getStatus());
        }
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.geoserver.updateschecker.TestFeedServer;
import org.geoserver.updateschecker.TestFeeds;
import org.geoserver.updateschecker.TestServices;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CheckResponseTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private TestFeedServer server;

    @Before
    public void startServer() throws Exception {
        server = TestFeedServer.start(feed(""));
    }

    @After
    public void stopServer() {
        server.stop();
    }

    /** A feed with the stable series 3.0.x at 3.0.1 and the given advisories. */
    private static String feed(String advisories) {
        return TestFeeds.feed(advisories, TestFeeds.series("3.0.x", "stable", "3.0.1", false));
    }

    private static String advisory(String severity) {
        return TestFeeds.advisory("GHSA-aaaa-bbbb-cccc", severity, "\"< 3.0.1\"", "\"3.0.x\": \"3.0.1\"");
    }

    private UpdatesCheckerService service(String running) {
        return TestServices.service(TestServices.persister(temp.getRoot()), running);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> notices(Map<String, Object> response) {
        return (List<Map<String, Object>>) response.get("notifications");
    }

    private static Object notice(Map<String, Object> response, int index, String field) {
        return notices(response).get(index).get(field);
    }

    private Map<String, Object> respond(String running) {
        return CheckResponse.of(service(running).check(UpdatesCheckerService.Trigger.MANUAL), Locale.ROOT, null);
    }

    @Test
    public void updateAvailable() {
        Map<String, Object> r = respond("3.0.0");
        assertEquals("UPDATE_AVAILABLE", r.get("status"));
        assertEquals("3.0.0", r.get("installedVersion"));
        assertEquals("3.0.1", r.get("newerVersion"));
        assertNull(r.get("error"));
        assertEquals("You are running GeoServer 3.0.0. GeoServer 3.0.1 is now available.", r.get("message"));
        assertEquals(1, notices(r).size());
        assertEquals("PATCH_AVAILABLE", notice(r, 0, "type"));
        assertEquals("WARN", notice(r, 0, "severity"));
        assertEquals("3.0.1", notice(r, 0, "discriminator"));
    }

    @Test
    public void criticalAndHighAdvisoriesAreUrgent() {
        for (String severity : new String[] {"critical", "high"}) {
            server.setBody(feed(advisory(severity)));
            Map<String, Object> r = respond("3.0.0");
            assertEquals(severity, "URGENT_UPDATE_AVAILABLE", r.get("status"));
            assertEquals("CVE_CONFIRMED", notice(r, 0, "type"));
        }
    }

    @Test
    public void aSecurityReleaseIsUrgentWhetherFlaggedOrOnlyCoordinated() {
        boolean[][] signals = {{true, false}, {false, true}, {true, true}};
        for (boolean[] signal : signals) {
            server.setBody(TestFeeds.feed(
                    "",
                    TestFeeds.seriesWithReleases(
                            "3.0.x", "stable", "3.0.1", TestFeeds.release("3.0.1", signal[0], signal[1], null))));
            Map<String, Object> r = respond("3.0.0");
            assertEquals(
                    "flagged=" + signal[0] + " coordinated=" + signal[1], "URGENT_UPDATE_AVAILABLE", r.get("status"));
            assertEquals("SECURITY_RELEASE", notice(r, 0, "type"));
            assertEquals("3.0.1", r.get("newerVersion"));
        }
    }

    @Test
    public void aReleaseWithNoSignalIsAnOrdinaryUpdate() {
        server.setBody(TestFeeds.feed(
                "",
                TestFeeds.seriesWithReleases(
                        "3.0.x", "stable", "3.0.1", TestFeeds.release("3.0.1", false, false, null))));
        assertEquals("UPDATE_AVAILABLE", respond("3.0.0").get("status"));
    }

    @Test
    public void lowerSeverityAdvisoryIsAnOrdinaryUpdate() {
        server.setBody(feed(advisory("medium")));
        assertEquals("UPDATE_AVAILABLE", respond("3.0.0").get("status"));
    }

    @Test
    public void anAdvisoryWithNoNewerPatchStillNeedsAction() {
        // running the newest patch of the series, but an advisory still applies: not "up to date"
        server.setBody(
                feed(
                        "{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\", \"cve_id\": \"CVE-2026-0001\", \"severity\": \"high\","
                                + " \"summary\": \"Unfixed\", \"vulnerable_versions\": [\"<= 3.0.1\"], \"patched_versions\": {}}"));
        Map<String, Object> r = respond("3.0.1");
        assertEquals("URGENT_UPDATE_AVAILABLE", r.get("status"));
        assertNull(r.get("newerVersion"));
    }

    @Test
    public void upToDate() {
        Map<String, Object> r = respond("3.0.1");
        assertEquals("UP_TO_DATE", r.get("status"));
        assertNull(r.get("newerVersion"));
        assertEquals("GeoServer 3.0.1 is up to date.", r.get("message"));
        assertTrue(notices(r).isEmpty());
    }

    @Test
    public void failedCheck() {
        server.setStatus(500);
        Map<String, Object> r = respond("3.0.0");
        assertEquals("CHECK_FAILED", r.get("status"));
        assertEquals("Version feed returned HTTP 500", r.get("error"));
        assertTrue(r.get("message").toString().contains("Version feed returned HTTP 500"));
    }

    @Test
    public void aFeedWithANewerSchemaIsAFailedCheck() {
        server.setBody("{\"schema_version\": 2, \"series\": []}");
        Map<String, Object> r = respond("3.0.0");
        assertEquals("CHECK_FAILED", r.get("status"));
        assertEquals("SCHEMA_TOO_NEW", notice(r, 0, "type"));
        assertTrue(r.get("error").toString().contains("newer than this plugin understands"));
        assertNull(r.get("newerVersion"));
    }

    @Test
    public void unknownRunningVersion() {
        Map<String, Object> r = respond(null);
        assertEquals("CHECK_FAILED", r.get("status"));
        assertNull(r.get("installedVersion"));
        assertEquals("GeoServer's version could not be determined, so update checks are disabled.", r.get("error"));
        assertEquals("VERSION_UNKNOWN", notice(r, 0, "type"));
    }

    @Test
    public void textFollowsTheRequestedLanguage() {
        Map<String, Object> zz =
                CheckResponse.of(service("3.0.1").check(UpdatesCheckerService.Trigger.MANUAL), new Locale("zz"), null);
        // src/test/resources carries a one-key "zz" translation of the up-to-date text
        assertEquals("zz: GeoServer 3.0.1", zz.get("message"));
    }
}

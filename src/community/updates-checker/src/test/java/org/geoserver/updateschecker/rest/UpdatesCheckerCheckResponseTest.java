/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Locale;
import org.geoserver.updateschecker.TestFeedServer;
import org.geoserver.updateschecker.TestFeeds;
import org.geoserver.updateschecker.TestServices;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

public class UpdatesCheckerCheckResponseTest {

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

    private UpdatesCheckerCheckResponse respond(String running) {
        return UpdatesCheckerCheckResponse.of(
                service(running).check(UpdatesCheckerService.Trigger.MANUAL), Locale.ROOT);
    }

    @Test
    public void updateAvailable() {
        UpdatesCheckerCheckResponse r = respond("3.0.0");
        assertEquals("UPDATE_AVAILABLE", r.getStatus());
        assertEquals("3.0.0", r.getInstalledVersion());
        assertEquals("3.0.1", r.getNewerVersion());
        assertNull(r.getError());
        assertEquals("You are running GeoServer 3.0.0. GeoServer 3.0.1 is now available.", r.getMessage());
        assertEquals(1, r.getNotifications().size());
        assertEquals("PATCH_AVAILABLE", r.getNotifications().get(0).getType());
        assertEquals("WARN", r.getNotifications().get(0).getSeverity());
        assertEquals("3.0.1", r.getNotifications().get(0).getDiscriminator());
    }

    @Test
    public void criticalAndHighAdvisoriesAreUrgent() {
        for (String severity : new String[] {"critical", "high"}) {
            server.setBody(feed(advisory(severity)));
            UpdatesCheckerCheckResponse r = respond("3.0.0");
            assertEquals(severity, "URGENT_UPDATE_AVAILABLE", r.getStatus());
            assertEquals("CVE_CONFIRMED", r.getNotifications().get(0).getType());
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
            UpdatesCheckerCheckResponse r = respond("3.0.0");
            assertEquals(
                    "flagged=" + signal[0] + " coordinated=" + signal[1], "URGENT_UPDATE_AVAILABLE", r.getStatus());
            assertEquals("SECURITY_RELEASE", r.getNotifications().get(0).getType());
            assertEquals("3.0.1", r.getNewerVersion());
        }
    }

    @Test
    public void aReleaseWithNoSignalIsAnOrdinaryUpdate() {
        server.setBody(TestFeeds.feed(
                "",
                TestFeeds.seriesWithReleases(
                        "3.0.x", "stable", "3.0.1", TestFeeds.release("3.0.1", false, false, null))));
        assertEquals("UPDATE_AVAILABLE", respond("3.0.0").getStatus());
    }

    @Test
    public void lowerSeverityAdvisoryIsAnOrdinaryUpdate() {
        server.setBody(feed(advisory("medium")));
        assertEquals("UPDATE_AVAILABLE", respond("3.0.0").getStatus());
    }

    @Test
    public void anAdvisoryWithNoNewerPatchStillNeedsAction() {
        // running the newest patch of the series, but an advisory still applies: not "up to date"
        server.setBody(
                feed(
                        "{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\", \"cve_id\": \"CVE-2026-0001\", \"severity\": \"high\","
                                + " \"summary\": \"Unfixed\", \"vulnerable_versions\": [\"<= 3.0.1\"], \"patched_versions\": {}}"));
        UpdatesCheckerCheckResponse r = respond("3.0.1");
        assertEquals("URGENT_UPDATE_AVAILABLE", r.getStatus());
        assertNull(r.getNewerVersion());
    }

    @Test
    public void upToDate() {
        UpdatesCheckerCheckResponse r = respond("3.0.1");
        assertEquals("UP_TO_DATE", r.getStatus());
        assertNull(r.getNewerVersion());
        assertEquals("GeoServer 3.0.1 is up to date.", r.getMessage());
        assertTrue(r.getNotifications().isEmpty());
    }

    @Test
    public void failedCheck() {
        server.setStatus(500);
        UpdatesCheckerCheckResponse r = respond("3.0.0");
        assertEquals("CHECK_FAILED", r.getStatus());
        assertEquals("Version feed returned HTTP 500", r.getError());
        assertTrue(r.getMessage().contains("Version feed returned HTTP 500"));
    }

    @Test
    public void aFeedWithANewerSchemaIsAFailedCheck() {
        server.setBody("{\"schema_version\": 2, \"series\": []}");
        UpdatesCheckerCheckResponse r = respond("3.0.0");
        assertEquals("CHECK_FAILED", r.getStatus());
        assertEquals("SCHEMA_TOO_NEW", r.getNotifications().get(0).getType());
        assertTrue(r.getError().contains("newer than this plugin understands"));
        assertNull(r.getNewerVersion());
    }

    @Test
    public void unknownRunningVersion() {
        UpdatesCheckerCheckResponse r = respond(null);
        assertEquals("CHECK_FAILED", r.getStatus());
        assertNull(r.getInstalledVersion());
        assertEquals("GeoServer's version could not be determined, so update checks are disabled.", r.getError());
        assertEquals("VERSION_UNKNOWN", r.getNotifications().get(0).getType());
    }

    @Test
    public void textFollowsTheRequestedLanguage() {
        UpdatesCheckerCheckResponse zz = UpdatesCheckerCheckResponse.of(
                service("3.0.1").check(UpdatesCheckerService.Trigger.MANUAL), new Locale("zz"));
        // src/test/resources carries a one-key "zz" translation of the up-to-date text
        assertEquals("zz: GeoServer 3.0.1", zz.getMessage());
    }

    @Test
    public void postPerformsTheCheck() {
        UpdatesCheckerCheckResponse response = new UpdatesCheckerRestController(service("3.0.0")).post();

        assertEquals("a POST is a real check, not a cached read", 1, server.getRequests());
        assertEquals("UPDATE_AVAILABLE", response.getStatus());
        assertEquals("3.0.1", response.getNewerVersion());
    }

    @Test
    public void onlyPostIsMapped() {
        // a check fetches the feed and changes the shared cache, so no GET (or any other method) may reach it
        int mappings = 0;
        for (Method method : UpdatesCheckerRestController.class.getDeclaredMethods()) {
            assertNull(method.getName(), method.getAnnotation(GetMapping.class));
            assertNull(method.getName(), method.getAnnotation(RequestMapping.class));
            assertNull(method.getName(), method.getAnnotation(PutMapping.class));
            assertNull(method.getName(), method.getAnnotation(DeleteMapping.class));
            if (method.getAnnotation(PostMapping.class) != null) {
                mappings++;
            }
        }
        assertEquals(1, mappings);
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import org.geoserver.updateschecker.Notification.Type;
import org.geoserver.updateschecker.UpdatesCheckerService.Trigger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UpdatesCheckerServiceTest {

    private static final Pattern VERSION_NUMBER = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    /** A feed with stable 3.0.1 and maintenance 2.28.5 / 2.27.6, and no advisories. */
    private static final String QUIET_FEED = TestFeeds.feed(
            "",
            "{\"series\": \"3.0.x\", \"phase\": \"stable\", \"latest_version\": \"3.0.1\"}",
            "{\"series\": \"2.28.x\", \"phase\": \"maintenance\", \"latest_version\": \"2.28.5\"}",
            "{\"series\": \"2.27.x\", \"phase\": \"maintenance\", \"latest_version\": \"2.27.6\"}");

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private TestFeedServer feed;
    private LogCapture log;

    @Before
    public void startServer() throws Exception {
        feed = TestFeedServer.start(QUIET_FEED);
        log = LogCapture.start();
    }

    @After
    public void stopServer() {
        feed.stop();
        log.stop();
    }

    private ConfigPersister persister() {
        return TestServices.persister(temp.getRoot());
    }

    private static UpdatesCheckerService service(ConfigPersister persister, String version) {
        return TestServices.service(persister, version);
    }

    private void save(ConfigPersister persister, boolean enabled, boolean log, boolean hide) {
        Config config = persister.getConfig();
        config.setEnabled(enabled);
        config.setLogUpdateChecks(log);
        config.setHideVersionNumbersInLogs(hide);
        persister.save(config);
    }

    private List<LogRecord> resultLines() {
        List<LogRecord> lines = new ArrayList<>();
        for (LogRecord r : log.records()) {
            if (r.getMessage().contains(" update check: ")) {
                lines.add(r);
            }
        }
        return lines;
    }

    // ---- fetching and evaluating --------------------------------------------------------------------------------

    @Test
    public void checkFetchesOnceAndEvaluates() {
        UpdatesCheckerService service = service(persister(), "2.27.0");
        Status status = service.check(Trigger.FIRST_VIEW);

        assertEquals(1, feed.getRequests());
        assertEquals("2.27.0", status.getRunningVersion());
        assertFalse(status.isFetchFailed());
        assertTrue(status.getCheckedAtMillis() > 0);
        assertEquals(2, status.getNotifications().size());
        assertEquals(Type.PATCH_AVAILABLE, status.getNotifications().get(0).getType());
        assertEquals("2.27.6", status.getNewerVersion());
    }

    @Test
    public void getStatusReadsTheCacheAndNeverFetches() {
        UpdatesCheckerService service = service(persister(), "2.27.0");
        Status cold = service.getStatus();
        assertEquals(0, feed.getRequests());
        assertFalse(cold.isAttempted());
        assertTrue(cold.getNotifications().isEmpty());

        service.check(Trigger.FIRST_VIEW);
        Status warm = service.getStatus();
        assertEquals("reading the cache is not a fetch", 1, feed.getRequests());
        assertEquals(2, warm.getNotifications().size());
    }

    @Test
    public void aDisabledModuleDoesNotFetchForItsOwnTriggersButCheckNowStillWorks() {
        ConfigPersister persister = persister();
        save(persister, false, true, false);
        UpdatesCheckerService service = service(persister, "2.27.0");

        service.check(Trigger.STARTUP);
        service.check(Trigger.FIRST_VIEW);
        assertEquals(0, feed.getRequests());
        assertTrue(resultLines().isEmpty());

        Status status = service.check(Trigger.MANUAL);
        assertEquals("Check Now ignores the enabled setting", 1, feed.getRequests());
        assertFalse(status.getNotifications().isEmpty());
    }

    @Test
    public void anUndeterminedVersionNeverFetchesAndSaysSo() {
        UpdatesCheckerService service = service(persister(), null);
        Status status = service.check(Trigger.MANUAL);

        assertEquals(0, feed.getRequests());
        assertEquals(RunningVersion.Kind.UNKNOWN, status.getRunningKind());
        assertNull(status.getRunningVersion());
        assertEquals(1, status.getNotifications().size());
        assertEquals(Type.VERSION_UNKNOWN, status.getNotifications().get(0).getType());
    }

    @Test
    public void anUndeterminedVersionIsWarnedAboutAtStartup() {
        service(persister(), "unknown").check(Trigger.STARTUP);
        boolean warned = false;
        for (LogRecord r : log.records()) {
            warned |= r.getLevel() == Level.WARNING && r.getMessage().contains("could not be determined");
        }
        assertTrue(warned);
        assertEquals(0, feed.getRequests());
    }

    @Test
    public void aForcedVersionReplacesTheRealOne() {
        System.setProperty(UpdatesCheckerService.FORCE_VERSION_PROPERTY, "2.28.3");
        try {
            UpdatesCheckerService service = service(persister(), "3.0.1");
            assertEquals("2.28.3", service.getRunningVersion().getRaw());
            assertEquals("2.28.3", service.check(Trigger.MANUAL).getRunningVersion());
        } finally {
            System.clearProperty(UpdatesCheckerService.FORCE_VERSION_PROPERTY);
        }
    }

    @Test
    public void developmentBuildsHaveNothingToReport() {
        Status status = service(persister(), "3.1.0-SNAPSHOT").check(Trigger.FIRST_VIEW);
        assertEquals(1, feed.getRequests());
        assertTrue(status.getNotifications().isEmpty());
        assertTrue(resultLines().get(0).getMessage().contains("development build"));
    }

    // ---- logging -------------------------------------------------------------------------------------------------

    @Test
    public void oneResultLinePerCheckWithTheTriggerAsLabel() {
        UpdatesCheckerService service = service(persister(), "2.27.0");

        service.check(Trigger.STARTUP);
        service.check(Trigger.FIRST_VIEW);
        service.check(Trigger.MANUAL);

        List<LogRecord> lines = resultLines();
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).getMessage().startsWith("Startup update check: "));
        assertTrue(lines.get(1).getMessage().startsWith("Login update check: "));
        assertTrue(lines.get(2).getMessage().startsWith("Manual update check: "));
    }

    @Test
    public void anAvailableUpdateIsLoggedAtWarnAndUpToDateAtInfo() {
        ConfigPersister persister = persister();
        service(persister, "2.27.0").check(Trigger.MANUAL);
        service(persister, "3.0.1").check(Trigger.MANUAL);

        List<LogRecord> lines = resultLines();
        assertEquals(2, lines.size());
        assertEquals(Level.WARNING, lines.get(0).getLevel());
        assertEquals(
                "Manual update check: UPDATE_AVAILABLE (running 2.27.0, 2.27.6 is available)",
                lines.get(0).getMessage());
        assertEquals(Level.INFO, lines.get(1).getLevel());
        assertEquals(
                "Manual update check: GeoServer 3.0.1 is up to date.",
                lines.get(1).getMessage());
    }

    @Test
    public void resultLinesAreSubjectToLogUpdateChecks() {
        ConfigPersister persister = persister();
        save(persister, true, false, false);
        service(persister, "2.27.0").check(Trigger.MANUAL);
        assertTrue(resultLines().isEmpty());
    }

    @Test
    public void failuresAreAlwaysLoggedAtWarnEvenWithLoggingOff() {
        ConfigPersister persister = persister();
        save(persister, true, false, false);
        feed.setStatus(500);

        Status result = service(persister, "2.27.0").check(Trigger.MANUAL);

        assertTrue(result.isFetchFailed());
        assertEquals("Version feed returned HTTP 500", result.getErrorMessage(Locale.ROOT));
        boolean warned = false;
        for (LogRecord r : log.records()) {
            warned |= r.getLevel() == Level.WARNING && r.getMessage().contains("Could not fetch the version feed");
        }
        assertTrue(warned);
        assertTrue("a failed check writes no result line", resultLines().isEmpty());
    }

    @Test
    public void aFailedCheckStillReportsTheLastGoodFeed() {
        UpdatesCheckerService service = service(persister(), "2.27.0");
        service.check(Trigger.FIRST_VIEW);
        feed.setStatus(503);
        Status failed = service.check(Trigger.MANUAL);

        assertTrue(failed.isFetchFailed());
        assertTrue(failed.getCheckedAtMillis() > 0);
        assertEquals(2, failed.getNotifications().size());
    }

    @Test
    public void aFirstFetchThatFailsRaisesFeedUnavailable() {
        feed.setStatus(500);
        Status result = service(persister(), "2.27.0").check(Trigger.FIRST_VIEW);
        assertEquals(1, result.getNotifications().size());
        assertEquals(Type.FEED_UNAVAILABLE, result.getNotifications().get(0).getType());
    }

    // ---- hiding version numbers in the log -----------------------------------------------------------------------

    @Test
    public void hidingVersionNumbersLeavesNoVersionInAnyResultLine() {
        ConfigPersister persister = persister();
        save(persister, true, true, true);

        // update available, coordinated release, series notice, development build, up to date
        String[] running = {"2.27.0", "2.28.2", "3.0.0", "2.18.3", "3.1.0-SNAPSHOT", "3.0.1"};
        for (String version : running) {
            service(persister, version).check(Trigger.MANUAL);
        }
        List<LogRecord> lines = resultLines();
        assertEquals(running.length, lines.size());
        for (LogRecord line : lines) {
            assertFalse(
                    "version number in a redacted result line: " + line.getMessage(),
                    VERSION_NUMBER.matcher(line.getMessage()).find());
        }
    }

    @Test
    public void withoutRedactionTheVersionsAreInTheLine() {
        ConfigPersister persister = persister();
        save(persister, true, true, false);
        service(persister, "3.0.1").check(Trigger.MANUAL);
        assertTrue(VERSION_NUMBER.matcher(resultLines().get(0).getMessage()).find());
    }

    @Test
    public void redactionDoesNotTouchFailureLines() {
        ConfigPersister persister = persister();
        save(persister, true, true, true);
        feed.setStatus(500);
        service(persister, "2.27.0").check(Trigger.MANUAL);
        boolean sawFailureDetail = false;
        for (LogRecord r : log.records()) {
            sawFailureDetail |= r.getMessage().contains("HTTP 500");
        }
        assertTrue(sawFailureDetail);
    }

    @Test
    public void theLogNamesTheStateButNeverTheAdvisory() {
        feed.setBody(("{\"schema_version\": 1, \"series\": ["
                + "{\"series\": \"2.27.x\", \"phase\": \"maintenance\", \"latest_version\": \"2.27.6\"}],"
                + "\"advisories\": [{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\", \"cve_id\": \"CVE-2026-0001\","
                + "\"severity\": \"critical\", \"summary\": \"Affects 2.27.0 through 2.27.5\","
                + "\"vulnerable_versions\": [\"< 2.27.6\"], \"patched_versions\": {\"2.27.x\": \"2.27.6\"}}]}"));
        ConfigPersister persister = persister();
        save(persister, true, true, true);
        service(persister, "2.27.0").check(Trigger.MANUAL);
        String line = resultLines().get(0).getMessage();
        assertEquals("Manual update check: URGENT_UPDATE_AVAILABLE", line);

        // the advisory itself (id, summary) is never written to the log, only the state
        save(persister, true, true, false);
        service(persister, "2.27.0").check(Trigger.MANUAL);
        String full = resultLines().get(1).getMessage();
        assertEquals("Manual update check: URGENT_UPDATE_AVAILABLE (running 2.27.0, 2.27.6 is available)", full);
        assertFalse(full, full.contains("CVE-") || full.contains("GHSA") || full.contains("Affects"));
    }
    // ---- status header and error lines --------------------------------------------------------------------------

    @Test
    public void headerBeforeAnyCheckSaysTheFeedWasNotChecked() {
        Status status = service(persister(), "2.27.0").getStatus();
        // the series phase comes from the feed, which has not been read yet
        assertEquals(
                "You are currently running version <strong>2.27.0</strong> which is on the <strong>2.27.x</strong> series.",
                status.getHeaderHtml(Locale.ROOT));
        assertEquals("The feed has not been checked yet.", status.getFeedLine(Locale.ROOT));
    }

    @Test
    public void headerAfterACheckNamesThePhaseAndTheCheckTime() {
        Status status = service(persister(), "2.27.0").check(Trigger.MANUAL);
        assertEquals(
                "You are currently running version <strong>2.27.0</strong> which is on the <strong>maintenance</strong> series.",
                status.getHeaderHtml(Locale.ROOT));
        String line = status.getFeedLine(Locale.ROOT);
        assertTrue(line, line.matches("Feed checked \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\S+\\.$"));
        assertNull(status.getErrorMessage(Locale.ROOT));
    }

    @Test
    public void headerOfADevelopmentBuildHasNoSeries() {
        Status status = service(persister(), "2.28-SNAPSHOT").check(Trigger.MANUAL);
        assertEquals(
                "You are currently running version <strong>2.28-SNAPSHOT</strong> (a development build).",
                status.getHeaderHtml(Locale.ROOT));
        assertNull(status.getNewerVersion());
    }

    @Test
    public void headerOfAnUnknownVersionSaysChecksAreDisabled() {
        Status status = service(persister(), null).check(Trigger.MANUAL);
        assertEquals(
                "The running GeoServer version could not be determined, so update checks are disabled.",
                status.getHeaderHtml(Locale.ROOT));
        assertEquals(0, feed.getRequests());
    }

    @Test
    public void headerOfASeriesTheFeedDoesNotListHasNoPhase() {
        Status status = service(persister(), "2.10.1").check(Trigger.MANUAL);
        assertEquals(
                "You are currently running version <strong>2.10.1</strong> which is on the <strong>2.10.x</strong> series.",
                status.getHeaderHtml(Locale.ROOT));
    }

    @Test
    public void aFailedFirstFetchReportsNoFeedAndTheReason() {
        feed.setStatus(500);
        Status result = service(persister(), "2.27.0").check(Trigger.MANUAL);
        assertTrue(result.isFetchFailed());
        assertEquals("No feed data is available.", result.getFeedLine(Locale.ROOT));
        assertTrue(result.getErrorMessage(Locale.ROOT).contains("HTTP 500"));
        assertEquals(
                "Check failed: " + result.getErrorMessage(Locale.ROOT) + ".",
                result.getCheckFailedMessage(Locale.ROOT));
    }

    @Test
    public void aFailureAfterAGoodFetchKeepsTheStaleCopyAndSaysSo() {
        UpdatesCheckerService service = service(persister(), "2.27.0");
        service.check(Trigger.MANUAL);
        feed.setStatus(500);
        Status result = service.check(Trigger.MANUAL);
        assertTrue(result.isFetchFailed());
        assertTrue(result.getFeedLine(Locale.ROOT).endsWith(" Feed unavailable, showing cached data."));
    }

    @Test
    public void checkFailedMessageFallsBackToAnUnknownReason() {
        Status ok = service(persister(), "2.27.0").check(Trigger.MANUAL);
        assertEquals("Check failed: unknown error.", ok.getCheckFailedMessage(Locale.ROOT));
    }
    // ---- suppressed advisories ----------------------------------------------------------------------------------

    /** The running 2.27.0 is up to date, and affected by one advisory. */
    private void serveOneAdvisory() {
        feed.setBody("{\"schema_version\": 1, \"series\": ["
                + "{\"series\": \"2.27.x\", \"phase\": \"stable\", \"latest_version\": \"2.27.0\"}],"
                + "\"advisories\": [{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\", \"cve_id\": \"CVE-2026-0001\","
                + "\"severity\": \"critical\", \"summary\": \"Affects 2.27.0\","
                + "\"vulnerable_versions\": [\"< 2.27.1\"], \"patched_versions\": {}}]}");
    }

    @Test
    public void anAdvisoryNamedByTheServerIsLeftOutAndTheLogSaysHowMany() {
        serveOneAdvisory();
        ConfigPersister persister = persister();
        save(persister, true, true, false);
        UpdatesCheckerService service = service(persister, "2.27.0");

        assertEquals(
                Status.UpdateState.URGENT_UPDATE_AVAILABLE,
                service.check(Trigger.MANUAL).getUpdateState());

        System.setProperty(Suppression.PROPERTY, "CVE-2026-0001");
        try {
            Status status = service.check(Trigger.MANUAL);
            assertEquals(Status.UpdateState.UP_TO_DATE, status.getUpdateState());
            assertTrue(status.getNotifications().isEmpty());
            assertEquals(List.of("CVE-2026-0001"), status.getSuppressed());
            assertEquals(List.of("CVE-2026-0001"), service.getStatus().getSuppressed());
            assertEquals(
                    "Suppressed by the server configuration: CVE-2026-0001.", status.getSuppressedMessage(Locale.ROOT));
            String line = resultLines().get(1).getMessage();
            assertEquals("Manual update check: GeoServer 2.27.0 is up to date. (1 suppressed)", line);
            assertFalse(line, line.contains("CVE-"));
        } finally {
            System.clearProperty(Suppression.PROPERTY);
        }
    }

    @Test
    public void aListFromARequestReplacesTheServersForThatCallOnly() {
        serveOneAdvisory();
        ConfigPersister persister = persister();
        save(persister, true, true, false);
        UpdatesCheckerService service = service(persister, "2.27.0");
        System.setProperty(Suppression.PROPERTY, "CVE-2026-0001");
        try {
            Suppression none = Suppression.fromRequest("{\"suppressedCves\": []}");
            Status shown = service.check(Trigger.MANUAL, none);
            assertEquals(Status.UpdateState.URGENT_UPDATE_AVAILABLE, shown.getUpdateState());
            assertTrue(shown.getSuppressed().isEmpty());

            Suppression other = Suppression.fromRequest("{\"suppressedCves\": [\"GHSA-aaaa-bbbb-cccc\"]}");
            Status hidden = service.check(Trigger.MANUAL, other);
            assertEquals(Status.UpdateState.UP_TO_DATE, hidden.getUpdateState());
            assertEquals(
                    "Manual update check: GeoServer 2.27.0 is up to date. (1 suppressed by the request)",
                    resultLines().get(1).getMessage());

            assertEquals(
                    "the server's list applies again afterwards",
                    List.of("CVE-2026-0001"),
                    service.getStatus().getSuppressed());
        } finally {
            System.clearProperty(Suppression.PROPERTY);
        }
    }

    @Test
    public void theStartupLogNamesTheSuppressedAdvisoriesAndTheEntriesItIgnored() {
        UpdatesCheckerService service = service(persister(), "2.27.0");
        service.logSuppression();
        assertFalse(log.warned("not CVE or GHSA"));

        System.setProperty(Suppression.PROPERTY, "cve-2026-0002 CVE-2026-0001 nonsense");
        try {
            service.logSuppression();
            boolean named = false;
            for (LogRecord r : log.records()) {
                named |= r.getLevel() == Level.WARNING
                        && r.getMessage()
                                .equals("Not reporting the advisories named by UPDATES_CHECKER_SUPPRESSED_CVES: "
                                        + "CVE-2026-0001, CVE-2026-0002");
            }
            assertTrue("the suppressed advisories are logged", named);
            assertTrue(log.warned(
                    "Ignoring entries of UPDATES_CHECKER_SUPPRESSED_CVES that are not CVE or GHSA ids: nonsense"));
        } finally {
            System.clearProperty(Suppression.PROPERTY);
        }
    }
}

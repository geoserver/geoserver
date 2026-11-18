/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.geoserver.updateschecker.TestFeeds.advisory;
import static org.geoserver.updateschecker.TestFeeds.series;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.geoserver.updateschecker.Notification.Type;
import org.junit.Test;

public class EvaluatorTest {

    private static Feed feed(String json) throws Exception {
        return Feed.parse(json.getBytes(StandardCharsets.UTF_8));
    }

    private static Feed realFeed() throws Exception {
        return Feed.parse(FeedTest.sampleFeed());
    }

    /** A feed with stable 3.0.1, maintenance 2.28.5 and 2.27.6, archive 2.26.4, and the given advisories. */
    private static Feed feedWith(boolean syncOn227, String advisories) throws Exception {
        return feed(TestFeeds.feed(
                advisories,
                series("3.0.x", "stable", "3.0.1", false),
                series("2.28.x", "maintenance", "2.28.5", false),
                series("2.27.x", "maintenance", "2.27.6", syncOn227),
                series("2.26.x", "archive", "2.26.4", false)));
    }

    private static List<Notification> evaluate(String running, Feed feed) {
        return Evaluator.evaluate(RunningVersion.of(running), feed, true);
    }

    private static List<Type> types(List<Notification> list) {
        List<Type> types = new ArrayList<>();
        for (Notification n : list) {
            types.add(n.getType());
        }
        return types;
    }

    private static Notification only(List<Notification> list) {
        assertEquals(list.toString(), 1, list.size());
        return list.get(0);
    }

    // ---- states with nothing to compare ------------------------------------------------------------------------

    @Test
    public void unknownVersionRaisesAnExplicitNotice() throws Exception {
        Notification n = only(evaluate(null, realFeed()));
        assertEquals(Type.VERSION_UNKNOWN, n.getType());
        assertEquals(Severity.INFO, n.getSeverity());
        assertEquals(
                "GeoServer's version could not be determined, so update checks are disabled.",
                n.getMessage(Locale.ROOT));
        assertEquals(Type.VERSION_UNKNOWN, only(evaluate("garbage", null)).getType());
        assertEquals(
                Type.VERSION_UNKNOWN,
                only(Evaluator.evaluate(RunningVersion.of(null), null, false)).getType());
    }

    @Test
    public void noFeedYet() {
        RunningVersion running = RunningVersion.of("2.27.0");
        // nothing attempted yet: say nothing
        assertTrue(Evaluator.evaluate(running, null, false).isEmpty());
        // attempted and failed with nothing cached: never silent
        Notification n = only(Evaluator.evaluate(running, null, true));
        assertEquals(Type.FEED_UNAVAILABLE, n.getType());
        assertEquals(Severity.WARN, n.getSeverity());
        assertTrue(n.getActions().isEmpty());
    }

    @Test
    public void feedSchemaNewerThanSupported() throws Exception {
        Notification n = only(evaluate("2.27.0", feed("{\"schema_version\": 99}")));
        assertEquals(Type.SCHEMA_TOO_NEW, n.getType());
        assertEquals(Severity.WARN, n.getSeverity());
        assertEquals(
                "Version feed schema is newer than this plugin understands. Please update gs-updates-checker.",
                n.getMessage(Locale.ROOT));
        assertTrue(n.getActions().isEmpty());
    }

    @Test
    public void developmentBuildsGetNoNotificationsAndNoAdvisories() throws Exception {
        assertTrue(evaluate("3.1.0-SNAPSHOT", realFeed()).isEmpty());

        // no advisory is evaluated for a development build, even one that would match
        Feed feed = feedWith(false, advisory("GHSA-aaaa-bbbb-cccc", "critical", "\">= 1.0.0\"", ""));
        assertTrue(evaluate("2.27.0-RC1", feed).isEmpty());
        assertTrue(evaluate("3.0-RC", feed).isEmpty());
    }

    // ---- patches and series -------------------------------------------------------------------------------------

    @Test
    public void upToDateOnTheStableSeriesHasNothingToSay() throws Exception {
        assertTrue(evaluate("3.0.1", feedWith(false, "")).isEmpty());
        // ahead of the latest patch the feed knows about
        assertTrue(evaluate("3.0.2", feedWith(false, "")).isEmpty());
    }

    @Test
    public void newerPatchInTheSameSeries() throws Exception {
        Notification n = only(evaluate("3.0.0", feedWith(false, "")));
        assertEquals(Type.PATCH_AVAILABLE, n.getType());
        assertEquals(Severity.WARN, n.getSeverity());
        assertEquals("3.0.1", n.getDiscriminator());
        assertEquals("3.0.1", n.getTargetVersion());
        assertEquals("You are running GeoServer 3.0.0. GeoServer 3.0.1 is now available.", n.getMessage(Locale.ROOT));
        assertEquals("https://github.com/geoserver/geoserver/releases/tag/3.0.1", n.getLink());
        assertEquals(
                actionList(Notification.Action.REMIND_TOMORROW, Notification.Action.DISMISS_VERSION), n.getActions());
    }

    private static List<Notification.Action> actionList(Notification.Action... a) {
        return java.util.Arrays.asList(a);
    }

    @Test
    public void patchNotificationNamesBothVersions() throws Exception {
        Notification n = only(evaluate("3.0.0", feedWith(false, "")));
        assertEquals("You are running GeoServer 3.0.0. GeoServer 3.0.1 is now available.", n.getMessage(Locale.ROOT));
    }

    @Test
    public void coordinatedReleaseReplacesThePlainPatchNotice() throws Exception {
        List<Notification> list = evaluate("2.27.0", feedWith(true, ""));
        assertTrue(types(list).contains(Type.SECURITY_RELEASE));
        assertFalse("only the stronger signal is shown", types(list).contains(Type.PATCH_AVAILABLE));
        Notification n = list.get(0);
        assertEquals(Type.SECURITY_RELEASE, n.getType());
        assertEquals(Severity.WARN, n.getSeverity());
        assertEquals("2.27.6", n.getDiscriminator());
        assertEquals(actionList(Notification.Action.REMIND_TOMORROW, Notification.Action.DISMISS), n.getActions());
    }

    @Test
    public void coordinatedReleaseOnlyMattersWhileBehindIt() throws Exception {
        assertFalse(types(evaluate("2.27.6", feedWith(true, ""))).contains(Type.SECURITY_RELEASE));
    }

    // ---- security releases: the recent releases of the running series ----------------------------------------------

    /** The series 2.28.x with the given recent releases, newest first, and the other series of {@link #feedWith}. */
    private static Feed feedWithRecent(String... releases) throws Exception {
        return feed(TestFeeds.feed(
                "",
                series("3.0.x", "stable", "3.0.1", false),
                TestFeeds.seriesWithReleases("2.28.x", "maintenance", "2.28.5", releases)));
    }

    @Test
    public void aFlaggedNewerReleaseIsAHighSecurityReleaseThatReplacesThePatchNotice() throws Exception {
        Feed feed = feedWithRecent(
                TestFeeds.release("2.28.5", false, false, null),
                TestFeeds.release("2.28.4", true, false, TestFeeds.blog("2.28.4")));
        List<Notification> list = evaluate("2.28.3", feed);
        Notification n = list.get(0);
        assertEquals(Type.SECURITY_RELEASE, n.getType());
        assertEquals(Severity.HIGH, n.getSeverity());
        assertEquals("the key is the selected release", "2.28.4", n.getDiscriminator());
        assertEquals("the upgrade target is the latest", "2.28.5", n.getTargetVersion());
        assertEquals(TestFeeds.blog("2.28.4"), n.getLink());
        assertEquals(
                "Security release: GeoServer 2.28.4 contains security fixes. You are running 2.28.3; 2.28.5 is"
                        + " available. Review the release notes.",
                n.getMessage(Locale.ROOT));
        assertFalse(types(list).contains(Type.PATCH_AVAILABLE));
        assertEquals(actionList(Notification.Action.REMIND_TOMORROW, Notification.Action.DISMISS), n.getActions());
    }

    @Test
    public void theNewestFlaggedReleaseIsNamedEvenWhenANewerOneIsOnlyCoordinated() throws Exception {
        // worked example: 2.28.5 is flagged and coordinated, 2.28.4 only flagged
        Feed both = feedWithRecent(
                TestFeeds.release("2.28.5", true, true, TestFeeds.blog("2.28.5")),
                TestFeeds.release("2.28.4", true, false, TestFeeds.blog("2.28.4")));
        assertEquals("2.28.5", evaluate("2.28.3", both).get(0).getDiscriminator());

        Feed older = feedWithRecent(
                TestFeeds.release("2.28.5", false, true, null),
                TestFeeds.release("2.28.4", true, false, TestFeeds.blog("2.28.4")));
        Notification n = evaluate("2.28.3", older).get(0);
        assertEquals(Severity.HIGH, n.getSeverity());
        assertEquals("2.28.4", n.getDiscriminator());
        assertEquals("2.28.5", n.getTargetVersion());
    }

    @Test
    public void onlyACoordinatedNewerReleaseIsAWarnSecurityRelease() throws Exception {
        Feed feed = feedWithRecent(
                TestFeeds.release("2.28.5", false, true, TestFeeds.blog("2.28.5")),
                TestFeeds.release("2.28.4", false, false, null));
        Notification n = evaluate("2.28.3", feed).get(0);
        assertEquals(Type.SECURITY_RELEASE, n.getType());
        assertEquals(Severity.WARN, n.getSeverity());
        assertEquals("2.28.5", n.getDiscriminator());
        assertEquals(
                "GeoServer 2.28.5 was released together with updates to other series, which usually means a"
                        + " security release. You are running 2.28.3; 2.28.5 is available. Review the release notes.",
                n.getMessage(Locale.ROOT));
    }

    @Test
    public void aReleaseWithoutAnnouncementSaysTheNotesAreNotPublishedYet() throws Exception {
        Feed feed = feedWithRecent(TestFeeds.release("2.28.5", true, false, null));
        Notification n = evaluate("2.28.3", feed).get(0);
        assertNull(n.getLink());
        assertTrue(
                n.getMessage(Locale.ROOT),
                n.getMessage(Locale.ROOT).endsWith(" The release notes are not published yet."));
    }

    @Test
    public void noNoticeWhenTheRunningVersionIsAtOrBeyondEveryFlaggedRelease() throws Exception {
        Feed feed = feedWithRecent(
                TestFeeds.release("2.28.5", false, false, null),
                TestFeeds.release("2.28.4", true, true, TestFeeds.blog("2.28.4")));
        // 2.28.4 is the flagged one: running it, or anything newer, leaves only the plain patch notice
        assertEquals(typeList(Type.PATCH_AVAILABLE, Type.SERIES_MAINTENANCE), types(evaluate("2.28.4", feed)));
        assertFalse(has(evaluate("2.28.5", feed), Type.PATCH_AVAILABLE));
        assertFalse(has(evaluate("2.28.5", feed), Type.SECURITY_RELEASE));
    }

    @Test
    public void aFeedWithoutRecentReleasesIsReadThroughTheLatestRelease() throws Exception {
        // no recent_releases: the series-level flag of the latest release decides
        Feed feed = feed(TestFeeds.feed(
                "",
                series("3.0.x", "stable", "3.0.1", false),
                "{\"series\": \"2.28.x\", \"phase\": \"maintenance\", \"latest_version\": \"2.28.5\","
                        + " \"security_flagged\": true, \"blog_url\": \"" + TestFeeds.blog("2.28.5") + "\"}"));
        Notification n = evaluate("2.28.3", feed).get(0);
        assertEquals(Type.SECURITY_RELEASE, n.getType());
        assertEquals(Severity.HIGH, n.getSeverity());
        assertEquals("2.28.5", n.getDiscriminator());
        assertFalse(has(evaluate("2.28.5", feed), Type.SECURITY_RELEASE));
    }

    @Test
    public void aReleaseOutsideTheWindowCannotQualify() throws Exception {
        // 2.28.1 was a security release, but it is older than the window and is no longer listed
        Feed feed = feedWithRecent(TestFeeds.release("2.28.5", false, false, null));
        List<Notification> list = evaluate("2.28.0", feed);
        assertEquals(typeList(Type.PATCH_AVAILABLE, Type.SERIES_MAINTENANCE), types(list));
    }

    @Test
    public void aSecurityReleaseSitsBesideTheAdvisoriesAndRanksAfterThem() throws Exception {
        String advisories = advisory("GHSA-aaaa-aaaa-aaaa", "high", "\">= 1.0.0\"", "");
        Feed feed = feed(TestFeeds.feed(
                advisories,
                series("3.0.x", "stable", "3.0.1", false),
                TestFeeds.seriesWithReleases(
                        "2.28.x", "maintenance", "2.28.5", TestFeeds.release("2.28.5", true, false, null))));
        List<Notification> list = evaluate("2.28.3", feed);
        // both are HIGH; the confirmed advisory comes first
        assertEquals(typeList(Type.CVE_CONFIRMED, Type.SECURITY_RELEASE, Type.SERIES_MAINTENANCE), types(list));
    }

    @Test
    public void maintenanceSeriesGetsANoticeKeyedByTheStableSeries() throws Exception {
        Notification n = only(evaluate("2.28.5", feedWith(false, "")));
        assertEquals(Type.SERIES_MAINTENANCE, n.getType());
        assertEquals(Severity.NOTICE, n.getSeverity());
        assertEquals("3.0.x", n.getDiscriminator());
        assertTrue(n.getMessage(Locale.ROOT).contains("3.0.x"));
        assertTrue(n.getMessage(Locale.ROOT).contains("3.0.1"));
        assertEquals(
                actionList(Notification.Action.REMIND_IN_30_DAYS, Notification.Action.DISMISS_SERIES), n.getActions());
    }

    @Test
    public void archiveSeriesIsAPersistentWarningWithUpgradeTargets() throws Exception {
        Notification n = only(evaluate("2.26.4", feedWith(false, "")));
        assertEquals(Type.SERIES_ARCHIVE, n.getType());
        assertEquals(Severity.WARN, n.getSeverity());
        assertTrue(n.getActions().isEmpty());
        assertTrue(n.getMessage(Locale.ROOT).contains("beyond community support"));
        assertTrue(n.getMessage(Locale.ROOT).contains("Stable: 3.0.1"));
        assertTrue(n.getMessage(Locale.ROOT).contains("Maintenance: 2.28.5, 2.27.6"));
    }

    @Test
    public void archiveSeriesStillSurfacesItsOwnNewerPatch() throws Exception {
        List<Notification> list = evaluate("2.26.1", feedWith(false, ""));
        assertEquals(typeList(Type.PATCH_AVAILABLE, Type.SERIES_ARCHIVE), types(list));
    }

    private static List<Type> typeList(Type... t) {
        return java.util.Arrays.asList(t);
    }

    @Test
    public void seriesMissingFromTheFeedIsTreatedAsArchive() throws Exception {
        Notification n = only(evaluate("2.18.3", feedWith(false, "")));
        assertEquals(Type.SERIES_ARCHIVE, n.getType());
        assertEquals(Severity.WARN, n.getSeverity());
        assertTrue(n.getMessage(Locale.ROOT).contains("Your series (2.18.x) predates the feed coverage."));
        assertTrue(n.getMessage(Locale.ROOT).contains("Community support has long since ended."));
        assertTrue(n.getMessage(Locale.ROOT).contains("Stable: 3.0.1"));
    }

    @Test
    public void aSeriesNewerThanTheWholeFeedIsAheadNotOutOfSupport() throws Exception {
        assertTrue(evaluate("3.1.0", feedWith(false, "")).isEmpty());
    }

    // ---- advisory applicability ----------------------------------------------------------------------

    private static final String GHSA = "GHSA-jvpx-6qxg-whgc";

    /** GHSA-jvpx-6qxg-whgc as first published: its third range can never match, which GitHub data sometimes does. */
    private static String jvpx() {
        return advisory(
                GHSA,
                "critical",
                "\"3.0.0\", \">=2.28.0\", \">2.23.5, <2.25.0, >=2.25.3\"",
                "\"3.0.x\": \"3.0.1\", \"2.28.x\": \"2.28.5\", \"2.27.x\": \"2.27.6\"");
    }

    private static boolean has(List<Notification> list, Type type) {
        return types(list).contains(type);
    }

    @Test
    public void atOrBeyondThePatchedVersionOfItsOwnSeriesIsUnaffected() throws Exception {
        Feed feed = feedWith(false, jvpx());
        assertFalse(has(evaluate("3.0.1", feed), Type.CVE_CONFIRMED));
        assertFalse(has(evaluate("2.28.5", feed), Type.CVE_CONFIRMED));
        assertFalse(has(evaluate("2.28.6", feed), Type.CVE_CONFIRMED));
        assertFalse(has(evaluate("2.27.6", feed), Type.CVE_CONFIRMED));
        assertFalse(has(evaluate("2.27.6", feed), Type.CVE_UNVERIFIABLE));
    }

    @Test
    public void aSeriesWithNoEntryAboveEveryPatchedVersionIsUnaffected() throws Exception {
        Feed feed = feedWith(false, jvpx());
        assertTrue(evaluate("3.1.0", feed).isEmpty());
        assertFalse(has(evaluate("4.0.0", feed), Type.CVE_UNVERIFIABLE));
    }

    @Test
    public void belowThePatchedVersionOfItsOwnSeriesIsAffectedEvenWhenNoRangeMatches() throws Exception {
        Feed feed = feedWith(false, jvpx());
        // 2.27.5 matches no readable range (the third row can never match), but 2.27.6 is the first fix
        Notification n = evaluate("2.27.5", feed).get(0);
        assertEquals(Type.CVE_CONFIRMED, n.getType());
        assertEquals(Severity.CRITICAL, n.getSeverity());
        assertEquals(GHSA, n.getDiscriminator());
        assertEquals("https://github.com/geoserver/geoserver/security/advisories/" + GHSA, n.getLink());
        assertEquals("CVE-2026-0001 (critical): Summary text", n.getMessage(Locale.ROOT));
        assertEquals(actionList(Notification.Action.REMIND_TOMORROW), n.getActions());

        assertEquals(Type.CVE_CONFIRMED, evaluate("3.0.0", feed).get(0).getType());
        assertEquals(Type.CVE_CONFIRMED, evaluate("2.28.4", feed).get(0).getType());
    }

    @Test
    public void unreadableRangeInAGapIsFlaggedForManualReview() throws Exception {
        // 2.26.3 has no patched entry of its own and is below the highest one, so only the ranges could decide:
        // the readable ones do not match and the unsatisfiable one is unreadable
        Feed feed = feedWith(false, jvpx());
        Notification n = evaluate("2.26.3", feed).get(0);
        assertEquals(Type.CVE_UNVERIFIABLE, n.getType());
        assertEquals(Severity.CRITICAL, n.getSeverity());
        assertTrue(n.getMessage(Locale.ROOT)
                .endsWith("Summary text. Version range could not be determined automatically. Review manually"));
        assertEquals(actionList(Notification.Action.DISMISS), n.getActions());
    }

    @Test
    public void aConfirmedAdvisoryRanksAboveOneThatCouldNotBeChecked() throws Exception {
        // the unreadable one is listed first in the feed, and both are critical
        Feed feed = feedWith(
                false,
                advisory("GHSA-aaaa-bbbb-cccc", "critical", "\"df11a650c650b2b5a73fe6c6b0e6d0d8a6e2a8c1\"", "")
                        + ","
                        + advisory("GHSA-dddd-eeee-ffff", "critical", "\"< 2.27.1\"", ""));
        List<Notification> list = evaluate("2.27.0", feed);
        assertEquals(typeList(Type.CVE_CONFIRMED, Type.CVE_UNVERIFIABLE), types(list.subList(0, 2)));
        assertEquals(Severity.CRITICAL, list.get(0).getSeverity());
        assertEquals(Severity.CRITICAL, list.get(1).getSeverity());
    }

    @Test
    public void rangeMatchWithNoPatchedInformation() throws Exception {
        Feed feed = feedWith(false, advisory("GHSA-aaaa-bbbb-cccc", "high", "\"< 2.27.1\"", ""));
        Notification n = evaluate("2.27.0", feed).get(0);
        assertEquals(Type.CVE_CONFIRMED, n.getType());
        assertEquals(Severity.HIGH, n.getSeverity());
        assertEquals(actionList(Notification.Action.DISMISS), n.getActions());
        assertFalse(has(evaluate("2.27.1", feed), Type.CVE_CONFIRMED));
    }

    @Test
    public void readableRangesThatDoNotMatchMeanNotAffected() throws Exception {
        Feed feed = feedWith(false, advisory("GHSA-aaaa-bbbb-cccc", "high", "\"< 2.20.0\"", ""));
        assertTrue(evaluate("2.28.5", feed).stream().noneMatch(x -> x.getType().getGroup() == 0));
    }

    @Test
    public void anAdvisoryWithNoRangesAndNoPatchedVersionsIsNeverIgnored() throws Exception {
        Feed feed = feedWith(false, advisory("GHSA-aaaa-bbbb-cccc", "medium", "", ""));
        Notification n = evaluate("2.28.5", feed).get(0);
        assertEquals(Type.CVE_UNVERIFIABLE, n.getType());
        assertEquals(Severity.WARN, n.getSeverity());
    }

    @Test
    public void commitShaRangeIsUnverifiable() throws Exception {
        Feed feed = feedWith(
                false, advisory("GHSA-aaaa-bbbb-cccc", "low", "\"df11a650c650b2b5a73fe6c6b0e6d0d8a6e2a8c1\"", ""));
        List<Notification> list = evaluate("2.28.5", feed);
        // a low-severity advisory is INFO, which ranks below the NOTICE about the maintenance series
        assertEquals(typeList(Type.SERIES_MAINTENANCE, Type.CVE_UNVERIFIABLE), types(list));
        assertEquals(Severity.INFO, list.get(1).getSeverity());
    }

    @Test
    public void dismissalActionsFollowTheSeverityOfAConfirmedCve() throws Exception {
        String r = "\">= 1.0.0\"";
        Feed feed = feedWith(
                false,
                advisory("GHSA-aaaa-aaaa-aaaa", "low", r, "") + ","
                        + advisory("GHSA-dddd-dddd-dddd", "critical", r, ""));
        for (Notification n : evaluate("2.28.5", feed)) {
            if (n.getType() != Type.CVE_CONFIRMED) {
                continue;
            }
            if (n.getSeverity() == Severity.CRITICAL) {
                assertEquals(actionList(Notification.Action.REMIND_TOMORROW), n.getActions());
            } else {
                assertEquals(actionList(Notification.Action.DISMISS), n.getActions());
            }
        }
    }

    // ---- ordering ------------------------------------------------------------------------------------------------

    @Test
    public void orderedBySeverityThenSecurityThenPatchThenSeries() throws Exception {
        String r = "\">= 1.0.0\"";
        Feed feed = feedWith(
                true,
                advisory("GHSA-aaaa-aaaa-aaaa", "medium", r, "") + ","
                        + advisory("GHSA-bbbb-bbbb-bbbb", "critical", r, ""));
        // 2.27.0 is maintenance, behind a coordinated release, with a critical and a medium advisory
        List<Notification> list = evaluate("2.27.0", feed);
        assertEquals(4, list.size());
        assertEquals(Type.CVE_CONFIRMED, list.get(0).getType());
        assertEquals(Severity.CRITICAL, list.get(0).getSeverity());
        assertEquals(Type.SERIES_MAINTENANCE, list.get(3).getType());
        // the WARN-level security notices (medium CVE, coordinated release) sit between them
        assertTrue(types(list.subList(1, 3)).contains(Type.CVE_CONFIRMED));
        assertTrue(types(list.subList(1, 3)).contains(Type.SECURITY_RELEASE));
        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i - 1).getSeverity().compareTo(list.get(i).getSeverity()) <= 0);
        }
    }

    @Test
    public void securityBeforePatchBeforeSeriesWithinOneSeverity() throws Exception {
        String r = "\">= 1.0.0\"";
        Feed feed = feedWith(false, advisory("GHSA-aaaa-aaaa-aaaa", "medium", r, ""));
        // archive 2.26.1 with a newer patch available: WARN patch, WARN archive series notice, WARN medium CVE
        List<Notification> list = evaluate("2.26.1", feed);
        assertEquals(typeList(Type.CVE_CONFIRMED, Type.PATCH_AVAILABLE, Type.SERIES_ARCHIVE), types(list));
    }

    // ---- the real feed -------------------------------------------------------------------------------------------

    @Test
    public void realFeedForTheDeveloperModeTestVersion() throws Exception {
        List<Notification> list = evaluate("2.27.0", realFeed());
        assertFalse(list.isEmpty());
        assertTrue(has(list, Type.SECURITY_RELEASE));
        assertTrue(has(list, Type.SERIES_MAINTENANCE));
        assertTrue(has(list, Type.CVE_CONFIRMED));
        Notification top = list.get(0);
        assertEquals(Severity.CRITICAL, top.getSeverity());
        for (int i = 1; i < list.size(); i++) {
            Notification a = list.get(i - 1);
            Notification b = list.get(i);
            int bySeverity = a.getSeverity().compareTo(b.getSeverity());
            assertTrue(bySeverity <= 0);
            if (bySeverity == 0) {
                assertTrue(a.getType().getGroup() <= b.getType().getGroup());
            }
        }
    }

    @Test
    public void realFeedNamesTheNewestFlaggedReleaseToAnAdministratorWhoIsBehind() throws Exception {
        // 2.28.4 and 2.28.5 are both flagged in the real feed; the notice names the newer one
        Notification n = evaluate("2.28.3", realFeed()).stream()
                .filter(x -> x.getType() == Type.SECURITY_RELEASE)
                .findFirst()
                .get();
        assertEquals(Severity.HIGH, n.getSeverity());
        assertEquals("2.28.5", n.getDiscriminator());
        assertTrue(n.getLink().startsWith("https://geoserver.org/"));
    }

    @Test
    public void realFeedForTheLatestStableHasNoNotifications() throws Exception {
        List<Notification> list = evaluate("3.0.1", realFeed());
        for (Notification n : list) {
            assertNotSame("unexpected for fully patched stable: " + n, Type.PATCH_AVAILABLE, n.getType());
            assertNotSame("unexpected for fully patched stable: " + n, Type.SERIES_ARCHIVE, n.getType());
        }
    }

    @Test
    public void everyNotificationHasADiscriminatorAndNoNullText() throws Exception {
        for (String running : new String[] {"2.27.0", "2.18.3", "3.0.0", "3.1.0-SNAPSHOT"}) {
            for (Notification n : evaluate(running, realFeed())) {
                assertTrue(n.getDiscriminator() != null && !n.getDiscriminator().isEmpty());
                assertTrue(n.getMessage(Locale.ROOT) != null
                        && !n.getMessage(Locale.ROOT).isEmpty());
            }
        }
    }
    // ---- suppressed advisories ----------------------------------------------------------------------------------

    private static List<Notification> evaluateSuppressing(String running, Feed feed, String suppressed) {
        return Evaluator.evaluate(
                RunningVersion.of(running), feed, true, Suppression.parse(suppressed, new ArrayList<>()));
    }

    private static Feed twoAdvisories() throws Exception {
        return feedWith(
                false,
                advisory("GHSA-aaaa-bbbb-cccc", "critical", "\"< 2.28.4\"", "\"2.28.x\": \"2.28.4\"")
                        + ","
                        + advisory("GHSA-dddd-eeee-ffff", "medium", "\"< 2.28.4\"", "\"2.28.x\": \"2.28.4\""));
    }

    @Test
    public void aSuppressedAdvisoryIsNotReportedWhateverIdNamesIt() throws Exception {
        Feed feed = twoAdvisories();
        assertEquals(
                2,
                types(evaluate("2.28.3", feed)).stream()
                        .filter(t -> t == Type.CVE_CONFIRMED)
                        .count());
        // both advisories carry the CVE id the test feeds give them
        for (String id : new String[] {"CVE-2026-0001", "cve-2026-0001"}) {
            List<Notification> result = evaluateSuppressing("2.28.3", feed, id);
            assertEquals(id, List.of(Type.PATCH_AVAILABLE, Type.SERIES_MAINTENANCE), types(result));
        }
    }

    @Test
    public void aGhsaIdSuppressesOnlyItsOwnAdvisory() throws Exception {
        List<Notification> result = evaluateSuppressing("2.28.3", twoAdvisories(), "GHSA-aaaa-bbbb-cccc");
        assertEquals(List.of(Type.CVE_CONFIRMED, Type.PATCH_AVAILABLE, Type.SERIES_MAINTENANCE), types(result));
        assertEquals(Severity.WARN, result.get(0).getSeverity());
    }

    @Test
    public void anAdvisoryThatCouldNotBeCheckedCanBeSuppressedToo() throws Exception {
        Feed feed = feedWith(
                false, advisory("GHSA-aaaa-bbbb-cccc", "critical", "\"df11a650c650b2b5a73fe6c6b0e6d0d8a6e2a8c1\"", ""));
        assertTrue(has(evaluate("2.28.3", feed), Type.CVE_UNVERIFIABLE));
        assertFalse(has(evaluateSuppressing("2.28.3", feed, "GHSA-aaaa-bbbb-cccc"), Type.CVE_UNVERIFIABLE));
    }

    @Test
    public void suppressingDoesNotHideAReasonToUpgrade() throws Exception {
        Feed feed = twoAdvisories();
        List<Notification> result = evaluateSuppressing("2.28.3", feed, "CVE-2026-0001");
        assertTrue("a newer patch is still reported", has(result, Type.PATCH_AVAILABLE));
        // up to date, so only the series notice is left
        assertEquals(List.of(Type.SERIES_MAINTENANCE), types(evaluateSuppressing("2.28.5", feed, "CVE-2026-0001")));
    }

    @Test
    public void theSuppressedAdvisoriesThatApplyAreListed() throws Exception {
        Feed feed = twoAdvisories();
        Suppression both = Suppression.parse("GHSA-aaaa-bbbb-cccc GHSA-dddd-eeee-ffff", new ArrayList<>());
        RunningVersion affected = RunningVersion.of("2.28.3");
        assertEquals(List.of("CVE-2026-0001", "CVE-2026-0001"), Evaluator.suppressedAdvisories(affected, feed, both));
        assertTrue(Evaluator.suppressedAdvisories(RunningVersion.of("2.28.4"), feed, both)
                .isEmpty());
        assertTrue(Evaluator.suppressedAdvisories(affected, feed, Suppression.none())
                .isEmpty());
        assertTrue(Evaluator.suppressedAdvisories(affected, null, both).isEmpty());
        assertTrue(Evaluator.suppressedAdvisories(RunningVersion.of("3.1.0-SNAPSHOT"), feed, both)
                .isEmpty());
    }

    @Test
    public void anAdvisoryWithoutACveIsListedByItsGhsaId() throws Exception {
        Feed feed = feed("{\"schema_version\": 1, \"series\": [], \"advisories\": ["
                + "{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\", \"severity\": \"high\", \"summary\": \"s\","
                + " \"vulnerable_versions\": [\"< 2.28.4\"], \"patched_versions\": {}}]}");
        Suppression s = Suppression.parse("GHSA-aaaa-bbbb-cccc", new ArrayList<>());
        assertEquals(
                List.of("GHSA-aaaa-bbbb-cccc"), Evaluator.suppressedAdvisories(RunningVersion.of("2.28.3"), feed, s));
    }
}

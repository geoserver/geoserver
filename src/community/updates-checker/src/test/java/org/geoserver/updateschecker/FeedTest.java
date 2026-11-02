/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

public class FeedTest {

    static byte[] sampleFeed() throws Exception {
        try (InputStream in = FeedTest.class.getResourceAsStream("feed-sample.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private Feed parse(String json) throws Exception {
        return Feed.parse(json.getBytes(StandardCharsets.UTF_8));
    }

    private void assertRejected(String json) {
        try {
            parse(json);
            fail("should have been rejected: " + json);
        } catch (FeedException expected) {
            assertNotNull(expected.getMessage());
        } catch (Exception other) {
            fail("wrong exception type: " + other);
        }
    }

    @Test
    public void readsTheRealFeed() throws Exception {
        Feed feed = Feed.parse(sampleFeed());

        assertFalse(feed.isSchemaTooNew());
        assertFalse(feed.isSchemaTooNew());
        assertEquals(12, feed.getSeries().size());
        assertEquals(33, feed.getAdvisories().size());
        assertTrue(
                "no entry of the real feed should be dropped: " + feed.getWarnings(),
                feed.getWarnings().isEmpty());

        Feed.Series stable = feed.findNewest(Feed.Phase.STABLE);
        assertEquals("3.0.x", stable.getSeries());
        assertEquals("3.0.1", stable.getLatestVersion().toString());
        assertTrue(stable.getLatestRelease().isSynchronizedRelease());
        assertTrue(stable.getLatestRelease().isSecurityFlagged());
        assertEquals("https://github.com/geoserver/geoserver/releases/tag/3.0.1", stable.getReleaseUrl());

        assertEquals(Feed.Phase.MAINTENANCE, feed.findSeries("2.28.x").getPhase());
        assertEquals(Feed.Phase.ARCHIVE, feed.findSeries("2.26.x").getPhase());
        assertFalse(feed.findSeries("2.26.x").getLatestRelease().isSynchronizedRelease());
        assertNull(feed.findSeries("9.9.x"));
    }

    @Test
    public void readsTheRecentReleasesOfTheRealFeed() throws Exception {
        Feed feed = Feed.parse(sampleFeed());

        List<Feed.Release> releases = feed.findSeries("3.0.x").getRecentReleases();
        assertEquals(2, releases.size());
        assertEquals("3.0.1", releases.get(0).getVersion().toString());
        assertTrue(releases.get(0).isSecurityFlagged());
        assertTrue(releases.get(0).isSynchronizedRelease());
        assertTrue(releases.get(0).getBlogUrl().startsWith("https://geoserver.org/"));
        assertEquals("3.0.0", releases.get(1).getVersion().toString());
        assertTrue(releases.get(1).isSecurityFlagged());
        assertFalse(releases.get(1).isSynchronizedRelease());
        assertTrue(
                "an archive series with nothing in the window has an empty list, not none",
                feed.findSeries("2.26.x").getRecentReleases().isEmpty());
    }

    @Test
    public void aSeriesWithoutRecentReleasesHasNoListAndKeepsItsSeriesLevelSignal() throws Exception {
        Feed feed = parse("{\"schema_version\": 1, \"series\": [{\"series\": \"2.27.x\","
                + " \"phase\": \"maintenance\", \"latest_version\": \"2.27.6\", \"security_flagged\": true,"
                + " \"synchronized_release\": false, \"blog_url\": \"" + TestFeeds.blog("2.27.6") + "\"}]}");
        Feed.Series series = feed.findSeries("2.27.x");
        assertNull(series.getRecentReleases());
        assertTrue(series.getLatestRelease().isSecurityFlagged());
        assertFalse(series.getLatestRelease().isSynchronizedRelease());
        assertEquals(TestFeeds.blog("2.27.6"), series.getLatestRelease().getBlogUrl());
    }

    @Test
    public void recentReleasesAreCheckedEntryByEntry() throws Exception {
        Feed feed = parse(
                "{\"schema_version\": 1, \"series\": [{\"series\": \"2.27.x\","
                        + " \"phase\": \"maintenance\", \"latest_version\": \"2.27.6\", \"recent_releases\": ["
                        + "{\"version\": \"2.27.6\", \"blog_url\": \"javascript:alert(1)\", \"security_flagged\": \"yes\","
                        + " \"synchronized_release\": 1},"
                        + "{\"version\": \"2.28.1\", \"security_flagged\": true},"
                        + "{\"version\": \"not a version\"}, \"text\", null,"
                        + "{\"version\": \"2.27.5\", \"blog_url\": \"https://evil.example/x\", \"security_flagged\": true}]}]}");
        List<Feed.Release> releases = feed.findSeries("2.27.x").getRecentReleases();
        assertEquals(2, releases.size());
        assertEquals("2.27.6", releases.get(0).getVersion().toString());
        assertNull("a link that is not allowed is dropped", releases.get(0).getBlogUrl());
        assertFalse("a flag that is not a boolean is false", releases.get(0).isSecurityFlagged());
        assertFalse(releases.get(0).isSynchronizedRelease());
        assertEquals("2.27.5", releases.get(1).getVersion().toString());
        assertNull(releases.get(1).getBlogUrl());
        assertTrue(releases.get(1).isSecurityFlagged());
        assertEquals(
                "four entries are dropped, each with a warning: " + feed.getWarnings(),
                4,
                feed.getWarnings().size());
    }

    @Test
    public void readsAdvisoryWithPatchedVersions() throws Exception {
        Feed feed = Feed.parse(sampleFeed());
        Feed.Advisory advisory = feed.getAdvisories().get(0);

        assertEquals("GHSA-jvpx-6qxg-whgc", advisory.getGhsaId());
        assertEquals("GHSA-jvpx-6qxg-whgc", advisory.getKey());
        assertEquals("CVE-2026-76904", advisory.getCveId());
        assertEquals(Severity.CRITICAL, advisory.getSeverity());
        assertEquals(3, advisory.getVulnerableVersions().size());
        assertEquals(">2.23.5, <2.27.6", advisory.getVulnerableVersions().get(2));
        assertEquals(3, advisory.getPatchedVersions().size());
        assertEquals("3.0.1", advisory.getPatchedVersions().get("3.0.x").toString());
        assertEquals("2.27.6", advisory.getPatchedVersions().get("2.27.x").toString());
    }

    @Test
    public void severityMapping() throws Exception {
        Feed feed = Feed.parse(sampleFeed());
        boolean sawCritical = false;
        boolean sawHigh = false;
        boolean sawWarn = false;
        for (Feed.Advisory a : feed.getAdvisories()) {
            sawCritical |= a.getSeverity() == Severity.CRITICAL;
            sawHigh |= a.getSeverity() == Severity.HIGH;
            sawWarn |= a.getSeverity() == Severity.WARN;
        }
        assertTrue(sawCritical && sawHigh && sawWarn);
        assertEquals(Severity.CRITICAL, Severity.fromAdvisory("critical"));
        assertEquals(Severity.HIGH, Severity.fromAdvisory("high"));
        assertEquals(Severity.WARN, Severity.fromAdvisory("medium"));
        assertEquals(Severity.INFO, Severity.fromAdvisory("low"));
        // never quietly downgrade an advisory because the feed said something unexpected
        assertEquals(Severity.HIGH, Severity.fromAdvisory("catastrophic"));
        assertEquals(Severity.HIGH, Severity.fromAdvisory(null));
    }

    @Test
    public void newerSchemaIsFlaggedAndNothingElseIsRead() throws Exception {
        Feed feed = parse("{\"schema_version\": 2, \"series\": \"not even a list\"}");
        assertTrue(feed.isSchemaTooNew());
        assertTrue(feed.getSeries().isEmpty());
        assertTrue(feed.getAdvisories().isEmpty());
    }

    @Test
    public void unusableFeedsAreRejected() {
        assertRejected("not json");
        assertRejected("");
        assertRejected("[]");
        assertRejected("{}");
        assertRejected("{\"series\": []}");
        assertRejected("{\"schema_version\": \"1\", \"series\": []}");
        assertRejected("{\"schema_version\": 0, \"series\": []}");
        assertRejected("{\"schema_version\": 1}");
        // 4294967297 would read as 1 if truncated to an int, and 1.0 is not an integer
        assertRejected("{\"schema_version\": 4294967297, \"series\": []}");
        assertRejected("{\"schema_version\": 1.0, \"series\": []}");
        // org.json reads a NUL as the end of its input, and takes seconds over a number of a million digits
        assertRejected("{\"schema_version\": 1, \"series\": []}\u0000trailing");
        assertRejected("{\"schema_version\": 1, \"series\": [], \"x\": " + "1".repeat(100_000) + "}");
        // a repeated key could be read differently by another consumer of the file
        assertRejected("{\"schema_version\": 1, \"schema_version\": 2, \"series\": []}");
        assertRejected("{\"schema_version\": 1, \"series\": [{\"series\": \"3.0.x\", \"series\": \"2.0.x\"}]}");
        assertRejected("{\"schema_version\": 1, \"series\": {}}");
        // the retired pre-release layout, a flat map, is not understood
        assertRejected("{\"schema_version\": 1, \"latest_by_series\": {\"3.0.x\": {\"version\": \"3.0.1\"}}}");
    }

    @Test
    public void unreadableSeriesEntriesAreDroppedWithAWarning() throws Exception {
        Feed feed = parse("{\"schema_version\": 1, \"series\": ["
                + "{\"series\": \"3.0.x\", \"phase\": \"stable\", \"latest_version\": \"3.0.1\"},"
                + "{\"series\": \"3.0\", \"phase\": \"stable\", \"latest_version\": \"3.0.1\"},"
                + "{\"series\": \"2.28.x\", \"phase\": \"unheard-of\", \"latest_version\": \"2.28.5\"},"
                + "{\"series\": \"2.27.x\", \"phase\": \"archive\", \"latest_version\": \"2.27\"},"
                + "42, null, \"text\"],"
                + "\"advisories\": []}");
        assertEquals(1, feed.getSeries().size());
        assertEquals("3.0.x", feed.getSeries().get(0).getSeries());
        assertEquals(6, feed.getWarnings().size());
    }

    @Test
    public void advisoriesAreKeptEvenWhenPartsAreUnreadable() throws Exception {
        Feed feed = parse("{\"schema_version\": 1, \"series\": [], \"advisories\": ["
                + "{\"ghsa_id\": \"not-a-ghsa\", \"cve_id\": \"CVE-2026-1234\", \"severity\": \"nonsense\","
                + " \"vulnerable_versions\": [\"< 2.0.0\", 7, null], \"patched_versions\": {\"2.0.x\": \"2.0.1\","
                + " \"bad\": \"2.0.2\", \"2.1.x\": \"x\"}},"
                + "\"not an object\"]}");
        assertEquals(1, feed.getAdvisories().size());
        Feed.Advisory a = feed.getAdvisories().get(0);
        assertNull(a.getGhsaId());
        assertEquals("CVE-2026-1234", a.getKey());
        assertEquals(Severity.HIGH, a.getSeverity());
        // non-string ranges become empty strings, which the range parser reports as unparseable
        assertEquals(3, a.getVulnerableVersions().size());
        assertEquals("", a.getVulnerableVersions().get(1));
        assertEquals(1, a.getPatchedVersions().size());
        assertEquals(2, feed.getWarnings().size());
    }

    @Test
    public void aVeryDeeplyNestedFeedIsRejectedNotFatal() {
        StringBuilder deep = new StringBuilder("{\"schema_version\": 1, \"series\": [], \"x\": ");
        for (int i = 0; i < 100_000; i++) {
            deep.append('[');
        }
        assertRejected(deep.toString());
    }

    @Test
    public void moderateIsAcceptedAsAnAdvisorySeverity() throws Exception {
        Feed feed = parse("{\"schema_version\": 1, \"series\": [], \"advisories\": [{\"severity\": \"moderate\"}]}");
        assertTrue(feed.getWarnings().isEmpty());
    }

    @Test
    public void advisoryWithNoUsableIdHasAFixedKey() throws Exception {
        Feed feed = parse("{\"schema_version\": 1, \"series\": [], \"advisories\": [{}]}");
        assertEquals("unidentified", feed.getAdvisories().get(0).getKey());
    }

    @Test
    public void longStringsAreTruncated() throws Exception {
        String longText = "x".repeat(Feed.MAX_STRING_LENGTH + 500);
        Feed feed = parse("{\"schema_version\": 1, \"series\": [], \"advisories\": ["
                + "{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\", \"severity\": \"low\", \"summary\": \"" + longText + "\"}]}");
        assertEquals(
                Feed.MAX_STRING_LENGTH, feed.getAdvisories().get(0).getSummary().length());
    }

    @Test
    public void linksAreRestrictedToAllowedHttpsHosts() throws Exception {
        Feed feed = parse("{\"schema_version\": 1, \"series\": ["
                + series("1.0.x", "https://evil.example.com/x") + ","
                + series("1.1.x", "http://github.com/geoserver/geoserver/releases") + ","
                + series("1.2.x", "javascript:alert(1)") + ","
                + series("1.3.x", "https://github.com.evil.example/x") + ","
                + series("1.4.x", "https://user@github.com/x") + ","
                + series("1.5.x", "https://geoserver.org/announcements/x.html") + ","
                + series("1.6.x", "https://CDN.JSDELIVR.NET/gh/geoserver/geoserver@v1/versions.json") + ","
                + series("1.7.x", "https://github.com/someone-else/repo/releases") + ","
                + series("1.8.x", "https://github.com/geoserver/../evil/x") + ","
                + series("1.9.x", "https://github.com/geoserver/%2e%2e/evil") + ","
                + series("2.0.x", "https://cdn.jsdelivr.net/gh/someone-else/repo@main/versions.json") + "]}");
        assertNull(feed.findSeries("1.0.x").getReleaseUrl());
        assertNull(feed.findSeries("1.1.x").getReleaseUrl());
        assertNull(feed.findSeries("1.2.x").getReleaseUrl());
        assertNull(feed.findSeries("1.3.x").getReleaseUrl());
        assertNull(feed.findSeries("1.4.x").getReleaseUrl());
        assertEquals(
                "https://geoserver.org/announcements/x.html",
                feed.findSeries("1.5.x").getReleaseUrl());
        assertNotNull(feed.findSeries("1.6.x").getReleaseUrl());
        for (String other : new String[] {"1.7.x", "1.8.x", "1.9.x", "2.0.x"}) {
            assertNull(other, feed.findSeries(other).getReleaseUrl());
        }
    }

    private static String series(String key, String url) {
        return "{\"series\": \"" + key + "\", \"phase\": \"archive\", \"latest_version\": \"" + key.replace(".x", ".0")
                + "\", \"release_url\": \"" + url + "\"}";
    }

    @Test
    public void ghsaLinksAreBuiltFromValidatedIdsOnly() {
        assertEquals(
                "https://github.com/geoserver/geoserver/security/advisories/GHSA-jvpx-6qxg-whgc",
                Feed.ghsaLink("GHSA-jvpx-6qxg-whgc"));
        assertNull(Feed.ghsaLink("GHSA-jvpx-6qxg-whgc/../../evil"));
        assertNull(Feed.ghsaLink("javascript:alert(1)"));
        assertNull(Feed.ghsaLink(null));
        assertTrue(Feed.isCveId("CVE-2026-76904"));
        assertFalse(Feed.isCveId("CVE-26-1"));
    }
}

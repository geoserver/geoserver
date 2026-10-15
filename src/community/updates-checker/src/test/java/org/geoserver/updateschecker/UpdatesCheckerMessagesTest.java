/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class UpdatesCheckerMessagesTest {

    @Test
    public void placeholdersAreReplacedAndNamedOnesAreLeftAlone() {
        assertEquals("GeoServer 3.0.1 is up to date.", UpdatesCheckerMessages.base("rest.upToDate", "3.0.1"));
        assertEquals(
                "Your series (2.27.x) is in maintenance. The stable series 3.0.x (GeoServer 3.0.1) is available.",
                UpdatesCheckerMessages.base("notification.maintenance", "2.27.x", "3.0.x", "3.0.1"));
        // quotes and braces have no special meaning
        assertEquals(
                "GeoServer's version could not be determined, so update checks are disabled.",
                UpdatesCheckerMessages.base("notification.versionUnknown"));
    }

    @Test
    public void anUndefinedKeyIsReturnedAsItIs() {
        assertEquals("no.such.key", UpdatesCheckerMessages.base("no.such.key"));
        assertEquals("no.such.key", UpdatesCheckerMessages.get(Locale.FRENCH, "no.such.key", "x"));
    }

    @Test
    public void aTranslationIsUsedForItsLanguageAndTheBaseTextForEverythingElse() {
        Locale zz = new Locale("zz");
        assertEquals("zz: GeoServer 1.2.3", UpdatesCheckerMessages.get(zz, "rest.upToDate", "1.2.3"));
        assertEquals("ZZWARN", UpdatesCheckerMessages.get(zz, "severity.WARN"));
        // a key the translation does not have falls back to the base text
        assertEquals("CRITICAL", UpdatesCheckerMessages.get(zz, "severity.CRITICAL"));
        // another language, and no language, get the base text, whatever the JVM's default language is
        assertEquals(
                "GeoServer 1.2.3 is up to date.", UpdatesCheckerMessages.get(Locale.GERMAN, "rest.upToDate", "1.2.3"));
        assertEquals("GeoServer 1.2.3 is up to date.", UpdatesCheckerMessages.get(null, "rest.upToDate", "1.2.3"));
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(zz);
            assertEquals(
                    "GeoServer 1.2.3 is up to date.",
                    UpdatesCheckerMessages.get(Locale.GERMAN, "rest.upToDate", "1.2.3"));
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    public void nestedTextIsResolvedInTheSameLanguage() {
        UpdatesCheckerText text = UpdatesCheckerText.of(
                "notification.cve.confirmed", "CVE-1", UpdatesCheckerText.of("advisory.severity.high"), "summary");
        assertEquals("CVE-1 (high): summary", text.resolve(Locale.ROOT));
        assertEquals("plain", UpdatesCheckerText.literal("plain").resolve(new Locale("zz")));
    }

    @Test
    public void aNotificationFollowsTheLanguageOfTheReader() throws Exception {
        UpdatesCheckerFeed feed = new UpdatesCheckerFeedParser()
                .parse(("{\"schema_version\": 1, \"series\": [{\"series\": \"3.0.x\", \"phase\": \"stable\","
                                + " \"latest_version\": \"3.0.1\"}]}")
                        .getBytes(StandardCharsets.UTF_8));
        UpdatesCheckerNotification n = UpdatesCheckerEvaluator.evaluate(
                        UpdatesCheckerRunningVersion.of("3.0.0"), feed, true)
                .get(0);
        assertEquals("You are running GeoServer 3.0.0. GeoServer 3.0.1 is now available.", n.getMessage());
        assertEquals("zz patch 3.0.0 to 3.0.1", n.getMessage(new Locale("zz")));
    }

    // ---- every message key used by the code exists -----------------------------------------------------------

    private static final Pattern KEY = Pattern.compile(
            "\"((?:severity|advisory|notification|status|rest|log|fetch|feed)\\.[A-Za-z0-9_.]*[A-Za-z0-9_])\"");

    @Test
    public void everyKeyNamedInTheSourceIsDefined() throws Exception {
        Path sources =
                new File(System.getProperty("basedir", System.getProperty("user.dir")), "src/main/java").toPath();
        List<String> missing = new ArrayList<>();
        int found = 0;
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList())) {
                Matcher m = KEY.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
                while (m.find()) {
                    found++;
                    if (UpdatesCheckerMessages.base(m.group(1)).equals(m.group(1))) {
                        missing.add(file.getFileName() + ": " + m.group(1));
                    }
                }
            }
        }
        assertTrue("the scan must find the keys", found > 30);
        assertEquals(
                "keys used in the code but missing from GeoServerApplication.properties",
                Collections.emptyList(),
                missing);
    }

    @Test
    public void keysBuiltFromAnEnumAreDefinedForEveryConstant() {
        for (UpdatesCheckerSeverity s : UpdatesCheckerSeverity.values()) {
            assertFalse(
                    s.name(),
                    UpdatesCheckerMessages.base("severity." + s.name()).startsWith("severity."));
        }
        for (UpdatesCheckerService.Trigger t : UpdatesCheckerService.Trigger.values()) {
            assertFalse(
                    t.name(),
                    UpdatesCheckerMessages.base("log.trigger." + t.name()).startsWith("log."));
        }
        for (String phase : new String[] {"stable", "maintenance", "archive"}) {
            assertFalse(
                    phase, UpdatesCheckerMessages.base("status.phase." + phase).startsWith("status."));
        }
    }

    @Test
    public void everyActionLabelIsDefinedInTheSettingsPageBundle() throws Exception {
        File file = new File(
                System.getProperty("basedir", System.getProperty("user.dir")),
                "src/main/java/GeoServerApplication.properties");
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        for (UpdatesCheckerNotification.Action a : UpdatesCheckerNotification.Action.values()) {
            assertTrue(a.getMessageKey(), text.contains("\n" + a.getMessageKey() + "="));
        }
    }

    @Test
    public void feedProblemsAndFailureReasonsAreMessagesToo() throws Exception {
        UpdatesCheckerFeed feed = new UpdatesCheckerFeedParser()
                .parse("{\"schema_version\": 1, \"series\": [42], \"advisories\": [\"x\"]}"
                        .getBytes(StandardCharsets.UTF_8));
        assertEquals(2, feed.getWarnings().size());
        assertEquals(
                "series entry 0 is unreadable and was ignored",
                feed.getWarnings().get(0).base());
        assertEquals(
                "advisory entry 0 is not an object and was ignored",
                feed.getWarnings().get(1).base());
        try {
            new UpdatesCheckerFeedParser().parse("{\"schema_version\": 1,".getBytes(StandardCharsets.UTF_8));
            org.junit.Assert.fail("not JSON");
        } catch (UpdatesCheckerFeedException e) {
            assertEquals("the feed is not valid JSON", e.getReason().base());
            assertEquals(e.getReason().base(), e.getMessage());
        }
        assertEquals(
                "Version feed returned HTTP 500",
                UpdatesCheckerText.of("fetch.error.http", "500").base());
    }

    @Test
    public void anArgumentIsNeverExpandedByALaterOne() {
        // notification.cve.confirmed is "{0} ({1}): {2}"; the first argument carries a placeholder of its own
        assertEquals(
                "{2} (high): summary",
                UpdatesCheckerMessages.base("notification.cve.confirmed", "{2}", "high", "summary"));
        assertEquals("a\\b (x): $1", UpdatesCheckerMessages.base("notification.cve.confirmed", "a\\b", "x", "$1"));
    }

    @Test
    public void emphasisIsKeptInHtmlAndDroppedInPlainTextAndValuesAreEscaped() {
        String hostile = "<script>alert(1)</script> & \"quotes\"";
        assertEquals(
                "CVE-1 (critical): Summary Version range could not be determined automatically. Review manually",
                UpdatesCheckerMessages.base("notification.cve.unverifiable", "CVE-1", "critical", "Summary"));
        assertEquals(
                "CVE-1 (critical): Summary <i>Version range could not be determined automatically.</i> <b>Review manually</b>",
                UpdatesCheckerMessages.getHtml(
                        Locale.ROOT, "notification.cve.unverifiable", "CVE-1", "critical", "Summary"));
        String html = UpdatesCheckerMessages.getHtml(
                Locale.ROOT, "notification.cve.unverifiable", "CVE-1", "critical", hostile);
        assertTrue(html, html.contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;quotes&quot;"));
        assertFalse(html, html.contains("<script>"));
        // tags that a value carries are values, not markup, in plain text as well
        String plain = UpdatesCheckerMessages.base("notification.cve.unverifiable", "CVE-1", "critical", "<b>x</b>");
        assertTrue(plain, plain.contains("<b>x</b>"));
    }
}

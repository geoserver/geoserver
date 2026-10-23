/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the version feed JSON into an {@link UpdatesCheckerFeed}, through {@link FeedJson}, the only class that touches
 * the JSON library, so a later move to another one is a change there.
 *
 * <p>The feed is untrusted: array lengths and string lengths are capped, and every entry that cannot be read exactly is
 * dropped and reported in {@link UpdatesCheckerFeed#getWarnings()}. Advisories are the exception to "drop what is
 * unclear": an advisory is kept even when parts of it are unreadable, so the evaluator can still flag it for manual
 * review.
 */
public final class UpdatesCheckerFeedParser {

    /** The highest feed {@code schema_version} this module understands. */
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    static final int MAX_SERIES = 100;

    static final int MAX_ADVISORIES = 500;

    static final int MAX_RECENT_RELEASES_PER_SERIES = 50;

    static final int MAX_RECENT_RELEASES = 300;

    static final int MAX_RANGES_PER_ADVISORY = 50;

    /**
     * @return the parsed feed. For a schema newer than {@link #SUPPORTED_SCHEMA_VERSION} the result is flagged (see
     *     {@link UpdatesCheckerFeed#isSchemaTooNew()}) and carries no series or advisories.
     * @throws UpdatesCheckerFeedException if the body is not a usable feed
     */
    public UpdatesCheckerFeed parse(byte[] body) throws UpdatesCheckerFeedException {
        FeedJson root;
        try {
            root = FeedJson.parse(body);
        } catch (RuntimeException | StackOverflowError e) {
            // a StackOverflowError is a feed nested deeper than the parser can follow
            throw new UpdatesCheckerFeedException(UpdatesCheckerText.of("feed.error.notJson"), e);
        }
        if (root == null || !root.isObject()) {
            throw new UpdatesCheckerFeedException(UpdatesCheckerText.of("feed.error.notObject"));
        }
        FeedJson schemaNode = root.get("schema_version");
        if (schemaNode == null || !schemaNode.isInt()) {
            throw new UpdatesCheckerFeedException(UpdatesCheckerText.of("feed.error.noSchemaVersion"));
        }
        int schemaVersion = schemaNode.asInt();
        if (schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            return new UpdatesCheckerFeed(
                    schemaVersion,
                    Collections.<UpdatesCheckerFeed.Series>emptyList(),
                    Collections.<UpdatesCheckerFeed.Advisory>emptyList(),
                    Collections.<UpdatesCheckerText>emptyList());
        }
        if (schemaVersion < 1) {
            throw new UpdatesCheckerFeedException(
                    UpdatesCheckerText.of("feed.error.badSchemaVersion", String.valueOf(schemaVersion)));
        }
        FeedJson seriesNode = root.get("series");
        if (seriesNode == null || !seriesNode.isArray()) {
            throw new UpdatesCheckerFeedException(UpdatesCheckerText.of("feed.error.noSeries"));
        }
        List<UpdatesCheckerText> warnings = new ArrayList<>();
        List<UpdatesCheckerFeed.Series> series = readSeries(seriesNode, warnings);
        List<UpdatesCheckerFeed.Advisory> advisories = readAdvisories(root.get("advisories"), warnings);
        return new UpdatesCheckerFeed(schemaVersion, series, advisories, warnings);
    }

    private List<UpdatesCheckerFeed.Series> readSeries(FeedJson array, List<UpdatesCheckerText> warnings) {
        List<UpdatesCheckerFeed.Series> result = new ArrayList<>();
        if (array.size() > MAX_SERIES) {
            warnings.add(UpdatesCheckerText.of(
                    "feed.warning.tooManySeries", String.valueOf(array.size()), String.valueOf(MAX_SERIES)));
        }
        int[] recentBudget = {MAX_RECENT_RELEASES};
        for (int i = 0; i < array.size() && i < MAX_SERIES; i++) {
            FeedJson node = array.get(i);
            String key = text(node, "series");
            UpdatesCheckerFeed.Phase phase = phase(text(node, "phase"));
            UpdatesCheckerVersion latest = UpdatesCheckerVersion.parse(text(node, "latest_version"));
            if (!UpdatesCheckerVersion.isSeries(key) || phase == null || latest == null) {
                warnings.add(UpdatesCheckerText.of("feed.warning.seriesIgnored", String.valueOf(i)));
                continue;
            }
            result.add(new UpdatesCheckerFeed.Series(
                    key,
                    phase,
                    latest,
                    UpdatesCheckerFeedText.safeLink(text(node, "release_url")),
                    readRelease(node, latest),
                    readRecentReleases(node, key, recentBudget, warnings)));
        }
        return result;
    }

    /** The release signal of an entry: its blog link and its two flags, each false or null when missing or invalid. */
    private static UpdatesCheckerFeed.Release readRelease(FeedJson node, UpdatesCheckerVersion version) {
        return new UpdatesCheckerFeed.Release(
                version,
                UpdatesCheckerFeedText.safeLink(text(node, "blog_url")),
                flag(node, "security_flagged"),
                flag(node, "synchronized_release"));
    }

    private static boolean flag(FeedJson node, String name) {
        FeedJson value = node.get(name);
        return value != null && value.isBoolean() && value.booleanValue();
    }

    /**
     * @return the entries of {@code recent_releases} that belong to the series, or {@code null} if the series has no
     *     such list; the list is capped per series and for the whole feed
     */
    private static List<UpdatesCheckerFeed.Release> readRecentReleases(
            FeedJson series, String seriesKey, int[] budget, List<UpdatesCheckerText> warnings) {
        FeedJson array = series.get("recent_releases");
        if (array == null || !array.isArray()) {
            return null;
        }
        List<UpdatesCheckerFeed.Release> releases = new ArrayList<>();
        int limit = Math.min(MAX_RECENT_RELEASES_PER_SERIES, budget[0]);
        if (array.size() > limit) {
            warnings.add(UpdatesCheckerText.of(
                    "feed.warning.tooManyReleases", seriesKey, String.valueOf(array.size()), String.valueOf(limit)));
        }
        for (int i = 0; i < array.size() && releases.size() < limit; i++) {
            FeedJson entry = array.get(i);
            UpdatesCheckerVersion version = entry == null ? null : UpdatesCheckerVersion.parse(text(entry, "version"));
            if (version == null || !seriesKey.equals(version.getSeries())) {
                warnings.add(UpdatesCheckerText.of("feed.warning.releaseIgnored", seriesKey, String.valueOf(i)));
                continue;
            }
            releases.add(readRelease(entry, version));
        }
        budget[0] -= releases.size();
        return releases;
    }

    private List<UpdatesCheckerFeed.Advisory> readAdvisories(FeedJson array, List<UpdatesCheckerText> warnings) {
        List<UpdatesCheckerFeed.Advisory> result = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return result;
        }
        if (array.size() > MAX_ADVISORIES) {
            warnings.add(UpdatesCheckerText.of(
                    "feed.warning.tooManyAdvisories", String.valueOf(array.size()), String.valueOf(MAX_ADVISORIES)));
        }
        for (int i = 0; i < array.size() && i < MAX_ADVISORIES; i++) {
            FeedJson node = array.get(i);
            if (node == null || !node.isObject()) {
                warnings.add(UpdatesCheckerText.of("feed.warning.advisoryIgnored", String.valueOf(i)));
                continue;
            }
            String ghsa = text(node, "ghsa_id");
            String cve = text(node, "cve_id");
            String severityText = text(node, "severity");
            UpdatesCheckerSeverity severity = UpdatesCheckerSeverity.fromAdvisory(severityText);
            if (severityText == null || !isKnownSeverity(severityText)) {
                warnings.add(UpdatesCheckerText.of("feed.warning.severityUnknown", String.valueOf(i)));
            }
            String summary = text(node, "summary");
            result.add(new UpdatesCheckerFeed.Advisory(
                    UpdatesCheckerFeedText.isGhsaId(ghsa) ? ghsa : null,
                    UpdatesCheckerFeedText.isCveId(cve) ? cve : null,
                    summary == null ? "" : summary,
                    severity,
                    readRanges(node.get("vulnerable_versions"), i, warnings),
                    readPatched(node.get("patched_versions"))));
        }
        return result;
    }

    private List<String> readRanges(FeedJson array, int advisoryIndex, List<UpdatesCheckerText> warnings) {
        List<String> ranges = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return ranges;
        }
        if (array.size() > MAX_RANGES_PER_ADVISORY) {
            warnings.add(UpdatesCheckerText.of(
                    "feed.warning.tooManyRanges",
                    String.valueOf(advisoryIndex),
                    String.valueOf(array.size()),
                    String.valueOf(MAX_RANGES_PER_ADVISORY)));
        }
        for (int i = 0; i < array.size() && i < MAX_RANGES_PER_ADVISORY; i++) {
            FeedJson item = array.get(i);
            // a non-string entry becomes an empty string, which the range parser reports as unparseable
            ranges.add(item != null && item.isTextual() ? UpdatesCheckerFeedText.truncate(item.textValue()) : "");
        }
        return ranges;
    }

    private Map<String, UpdatesCheckerVersion> readPatched(FeedJson object) {
        Map<String, UpdatesCheckerVersion> patched = new LinkedHashMap<>();
        if (object == null || !object.isObject()) {
            return patched;
        }
        Iterator<String> names = object.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            FeedJson value = object.get(name);
            UpdatesCheckerVersion version =
                    value != null && value.isTextual() ? UpdatesCheckerVersion.parse(value.textValue()) : null;
            if (UpdatesCheckerVersion.isSeries(name) && version != null) {
                patched.put(name, version);
            }
        }
        return patched;
    }

    private static boolean isKnownSeverity(String severity) {
        switch (severity.trim().toLowerCase(Locale.ROOT)) {
            case "critical":
            case "high":
            case "medium":
            case "moderate":
            case "low":
                return true;
            default:
                return false;
        }
    }

    private static UpdatesCheckerFeed.Phase phase(String text) {
        if (text == null) {
            return null;
        }
        switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "stable":
                return UpdatesCheckerFeed.Phase.STABLE;
            case "maintenance":
                return UpdatesCheckerFeed.Phase.MAINTENANCE;
            case "archive":
                return UpdatesCheckerFeed.Phase.ARCHIVE;
            default:
                return null;
        }
    }

    /** @return the child as text, truncated, or {@code null} if absent or not a string. */
    private static String text(FeedJson parent, String name) {
        FeedJson child = parent == null ? null : parent.get(name);
        if (child == null || !child.isTextual()) {
            return null;
        }
        return UpdatesCheckerFeedText.truncate(child.textValue());
    }
}

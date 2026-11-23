/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * The version feed: the latest release of each series and the published security advisories. Immutable.
 *
 * <p>The feed is untrusted. Strings are truncated, links must point to the GeoServer project, and an entry that cannot
 * be read is dropped with a warning. An advisory is kept even when parts of it are unreadable, so it can be flagged for
 * manual review.
 */
public final class Feed {

    /** The highest feed {@code schema_version} this module understands. */
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    /** Longest feed string kept. */
    public static final int MAX_STRING_LENGTH = 2000;

    /** The hosts links may point to, with the path a link on that host must start with. */
    private static final Map<String, String> ALLOWED_LINK_PATHS = Map.of(
            "geoserver.org", "/",
            "github.com", "/geoserver/",
            "cdn.jsdelivr.net", "/gh/geoserver/");

    private static final Pattern GHSA_ID = Pattern.compile("^GHSA-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}$");

    private static final Pattern CVE_ID = Pattern.compile("^CVE-\\d{4}-\\d{4,7}$");

    /** org.json takes seconds to read a number this long; a 1 MiB one takes about 40 seconds. */
    private static final Pattern HUGE_NUMBER = Pattern.compile("\\d{64}");

    /** Lifecycle phase of a release series. */
    public enum Phase {
        STABLE,
        MAINTENANCE,
        ARCHIVE
    }

    /** One release of a series, with the feed's signals that it may be a security release. */
    public static final class Release {
        private final Version version;
        private final String blogUrl;
        private final boolean securityFlagged;
        private final boolean synchronizedRelease;

        public Release(Version version, String blogUrl, boolean securityFlagged, boolean synchronizedRelease) {
            this.version = version;
            this.blogUrl = blogUrl;
            this.securityFlagged = securityFlagged;
            this.synchronizedRelease = synchronizedRelease;
        }

        public Version getVersion() {
            return version;
        }

        /** @return the announcement on geoserver.org, or {@code null} */
        public String getBlogUrl() {
            return blogUrl;
        }

        public boolean isSecurityFlagged() {
            return securityFlagged;
        }

        /** @return whether several series were released together, a probable sign of a security release */
        public boolean isSynchronizedRelease() {
            return synchronizedRelease;
        }
    }

    /** One release series. */
    public static final class Series {
        private final String series;
        private final Phase phase;
        private final Version latestVersion;
        private final String releaseUrl;
        private final Release latestRelease;
        private final List<Release> recentReleases;

        /** @param recentReleases the releases of the recent window, or {@code null} if the feed does not list them */
        public Series(
                String series,
                Phase phase,
                Version latestVersion,
                String releaseUrl,
                Release latestRelease,
                List<Release> recentReleases) {
            this.series = series;
            this.phase = phase;
            this.latestVersion = latestVersion;
            this.releaseUrl = releaseUrl;
            this.latestRelease = latestRelease;
            this.recentReleases = recentReleases == null ? null : Collections.unmodifiableList(recentReleases);
        }

        /** @return the series key, such as {@code 2.27.x} */
        public String getSeries() {
            return series;
        }

        public Phase getPhase() {
            return phase;
        }

        public Version getLatestVersion() {
            return latestVersion;
        }

        /** @return the release page, or {@code null} */
        public String getReleaseUrl() {
            return releaseUrl;
        }

        /** @return the series-level signals, which describe the latest release */
        public Release getLatestRelease() {
            return latestRelease;
        }

        /** @return the releases of the recent window, or {@code null} if the feed does not list them */
        public List<Release> getRecentReleases() {
            return recentReleases;
        }
    }

    /** A published security advisory. */
    public static final class Advisory {
        private final String ghsaId;
        private final String cveId;
        private final String summary;
        private final Severity severity;
        private final List<String> vulnerableVersions;
        private final Map<String, Version> patchedVersions;

        public Advisory(
                String ghsaId,
                String cveId,
                String summary,
                Severity severity,
                List<String> vulnerableVersions,
                Map<String, Version> patchedVersions) {
            this.ghsaId = ghsaId;
            this.cveId = cveId;
            this.summary = summary;
            this.severity = severity;
            this.vulnerableVersions = Collections.unmodifiableList(vulnerableVersions);
            this.patchedVersions = Collections.unmodifiableMap(new LinkedHashMap<>(patchedVersions));
        }

        /** @return the GHSA id, or {@code null} if the feed gave none that is well formed */
        public String getGhsaId() {
            return ghsaId;
        }

        /** @return the CVE id, or {@code null} if the feed gave none that is well formed */
        public String getCveId() {
            return cveId;
        }

        public String getSummary() {
            return summary;
        }

        public Severity getSeverity() {
            return severity;
        }

        /** @return the version-range strings, in feed order */
        public List<String> getVulnerableVersions() {
            return vulnerableVersions;
        }

        /** @return the first patched version of each series, keyed by series */
        public Map<String, Version> getPatchedVersions() {
            return patchedVersions;
        }

        /** @return the id a dismissal is remembered by: the GHSA id, else the CVE id, else a fixed word */
        public String getKey() {
            if (ghsaId != null) {
                return ghsaId;
            }
            return cveId != null ? cveId : "unidentified";
        }
    }

    private final int schemaVersion;
    private final List<Series> series;
    private final List<Advisory> advisories;
    private final List<Text> warnings;

    public Feed(int schemaVersion, List<Series> series, List<Advisory> advisories, List<Text> warnings) {
        this.schemaVersion = schemaVersion;
        this.series = Collections.unmodifiableList(series);
        this.advisories = Collections.unmodifiableList(advisories);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    /**
     * @return the feed; a newer schema gives one with no series or advisories, see {@link #isSchemaTooNew()}
     * @throws FeedException if the body is not a usable feed
     */
    public static Feed parse(byte[] body) throws FeedException {
        Object root;
        try {
            root = parseJson(new String(body, StandardCharsets.UTF_8));
        } catch (JSONException | StackOverflowError e) {
            // a StackOverflowError is a feed nested deeper than the parser can follow
            throw new FeedException(Text.of("feed.error.notJson"), e);
        }
        if (!(root instanceof JSONObject)) {
            throw new FeedException(Text.of("feed.error.notObject"));
        }
        JSONObject json = (JSONObject) root;
        Object schema = json.opt("schema_version");
        if (!(schema instanceof Integer)) {
            throw new FeedException(Text.of("feed.error.noSchemaVersion"));
        }
        int schemaVersion = (Integer) schema;
        if (schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            return new Feed(schemaVersion, List.of(), List.of(), List.of());
        }
        if (schemaVersion < 1) {
            throw new FeedException(Text.of("feed.error.badSchemaVersion", String.valueOf(schemaVersion)));
        }
        JSONArray seriesArray = json.optJSONArray("series");
        if (seriesArray == null) {
            throw new FeedException(Text.of("feed.error.noSeries"));
        }
        List<Text> warnings = new ArrayList<>();
        List<Series> series = readSeries(seriesArray, warnings);
        List<Advisory> advisories = readAdvisories(json.optJSONArray("advisories"), warnings);
        return new Feed(schemaVersion, series, advisories, warnings);
    }

    /**
     * @return the single JSON value in the text
     * @throws JSONException if the text is not exactly one JSON value, repeats a key, holds a NUL character (which
     *     org.json reads as the end of its input) or holds a huge number
     */
    static Object parseJson(String text) {
        if (text.indexOf('\0') >= 0 || HUGE_NUMBER.matcher(text).find()) {
            throw new JSONException("Not a feed");
        }
        JSONTokener tokens = new JSONTokener(text);
        Object value = tokens.nextValue();
        if (tokens.nextClean() != 0) {
            throw new JSONException("Text after the JSON value");
        }
        return value;
    }

    private static List<Series> readSeries(JSONArray array, List<Text> warnings) {
        List<Series> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject node = array.optJSONObject(i);
            String key = text(node, "series");
            Phase phase = phase(text(node, "phase"));
            Version latest = Version.parse(text(node, "latest_version"));
            if (!Version.isSeries(key) || phase == null || latest == null) {
                warnings.add(Text.of("feed.warning.seriesIgnored", String.valueOf(i)));
                continue;
            }
            result.add(new Series(
                    key,
                    phase,
                    latest,
                    safeLink(text(node, "release_url")),
                    readRelease(node, latest),
                    readRecentReleases(node, key, warnings)));
        }
        return result;
    }

    private static Release readRelease(JSONObject node, Version version) {
        return new Release(
                version,
                safeLink(text(node, "blog_url")),
                Boolean.TRUE.equals(node.opt("security_flagged")),
                Boolean.TRUE.equals(node.opt("synchronized_release")));
    }

    /** @return the entries of {@code recent_releases} that belong to the series, or {@code null} if there is no list */
    private static List<Release> readRecentReleases(JSONObject series, String seriesKey, List<Text> warnings) {
        JSONArray array = series.optJSONArray("recent_releases");
        if (array == null) {
            return null;
        }
        List<Release> releases = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.optJSONObject(i);
            Version version = Version.parse(text(entry, "version"));
            if (version == null || !seriesKey.equals(version.getSeries())) {
                warnings.add(Text.of("feed.warning.releaseIgnored", seriesKey, String.valueOf(i)));
                continue;
            }
            releases.add(readRelease(entry, version));
        }
        return releases;
    }

    private static List<Advisory> readAdvisories(JSONArray array, List<Text> warnings) {
        List<Advisory> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject node = array.optJSONObject(i);
            if (node == null) {
                warnings.add(Text.of("feed.warning.advisoryIgnored", String.valueOf(i)));
                continue;
            }
            String ghsa = text(node, "ghsa_id");
            String cve = text(node, "cve_id");
            String severity = text(node, "severity");
            if (severity == null || !Severity.isAdvisorySeverity(severity)) {
                warnings.add(Text.of("feed.warning.severityUnknown", String.valueOf(i)));
            }
            String summary = text(node, "summary");
            result.add(new Advisory(
                    isGhsaId(ghsa) ? ghsa : null,
                    isCveId(cve) ? cve : null,
                    summary == null ? "" : summary,
                    Severity.fromAdvisory(severity),
                    readRanges(node.optJSONArray("vulnerable_versions")),
                    readPatched(node.optJSONObject("patched_versions"))));
        }
        return result;
    }

    /** A non-string entry becomes an empty string, which {@link VersionRange} reports as unparseable. */
    private static List<String> readRanges(JSONArray array) {
        List<String> ranges = new ArrayList<>();
        if (array == null) {
            return ranges;
        }
        for (int i = 0; i < array.length(); i++) {
            Object item = array.opt(i);
            ranges.add(item instanceof String ? truncate((String) item) : "");
        }
        return ranges;
    }

    private static Map<String, Version> readPatched(JSONObject object) {
        Map<String, Version> patched = new LinkedHashMap<>();
        if (object == null) {
            return patched;
        }
        for (String name : object.keySet()) {
            Version version = Version.parse(text(object, name));
            if (Version.isSeries(name) && version != null) {
                patched.put(name, version);
            }
        }
        return patched;
    }

    private static Phase phase(String text) {
        if (text == null) {
            return null;
        }
        switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "stable":
                return Phase.STABLE;
            case "maintenance":
                return Phase.MAINTENANCE;
            case "archive":
                return Phase.ARCHIVE;
            default:
                return null;
        }
    }

    /** @return the member as truncated text, or {@code null} if it is absent or not a string */
    private static String text(JSONObject parent, String name) {
        Object value = parent == null ? null : parent.opt(name);
        return value instanceof String ? truncate((String) value) : null;
    }

    /** @return the text cut to {@link #MAX_STRING_LENGTH} characters */
    public static String truncate(String text) {
        if (text == null || text.length() <= MAX_STRING_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_STRING_LENGTH);
    }

    /**
     * @return the link if it is {@code https}, has no user info, and points to geoserver.org or to the
     *     {@code geoserver} organisation on GitHub or jsDelivr, otherwise {@code null}
     */
    public static String safeLink(String link) {
        if (link == null) {
            return null;
        }
        try {
            URI uri = new URI(link.trim());
            String host = uri.getHost();
            String prefix = host == null ? null : ALLOWED_LINK_PATHS.get(host.toLowerCase(Locale.ROOT));
            String path = uri.getPath();
            if ("https".equalsIgnoreCase(uri.getScheme())
                    && prefix != null
                    && uri.getUserInfo() == null
                    && path != null
                    && path.startsWith(prefix)
                    && !path.contains("/../")
                    && !path.contains("/./")
                    && !path.endsWith("/..")
                    && !path.endsWith("/.")) {
                return uri.toASCIIString();
            }
        } catch (Exception e) {
            // not a usable link
        }
        return null;
    }

    /** @return whether the text is a GitHub advisory id, such as {@code GHSA-jvpx-6qxg-whgc} */
    public static boolean isGhsaId(String text) {
        return text != null && GHSA_ID.matcher(text).matches();
    }

    /** @return whether the text is a CVE id, such as {@code CVE-2026-76904} */
    public static boolean isCveId(String text) {
        return text != null && CVE_ID.matcher(text).matches();
    }

    /** @return the advisory page, built from the id and never taken from the feed */
    public static String ghsaLink(String ghsaId) {
        return isGhsaId(ghsaId) ? "https://github.com/geoserver/geoserver/security/advisories/" + ghsaId : null;
    }

    /** @return whether the schema is newer than this module understands; then nothing else is filled in */
    public boolean isSchemaTooNew() {
        return schemaVersion > SUPPORTED_SCHEMA_VERSION;
    }

    public List<Series> getSeries() {
        return series;
    }

    public List<Advisory> getAdvisories() {
        return advisories;
    }

    /** @return the entries that were dropped while reading */
    public List<Text> getWarnings() {
        return warnings;
    }

    /** @return the entry for a series key, or {@code null} */
    public Series findSeries(String key) {
        for (Series s : series) {
            if (s.getSeries().equals(key)) {
                return s;
            }
        }
        return null;
    }

    /** @return the series of the given phase with the highest number, or {@code null} */
    public Series findNewest(Phase phase) {
        Series best = null;
        for (Series s : series) {
            if (s.getPhase() == phase && (best == null || Version.compareSeries(s.getSeries(), best.getSeries()) > 0)) {
                best = s;
            }
        }
        return best;
    }
}

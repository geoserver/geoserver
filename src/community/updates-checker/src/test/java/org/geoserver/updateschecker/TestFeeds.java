/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

/** Builds feed JSON for tests. */
public final class TestFeeds {

    private TestFeeds() {}

    /** @return a series entry: {@code phase} is {@code stable}, {@code maintenance} or {@code archive}. */
    public static String series(String key, String phase, String latest, boolean synchronizedRelease) {
        return "{\"series\": \"" + key + "\", \"phase\": \"" + phase + "\", \"latest_version\": \"" + latest
                + "\", \"synchronized_release\": " + synchronizedRelease
                + ", \"release_url\": \"https://github.com/geoserver/geoserver/releases/tag/" + latest + "\"}";
    }

    /**
     * @param ranges the {@code vulnerable_versions} entries, as JSON strings separated by commas
     * @param patched the {@code patched_versions} members, as JSON, separated by commas
     */
    public static String advisory(String ghsa, String severity, String ranges, String patched) {
        return "{\"ghsa_id\": \"" + ghsa + "\", \"cve_id\": \"CVE-2026-0001\", \"summary\": \"Summary text\","
                + " \"severity\": \"" + severity + "\", \"vulnerable_versions\": [" + ranges
                + "], \"patched_versions\": {" + patched + "}}";
    }

    /** @return a version 1 feed with these series entries and the advisories (JSON separated by commas, or empty). */
    public static String feed(String advisories, String... series) {
        return "{\"schema_version\": 1, \"series\": [" + String.join(",", series) + "], \"advisories\": [" + advisories
                + "]}";
    }

    /** @return an entry of {@code recent_releases}; {@code blogUrl} may be {@code null}. */
    public static String release(String version, boolean flagged, boolean synchronizedRelease, String blogUrl) {
        return "{\"version\": \"" + version + "\", \"published_at\": \"2026-08-14T22:44:22Z\", \"blog_url\": "
                + (blogUrl == null ? "null" : "\"" + blogUrl + "\"") + ", \"security_flagged\": " + flagged
                + ", \"synchronized_release\": " + synchronizedRelease + "}";
    }

    /** @return the blog post of a release, as the feed gives it. */
    public static String blog(String version) {
        return "https://geoserver.org/announcements/vulnerability/2026/08/14/geoserver-" + version.replace('.', '-')
                + "-released.html";
    }

    /**
     * @return a series entry that lists its recent releases; the series-level signal repeats the first of them, which
     *     must be the latest
     */
    public static String seriesWithReleases(String key, String phase, String latest, String... releases) {
        String repeated = releases.length == 0
                ? "\"security_flagged\": false, \"synchronized_release\": false"
                : releases[0].substring(releases[0].indexOf("\"security_flagged\""), releases[0].lastIndexOf('}'));
        return "{\"series\": \"" + key + "\", \"phase\": \"" + phase + "\", \"latest_version\": \"" + latest
                + "\", \"release_url\": \"https://github.com/geoserver/geoserver/releases/tag/" + latest + "\", "
                + repeated + ", \"recent_releases\": [" + String.join(",", releases) + "]}";
    }
}

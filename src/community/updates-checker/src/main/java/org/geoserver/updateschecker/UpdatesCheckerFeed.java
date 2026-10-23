/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The parsed version feed: the latest patch of each release series and the published security advisories. Immutable, so
 * one instance can be shared between threads and swapped in atomically.
 */
public final class UpdatesCheckerFeed {

    /** Lifecycle phase of a release series, as declared by the feed. */
    public enum Phase {
        STABLE,
        MAINTENANCE,
        ARCHIVE
    }

    /** One release of a series, with what the feed says about whether it is a security release. */
    public static final class Release {
        private final UpdatesCheckerVersion version;
        private final String blogUrl;
        private final boolean securityFlagged;
        private final boolean synchronizedRelease;

        public Release(
                UpdatesCheckerVersion version, String blogUrl, boolean securityFlagged, boolean synchronizedRelease) {
            this.version = version;
            this.blogUrl = blogUrl;
            this.securityFlagged = securityFlagged;
            this.synchronizedRelease = synchronizedRelease;
        }

        public UpdatesCheckerVersion getVersion() {
            return version;
        }

        /** @return the announcement on geoserver.org, or {@code null} if there is none yet or it is not allowed. */
        public String getBlogUrl() {
            return blogUrl;
        }

        /** @return whether the announcement marks this release as a security release. */
        public boolean isSecurityFlagged() {
            return securityFlagged;
        }

        /** @return whether several series were released together, a probable sign of a security release. */
        public boolean isSynchronizedRelease() {
            return synchronizedRelease;
        }
    }

    /** One release series: its latest patch and the releases of the recent window. */
    public static final class Series {
        private final String series;
        private final Phase phase;
        private final UpdatesCheckerVersion latestVersion;
        private final String releaseUrl;
        private final Release latestRelease;
        private final List<Release> recentReleases;

        /**
         * @param latestRelease what the feed says about {@code latestVersion} at the series level
         * @param recentReleases the releases of the recent window, or {@code null} if the feed does not list them
         */
        public Series(
                String series,
                Phase phase,
                UpdatesCheckerVersion latestVersion,
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

        /** @return the series key, e.g. {@code "2.27.x"}. */
        public String getSeries() {
            return series;
        }

        public Phase getPhase() {
            return phase;
        }

        public UpdatesCheckerVersion getLatestVersion() {
            return latestVersion;
        }

        /** @return the release page, or {@code null} if the feed gave none or one that is not allowed. */
        public String getReleaseUrl() {
            return releaseUrl;
        }

        /** @return the series-level signal, which describes the latest release only. */
        public Release getLatestRelease() {
            return latestRelease;
        }

        /**
         * @return the releases of the recent window, or {@code null} if the feed does not list them (an empty list
         *     means there are none)
         */
        public List<Release> getRecentReleases() {
            return recentReleases;
        }
    }

    /** A published security advisory. */
    public static final class Advisory {
        private final String ghsaId;
        private final String cveId;
        private final String summary;
        private final UpdatesCheckerSeverity severity;
        private final List<String> vulnerableVersions;
        private final Map<String, UpdatesCheckerVersion> patchedVersions;

        public Advisory(
                String ghsaId,
                String cveId,
                String summary,
                UpdatesCheckerSeverity severity,
                List<String> vulnerableVersions,
                Map<String, UpdatesCheckerVersion> patchedVersions) {
            this.ghsaId = ghsaId;
            this.cveId = cveId;
            this.summary = summary;
            this.severity = severity;
            this.vulnerableVersions = Collections.unmodifiableList(vulnerableVersions);
            this.patchedVersions = Collections.unmodifiableMap(new LinkedHashMap<>(patchedVersions));
        }

        /** @return the validated GHSA id, or {@code null} if the feed gave none that is well formed. */
        public String getGhsaId() {
            return ghsaId;
        }

        /** @return the validated CVE id, or {@code null}. */
        public String getCveId() {
            return cveId;
        }

        public String getSummary() {
            return summary;
        }

        /** @return the notification severity this advisory maps to. */
        public UpdatesCheckerSeverity getSeverity() {
            return severity;
        }

        /** @return the raw version-range strings, in feed order. */
        public List<String> getVulnerableVersions() {
            return vulnerableVersions;
        }

        /** @return the first patched version of each series, keyed by series (e.g. {@code "2.27.x"}). */
        public Map<String, UpdatesCheckerVersion> getPatchedVersions() {
            return patchedVersions;
        }

        /**
         * @return the id used to remember a dismissal and to name the advisory: GHSA id, else CVE id, else a fixed
         *     word.
         */
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
    private final List<UpdatesCheckerText> warnings;

    public UpdatesCheckerFeed(
            int schemaVersion, List<Series> series, List<Advisory> advisories, List<UpdatesCheckerText> warnings) {
        this.schemaVersion = schemaVersion;
        this.series = Collections.unmodifiableList(series);
        this.advisories = Collections.unmodifiableList(advisories);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    /** @return whether the feed's schema is newer than this module understands; then nothing else is filled in. */
    public boolean isSchemaTooNew() {
        return schemaVersion > UpdatesCheckerFeedParser.SUPPORTED_SCHEMA_VERSION;
    }

    public List<Series> getSeries() {
        return series;
    }

    public List<Advisory> getAdvisories() {
        return advisories;
    }

    /** @return problems found while reading (entries dropped, limits hit). */
    public List<UpdatesCheckerText> getWarnings() {
        return warnings;
    }

    /** @return the entry for a series key such as {@code "2.27.x"}, or {@code null}. */
    public Series findSeries(String key) {
        for (Series s : series) {
            if (s.getSeries().equals(key)) {
                return s;
            }
        }
        return null;
    }

    /** @return the entry of the given phase with the highest series number, or {@code null}. */
    public Series findNewest(Phase phase) {
        Series best = null;
        for (Series s : series) {
            if (s.getPhase() == phase
                    && (best == null || UpdatesCheckerVersion.compareSeries(s.getSeries(), best.getSeries()) > 0)) {
                best = s;
            }
        }
        return best;
    }
}

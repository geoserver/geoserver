/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.geoserver.updateschecker.UpdatesCheckerNotification.Type;

/**
 * Turns "what is running" and "what the feed says" into the ordered list of notifications to show. Stateless and free
 * of any network, session or UI code, so every rule can be tested directly. All wording comes from
 * {@link UpdatesCheckerMessages}, resolved in the reader's language only when the text is asked for.
 *
 * <p>Order: most urgent severity first; within one severity, security notices before patches before series notices.
 */
public final class UpdatesCheckerEvaluator {

    /** Most urgent severity first; then security, patch and series notices; then the declaration order of the types. */
    private static final Comparator<UpdatesCheckerNotification> PRIORITY = (a, b) -> {
        int c = a.getSeverity().compareTo(b.getSeverity());
        if (c == 0) {
            c = Integer.compare(a.getType().getGroup(), b.getType().getGroup());
        }
        return c != 0 ? c : a.getType().compareTo(b.getType());
    };

    private UpdatesCheckerEvaluator() {}

    /**
     * @param running the version this server runs
     * @param feed the cached feed, or {@code null} if none has been fetched successfully
     * @param fetchAttempted whether a fetch has been tried at least once (a failed first attempt leaves {@code feed}
     *     empty, which is a {@code FEED_UNAVAILABLE} notice rather than silence)
     * @return the notifications, most important first; empty when there is nothing to say
     */
    public static List<UpdatesCheckerNotification> evaluate(
            UpdatesCheckerRunningVersion running, UpdatesCheckerFeed feed, boolean fetchAttempted) {
        return evaluate(running, feed, fetchAttempted, UpdatesCheckerSuppression.none());
    }

    /** As above, without the advisories that {@code suppression} names. */
    public static List<UpdatesCheckerNotification> evaluate(
            UpdatesCheckerRunningVersion running,
            UpdatesCheckerFeed feed,
            boolean fetchAttempted,
            UpdatesCheckerSuppression suppression) {
        List<UpdatesCheckerNotification> result = new ArrayList<>();
        if (running.getKind() == UpdatesCheckerRunningVersion.Kind.UNKNOWN) {
            UpdatesCheckerText text = UpdatesCheckerText.of("notification.versionUnknown");
            result.add(new UpdatesCheckerNotification(
                    Type.VERSION_UNKNOWN, UpdatesCheckerSeverity.INFO, "version", text, null, null));
            return result;
        }
        if (feed == null) {
            if (fetchAttempted) {
                UpdatesCheckerText text = UpdatesCheckerText.of("notification.feedUnavailable");
                result.add(new UpdatesCheckerNotification(
                        Type.FEED_UNAVAILABLE, UpdatesCheckerSeverity.WARN, "feed", text, null, null));
            }
            return result;
        }
        if (feed.isSchemaTooNew()) {
            UpdatesCheckerText text = UpdatesCheckerText.of("notification.schemaTooNew");
            result.add(new UpdatesCheckerNotification(
                    Type.SCHEMA_TOO_NEW, UpdatesCheckerSeverity.WARN, "schema", text, null, null));
            return result;
        }
        if (running.getKind() == UpdatesCheckerRunningVersion.Kind.DEVELOPMENT) {
            // no comparison against released versions and no advisories: there is nothing to report
            return result;
        }

        UpdatesCheckerVersion version = running.getRelease();
        UpdatesCheckerFeed.Series own = feed.findSeries(version.getSeries());
        addPatchNotification(result, running, version, own);
        addSeriesNotification(result, version, own, feed);
        addAdvisoryNotifications(result, version, feed, suppression);
        result.sort(PRIORITY);
        return result;
    }

    private static void addPatchNotification(
            List<UpdatesCheckerNotification> result,
            UpdatesCheckerRunningVersion running,
            UpdatesCheckerVersion version,
            UpdatesCheckerFeed.Series own) {
        if (own == null || own.getLatestVersion().compareTo(version) <= 0) {
            return;
        }
        String latest = own.getLatestVersion().toString();
        List<UpdatesCheckerFeed.Release> qualifying = qualifyingReleases(own, version);
        if (!qualifying.isEmpty()) {
            // a probable security release: shown instead of the plain patch notice, because it is the stronger signal
            result.add(securityRelease(qualifying, running, latest));
            return;
        }
        result.add(new UpdatesCheckerNotification(
                Type.PATCH_AVAILABLE,
                UpdatesCheckerSeverity.WARN,
                latest,
                UpdatesCheckerText.of("notification.patch", running.getRaw(), latest),
                own.getReleaseUrl(),
                latest));
    }

    /**
     * The releases of the running series that are newer than the running version and are flagged as security releases
     * or were released with other series. A feed that does not list the recent releases is read through the latest
     * release alone.
     */
    private static List<UpdatesCheckerFeed.Release> qualifyingReleases(
            UpdatesCheckerFeed.Series own, UpdatesCheckerVersion running) {
        List<UpdatesCheckerFeed.Release> releases =
                own.getRecentReleases() != null ? own.getRecentReleases() : List.of(own.getLatestRelease());
        List<UpdatesCheckerFeed.Release> qualifying = new ArrayList<>();
        for (UpdatesCheckerFeed.Release release : releases) {
            if (release.getVersion().compareTo(running) > 0
                    && (release.isSecurityFlagged() || release.isSynchronizedRelease())) {
                qualifying.add(release);
            }
        }
        return qualifying;
    }

    /**
     * HIGH, naming the newest flagged release, if any qualifying release is flagged; otherwise WARN, naming the newest
     * coordinated one. The notice offers the series' latest version as the one to upgrade to.
     */
    private static UpdatesCheckerNotification securityRelease(
            List<UpdatesCheckerFeed.Release> qualifying, UpdatesCheckerRunningVersion running, String latest) {
        UpdatesCheckerFeed.Release selected = newest(qualifying, true);
        boolean flagged = selected != null;
        if (!flagged) {
            selected = newest(qualifying, false);
        }
        UpdatesCheckerText notes = UpdatesCheckerText.of(
                selected.getBlogUrl() == null
                        ? "notification.securityRelease.noNotes"
                        : "notification.securityRelease.notes");
        String key = flagged ? "notification.securityRelease.flagged" : "notification.securityRelease.coordinated";
        String version = selected.getVersion().toString();
        return new UpdatesCheckerNotification(
                Type.SECURITY_RELEASE,
                flagged ? UpdatesCheckerSeverity.HIGH : UpdatesCheckerSeverity.WARN,
                version,
                UpdatesCheckerText.of(key, version, running.getRaw(), latest, notes),
                selected.getBlogUrl(),
                latest);
    }

    /** @return the newest release that is flagged, or if {@code flagged} is false the newest of any. */
    private static UpdatesCheckerFeed.Release newest(List<UpdatesCheckerFeed.Release> releases, boolean flagged) {
        UpdatesCheckerFeed.Release best = null;
        for (UpdatesCheckerFeed.Release release : releases) {
            boolean eligible = !flagged || release.isSecurityFlagged();
            if (eligible && (best == null || release.getVersion().compareTo(best.getVersion()) > 0)) {
                best = release;
            }
        }
        return best;
    }

    private static void addSeriesNotification(
            List<UpdatesCheckerNotification> result,
            UpdatesCheckerVersion version,
            UpdatesCheckerFeed.Series own,
            UpdatesCheckerFeed feed) {
        String series = version.getSeries();
        UpdatesCheckerFeed.Series stable = feed.findNewest(UpdatesCheckerFeed.Phase.STABLE);
        if (own == null) {
            if (isAheadOfFeed(series, feed)) {
                return;
            }
            result.add(new UpdatesCheckerNotification(
                    Type.SERIES_ARCHIVE,
                    UpdatesCheckerSeverity.WARN,
                    series,
                    UpdatesCheckerText.of(
                            "notification.archive.absent", series, stableTarget(feed), maintenanceTargets(feed)),
                    null,
                    null));
            return;
        }
        if (own.getPhase() == UpdatesCheckerFeed.Phase.MAINTENANCE) {
            UpdatesCheckerText text = stable == null
                    ? UpdatesCheckerText.of("notification.maintenance.noStable", series)
                    : UpdatesCheckerText.of(
                            "notification.maintenance",
                            series,
                            stable.getSeries(),
                            stable.getLatestVersion().toString());
            result.add(new UpdatesCheckerNotification(
                    Type.SERIES_MAINTENANCE,
                    UpdatesCheckerSeverity.NOTICE,
                    stable == null ? "none" : stable.getSeries(),
                    text,
                    stable == null ? null : stable.getReleaseUrl(),
                    null));
        } else if (own.getPhase() == UpdatesCheckerFeed.Phase.ARCHIVE) {
            result.add(new UpdatesCheckerNotification(
                    Type.SERIES_ARCHIVE,
                    UpdatesCheckerSeverity.WARN,
                    series,
                    UpdatesCheckerText.of("notification.archive", series, stableTarget(feed), maintenanceTargets(feed)),
                    null,
                    null));
        }
    }

    /** A running series newer than every series in the feed is simply ahead of it, not out of support. */
    private static boolean isAheadOfFeed(String series, UpdatesCheckerFeed feed) {
        if (feed.getSeries().isEmpty()) {
            return false;
        }
        for (UpdatesCheckerFeed.Series s : feed.getSeries()) {
            if (UpdatesCheckerVersion.compareSeries(s.getSeries(), series) > 0) {
                return false;
            }
        }
        return true;
    }

    /** The stable version to move to, as the text that ends an "unsupported series" notice; empty if there is none. */
    private static UpdatesCheckerText stableTarget(UpdatesCheckerFeed feed) {
        UpdatesCheckerFeed.Series stable = feed.findNewest(UpdatesCheckerFeed.Phase.STABLE);
        return stable == null
                ? UpdatesCheckerText.literal("")
                : UpdatesCheckerText.of(
                        "notification.upgradeTargets.stable",
                        stable.getLatestVersion().toString());
    }

    /** The maintenance versions to move to, newest series first; empty if there are none. */
    private static UpdatesCheckerText maintenanceTargets(UpdatesCheckerFeed feed) {
        List<UpdatesCheckerFeed.Series> maintenance = new ArrayList<>();
        for (UpdatesCheckerFeed.Series s : feed.getSeries()) {
            if (s.getPhase() == UpdatesCheckerFeed.Phase.MAINTENANCE) {
                maintenance.add(s);
            }
        }
        if (maintenance.isEmpty()) {
            return UpdatesCheckerText.literal("");
        }
        maintenance.sort((a, b) -> UpdatesCheckerVersion.compareSeries(b.getSeries(), a.getSeries()));
        StringBuilder versions = new StringBuilder();
        for (int i = 0; i < maintenance.size(); i++) {
            versions.append(i == 0 ? "" : ", ").append(maintenance.get(i).getLatestVersion());
        }
        return UpdatesCheckerText.of("notification.upgradeTargets.maintenance", versions.toString());
    }

    /**
     * @return what the advisories that {@code suppression} names would have reported for this version: the CVE id, else
     *     the GHSA id, of each that applies. Empty if nothing is suppressed or the feed is not read.
     */
    public static List<String> suppressedAdvisories(
            UpdatesCheckerRunningVersion running, UpdatesCheckerFeed feed, UpdatesCheckerSuppression suppression) {
        List<String> names = new ArrayList<>();
        if (suppression.isEmpty()
                || feed == null
                || feed.isSchemaTooNew()
                || running.getKind() != UpdatesCheckerRunningVersion.Kind.RELEASE) {
            return names;
        }
        for (UpdatesCheckerFeed.Advisory advisory : feed.getAdvisories()) {
            if (suppression.suppresses(advisory)
                    && applicability(advisory, running.getRelease()) != Applicability.NOT_AFFECTED) {
                names.add(advisory.getCveId() != null ? advisory.getCveId() : advisory.getGhsaId());
            }
        }
        return names;
    }

    private static void addAdvisoryNotifications(
            List<UpdatesCheckerNotification> result,
            UpdatesCheckerVersion version,
            UpdatesCheckerFeed feed,
            UpdatesCheckerSuppression suppression) {
        for (UpdatesCheckerFeed.Advisory advisory : feed.getAdvisories()) {
            Applicability applicability = applicability(advisory, version);
            if (applicability == Applicability.NOT_AFFECTED || suppression.suppresses(advisory)) {
                continue;
            }
            Object id = advisoryName(advisory);
            UpdatesCheckerText severity = advisoryWord(advisory.getSeverity());
            String link = UpdatesCheckerFeedText.ghsaLink(advisory.getGhsaId());
            UpdatesCheckerText text;
            Type type;
            if (applicability == Applicability.AFFECTED) {
                type = Type.CVE_CONFIRMED;
                text = UpdatesCheckerText.of("notification.cve.confirmed", id, severity, advisory.getSummary());
            } else {
                type = Type.CVE_UNVERIFIABLE;
                text = advisory.getSummary().isEmpty()
                        ? UpdatesCheckerText.of("notification.cve.unverifiable.noSummary", id, severity)
                        : UpdatesCheckerText.of(
                                "notification.cve.unverifiable", id, severity, endWithFullStop(advisory.getSummary()));
            }
            result.add(
                    new UpdatesCheckerNotification(type, advisory.getSeverity(), advisory.getKey(), text, link, null));
        }
    }

    /** What names an advisory in a notification: its CVE id, else its GHSA id, else a generic word. */
    private static Object advisoryName(UpdatesCheckerFeed.Advisory advisory) {
        if (advisory.getCveId() != null) {
            return advisory.getCveId();
        }
        if (advisory.getGhsaId() != null) {
            return advisory.getGhsaId();
        }
        return UpdatesCheckerText.of("notification.advisory.unnamed");
    }

    /** The summary as a sentence, so the text that follows it starts a new one. */
    private static String endWithFullStop(String summary) {
        String trimmed = summary.trim();
        return trimmed.endsWith(".") ? trimmed : trimmed + ".";
    }

    /** The word the advisory itself uses for a severity (medium and low map to WARN and INFO notifications). */
    private static UpdatesCheckerText advisoryWord(UpdatesCheckerSeverity severity) {
        switch (severity) {
            case WARN:
                return UpdatesCheckerText.of("advisory.severity.medium");
            case INFO:
                return UpdatesCheckerText.of("advisory.severity.low");
            case CRITICAL:
                return UpdatesCheckerText.of("advisory.severity.critical");
            default:
                return UpdatesCheckerText.of("advisory.severity.high");
        }
    }

    /** How an advisory relates to the running version. */
    enum Applicability {
        AFFECTED,
        UNVERIFIABLE,
        NOT_AFFECTED
    }

    /**
     * Decides whether an advisory applies, in this order: a version at or beyond the patched version of its own series
     * (or beyond every patched version, if its series has none) is unaffected; otherwise it is affected if a readable
     * range matches or it is below its series' patched version; otherwise, if any range is unreadable (or there is no
     * range at all), the advisory is flagged for manual review rather than ignored.
     */
    static Applicability applicability(UpdatesCheckerFeed.Advisory advisory, UpdatesCheckerVersion version) {
        Map<String, UpdatesCheckerVersion> patched = advisory.getPatchedVersions();
        UpdatesCheckerVersion ownPatch = patched.get(version.getSeries());
        if (ownPatch != null) {
            if (version.compareTo(ownPatch) >= 0) {
                return Applicability.NOT_AFFECTED;
            }
        } else if (!patched.isEmpty() && version.compareTo(highest(patched)) > 0) {
            return Applicability.NOT_AFFECTED;
        }
        boolean unreadable = false;
        for (String text : advisory.getVulnerableVersions()) {
            UpdatesCheckerVersionRange range = UpdatesCheckerVersionRange.parse(text);
            if (!range.isParseable()) {
                unreadable = true;
            } else if (range.matches(version)) {
                return Applicability.AFFECTED;
            }
        }
        if (ownPatch != null) {
            // below the patched version of its own series, so not yet fixed
            return Applicability.AFFECTED;
        }
        if (unreadable || advisory.getVulnerableVersions().isEmpty()) {
            return Applicability.UNVERIFIABLE;
        }
        return Applicability.NOT_AFFECTED;
    }

    private static UpdatesCheckerVersion highest(Map<String, UpdatesCheckerVersion> patched) {
        UpdatesCheckerVersion best = null;
        for (UpdatesCheckerVersion v : patched.values()) {
            if (best == null || v.compareTo(best) > 0) {
                best = v;
            }
        }
        return best;
    }
}

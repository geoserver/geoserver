/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.updateschecker.Notification.Type;

/**
 * Compares the running version with the feed and returns the notifications: most urgent severity first, then security,
 * patch and series notices.
 */
public final class Evaluator {

    private static final Comparator<Notification> PRIORITY = (a, b) -> {
        int c = a.getSeverity().compareTo(b.getSeverity());
        if (c == 0) {
            c = Integer.compare(a.getType().getGroup(), b.getType().getGroup());
        }
        return c != 0 ? c : a.getType().compareTo(b.getType());
    };

    private Evaluator() {}

    /**
     * @param feed the last good feed, or {@code null}
     * @param fetchAttempted whether a fetch was tried; with no feed, that gives a {@code FEED_UNAVAILABLE} notice
     */
    public static List<Notification> evaluate(RunningVersion running, Feed feed, boolean fetchAttempted) {
        return evaluate(running, feed, fetchAttempted, Suppression.none());
    }

    /** As above, leaving out the suppressed advisories. */
    public static List<Notification> evaluate(
            RunningVersion running, Feed feed, boolean fetchAttempted, Suppression suppression) {
        List<Notification> result = new ArrayList<>();
        if (running.getKind() == RunningVersion.Kind.UNKNOWN) {
            Text text = Text.of("notification.versionUnknown");
            result.add(new Notification(Type.VERSION_UNKNOWN, Severity.INFO, "version", text, null, null));
            return result;
        }
        if (feed == null) {
            if (fetchAttempted) {
                Text text = Text.of("notification.feedUnavailable");
                result.add(new Notification(Type.FEED_UNAVAILABLE, Severity.WARN, "feed", text, null, null));
            }
            return result;
        }
        if (feed.isSchemaTooNew()) {
            Text text = Text.of("notification.schemaTooNew");
            result.add(new Notification(Type.SCHEMA_TOO_NEW, Severity.WARN, "schema", text, null, null));
            return result;
        }
        if (running.getKind() == RunningVersion.Kind.DEVELOPMENT) {
            return result;
        }

        Version version = running.getRelease();
        Feed.Series own = feed.findSeries(version.getSeries());
        addPatchNotification(result, running, version, own);
        addSeriesNotification(result, version, own, feed);
        addAdvisoryNotifications(result, version, feed, suppression);
        result.sort(PRIORITY);
        return result;
    }

    private static void addPatchNotification(
            List<Notification> result, RunningVersion running, Version version, Feed.Series own) {
        if (own == null || own.getLatestVersion().compareTo(version) <= 0) {
            return;
        }
        String latest = own.getLatestVersion().toString();
        List<Feed.Release> qualifying = qualifyingReleases(own, version);
        if (!qualifying.isEmpty()) {
            // shown instead of the patch notice
            result.add(securityRelease(qualifying, running, latest));
            return;
        }
        result.add(new Notification(
                Type.PATCH_AVAILABLE,
                Severity.WARN,
                latest,
                Text.of("notification.patch", running.getRaw(), latest),
                own.getReleaseUrl(),
                latest));
    }

    /**
     * The releases of the running series, newer than the running version, that are flagged as security releases or were
     * released with other series. Without a recent releases list, only the latest release is read.
     */
    private static List<Feed.Release> qualifyingReleases(Feed.Series own, Version running) {
        List<Feed.Release> releases =
                own.getRecentReleases() != null ? own.getRecentReleases() : List.of(own.getLatestRelease());
        List<Feed.Release> qualifying = new ArrayList<>();
        for (Feed.Release release : releases) {
            if (release.getVersion().compareTo(running) > 0
                    && (release.isSecurityFlagged() || release.isSynchronizedRelease())) {
                qualifying.add(release);
            }
        }
        return qualifying;
    }

    /** HIGH naming the newest flagged release if there is one, otherwise WARN naming the newest coordinated one. */
    private static Notification securityRelease(List<Feed.Release> qualifying, RunningVersion running, String latest) {
        Feed.Release selected = newest(qualifying, true);
        boolean flagged = selected != null;
        if (!flagged) {
            selected = newest(qualifying, false);
        }
        Text notes = Text.of(
                selected.getBlogUrl() == null
                        ? "notification.securityRelease.noNotes"
                        : "notification.securityRelease.notes");
        String key = flagged ? "notification.securityRelease.flagged" : "notification.securityRelease.coordinated";
        String version = selected.getVersion().toString();
        return new Notification(
                Type.SECURITY_RELEASE,
                flagged ? Severity.HIGH : Severity.WARN,
                version,
                Text.of(key, version, running.getRaw(), latest, notes),
                selected.getBlogUrl(),
                latest);
    }

    /** @return the newest release, or the newest flagged one */
    private static Feed.Release newest(List<Feed.Release> releases, boolean flagged) {
        Feed.Release best = null;
        for (Feed.Release release : releases) {
            boolean eligible = !flagged || release.isSecurityFlagged();
            if (eligible && (best == null || release.getVersion().compareTo(best.getVersion()) > 0)) {
                best = release;
            }
        }
        return best;
    }

    private static void addSeriesNotification(List<Notification> result, Version version, Feed.Series own, Feed feed) {
        String series = version.getSeries();
        Feed.Series stable = feed.findNewest(Feed.Phase.STABLE);
        if (own == null) {
            if (isAheadOfFeed(series, feed)) {
                return;
            }
            result.add(archive(series, "notification.archive.absent", feed));
            return;
        }
        if (own.getPhase() == Feed.Phase.MAINTENANCE) {
            Text text = stable == null
                    ? Text.of("notification.maintenance.noStable", series)
                    : Text.of(
                            "notification.maintenance",
                            series,
                            stable.getSeries(),
                            stable.getLatestVersion().toString());
            result.add(new Notification(
                    Type.SERIES_MAINTENANCE,
                    Severity.NOTICE,
                    stable == null ? "none" : stable.getSeries(),
                    text,
                    stable == null ? null : stable.getReleaseUrl(),
                    null));
        } else if (own.getPhase() == Feed.Phase.ARCHIVE) {
            result.add(archive(series, "notification.archive", feed));
        }
    }

    private static Notification archive(String series, String textKey, Feed feed) {
        Text text = Text.of(textKey, series, stableTarget(feed), maintenanceTargets(feed));
        return new Notification(Type.SERIES_ARCHIVE, Severity.WARN, series, text, null, null);
    }

    /** A series newer than every series in the feed is ahead of it, not out of support. */
    private static boolean isAheadOfFeed(String series, Feed feed) {
        if (feed.getSeries().isEmpty()) {
            return false;
        }
        for (Feed.Series s : feed.getSeries()) {
            if (Version.compareSeries(s.getSeries(), series) > 0) {
                return false;
            }
        }
        return true;
    }

    /** The stable version to move to, or empty. */
    private static Text stableTarget(Feed feed) {
        Feed.Series stable = feed.findNewest(Feed.Phase.STABLE);
        return stable == null
                ? Text.literal("")
                : Text.of("notification.upgradeTargets.stable", stable.getLatestVersion());
    }

    /** The maintenance versions to move to, newest first, or empty. */
    private static Text maintenanceTargets(Feed feed) {
        List<Feed.Series> maintenance = new ArrayList<>();
        for (Feed.Series s : feed.getSeries()) {
            if (s.getPhase() == Feed.Phase.MAINTENANCE) {
                maintenance.add(s);
            }
        }
        if (maintenance.isEmpty()) {
            return Text.literal("");
        }
        maintenance.sort((a, b) -> Version.compareSeries(b.getSeries(), a.getSeries()));
        String versions =
                maintenance.stream().map(s -> s.getLatestVersion().toString()).collect(Collectors.joining(", "));
        return Text.of("notification.upgradeTargets.maintenance", versions);
    }

    /** @return the CVE id, else the GHSA id, of each suppressed advisory that applies to the running version */
    public static List<String> suppressedAdvisories(RunningVersion running, Feed feed, Suppression suppression) {
        List<String> names = new ArrayList<>();
        if (suppression.isEmpty()
                || feed == null
                || feed.isSchemaTooNew()
                || running.getKind() != RunningVersion.Kind.RELEASE) {
            return names;
        }
        for (Feed.Advisory advisory : feed.getAdvisories()) {
            if (suppression.suppresses(advisory)
                    && applicability(advisory, running.getRelease()) != Applicability.NOT_AFFECTED) {
                names.add(advisory.getCveId() != null ? advisory.getCveId() : advisory.getGhsaId());
            }
        }
        return names;
    }

    private static void addAdvisoryNotifications(
            List<Notification> result, Version version, Feed feed, Suppression suppression) {
        for (Feed.Advisory advisory : feed.getAdvisories()) {
            Applicability applicability = applicability(advisory, version);
            if (applicability == Applicability.NOT_AFFECTED || suppression.suppresses(advisory)) {
                continue;
            }
            Object id = advisoryName(advisory);
            Text severity = advisoryWord(advisory.getSeverity());
            String link = Feed.ghsaLink(advisory.getGhsaId());
            Text text;
            Type type;
            if (applicability == Applicability.AFFECTED) {
                type = Type.CVE_CONFIRMED;
                text = Text.of("notification.cve.confirmed", id, severity, advisory.getSummary());
            } else {
                type = Type.CVE_UNVERIFIABLE;
                text = advisory.getSummary().isEmpty()
                        ? Text.of("notification.cve.unverifiable.noSummary", id, severity)
                        : Text.of(
                                "notification.cve.unverifiable", id, severity, endWithFullStop(advisory.getSummary()));
            }
            result.add(new Notification(type, advisory.getSeverity(), advisory.getKey(), text, link, null));
        }
    }

    private static Object advisoryName(Feed.Advisory advisory) {
        if (advisory.getCveId() != null) {
            return advisory.getCveId();
        }
        if (advisory.getGhsaId() != null) {
            return advisory.getGhsaId();
        }
        return Text.of("notification.advisory.unnamed");
    }

    private static String endWithFullStop(String summary) {
        String trimmed = summary.trim();
        return trimmed.endsWith(".") ? trimmed : trimmed + ".";
    }

    /** The advisory's own word for the severity. */
    private static Text advisoryWord(Severity severity) {
        switch (severity) {
            case WARN:
                return Text.of("advisory.severity.medium");
            case INFO:
                return Text.of("advisory.severity.low");
            case CRITICAL:
                return Text.of("advisory.severity.critical");
            default:
                return Text.of("advisory.severity.high");
        }
    }

    enum Applicability {
        AFFECTED,
        UNVERIFIABLE,
        NOT_AFFECTED
    }

    /**
     * In order: at or beyond the patched version of its series (or beyond every patched version, if its series has
     * none) is not affected; a matching range, or being below its series' patched version, is affected; an unreadable
     * or missing range is unverifiable, for manual review.
     */
    static Applicability applicability(Feed.Advisory advisory, Version version) {
        Map<String, Version> patched = advisory.getPatchedVersions();
        Version ownPatch = patched.get(version.getSeries());
        if (ownPatch != null) {
            if (version.compareTo(ownPatch) >= 0) {
                return Applicability.NOT_AFFECTED;
            }
        } else if (!patched.isEmpty() && version.compareTo(highest(patched)) > 0) {
            return Applicability.NOT_AFFECTED;
        }
        boolean unreadable = false;
        for (String text : advisory.getVulnerableVersions()) {
            VersionRange range = VersionRange.parse(text);
            if (!range.isParseable()) {
                unreadable = true;
            } else if (range.matches(version)) {
                return Applicability.AFFECTED;
            }
        }
        if (ownPatch != null) {
            return Applicability.AFFECTED;
        }
        if (unreadable || advisory.getVulnerableVersions().isEmpty()) {
            return Applicability.UNVERIFIABLE;
        }
        return Applicability.NOT_AFFECTED;
    }

    private static Version highest(Map<String, Version> patched) {
        Version best = null;
        for (Version v : patched.values()) {
            if (best == null || v.compareTo(best) > 0) {
                best = v;
            }
        }
        return best;
    }
}

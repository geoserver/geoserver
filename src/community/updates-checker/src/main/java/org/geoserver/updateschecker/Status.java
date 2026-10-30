/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.geoserver.updateschecker.Notification.Type;

/** The outcome of the update check at one moment, as shown by the banner, the settings page and REST. */
public final class Status implements Serializable {

    private static final long serialVersionUID = 1L;

    /** What a check tells the administrator to do. The names are written to the log and returned by REST. */
    public enum UpdateState {
        /** A critical or high advisory applies or could not be ruled out, or a newer release is a security release. */
        URGENT_UPDATE_AVAILABLE,
        /** Any other advisory, a newer patch or an unsupported series. */
        UPDATE_AVAILABLE,
        UP_TO_DATE,
        /** The feed could not be read, or the running version is unknown. */
        CHECK_FAILED
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final RunningVersion.Kind runningKind;
    private final String runningVersion;
    private final String runningSeries;
    private final String runningPhase;
    private final List<Notification> notifications;
    private final List<String> suppressed;
    private final long checkedAtMillis;
    private final boolean attempted;
    private final boolean fetchFailed;
    private final boolean haveFeed;
    private final Text error;

    Status(
            RunningVersion running,
            Feed feed,
            List<Notification> notifications,
            List<String> suppressed,
            FeedClient.State state) {
        this.runningKind = running.getKind();
        this.runningVersion = running.getRaw();
        this.runningSeries = running.isRelease() ? running.getRelease().getSeries() : null;
        this.notifications = Collections.unmodifiableList(new ArrayList<>(notifications));
        this.suppressed = Collections.unmodifiableList(new ArrayList<>(suppressed));
        this.checkedAtMillis = state.getCheckedAtMillis();
        this.attempted = state.isAttempted();
        this.fetchFailed = state.isLastAttemptFailed();
        this.haveFeed = feed != null;
        this.error = state.getLastError();
        String phase = null;
        if (feed != null && !feed.isSchemaTooNew() && runningSeries != null) {
            Feed.Series own = feed.findSeries(runningSeries);
            if (own != null) {
                phase = own.getPhase().name().toLowerCase(Locale.ROOT);
            }
        }
        this.runningPhase = phase;
    }

    public RunningVersion.Kind getRunningKind() {
        return runningKind;
    }

    /** @return the running version, or {@code null} when it is not known */
    public String getRunningVersion() {
        return runningVersion;
    }

    /** @return the notifications, most important first */
    public List<Notification> getNotifications() {
        return notifications;
    }

    /** @return the advisories that apply but are suppressed */
    public List<String> getSuppressed() {
        return suppressed;
    }

    public String getSuppressedMessage(Locale locale) {
        return Messages.get(locale, "status.suppressed", String.join(", ", suppressed));
    }

    /** @return when the feed was last confirmed, in epoch milliseconds, or 0 if never */
    public long getCheckedAtMillis() {
        return checkedAtMillis;
    }

    public boolean isAttempted() {
        return attempted;
    }

    /** @return whether the latest fetch failed; the feed shown, if any, is the last good copy */
    public boolean isFetchFailed() {
        return fetchFailed;
    }

    /** @return why the latest fetch failed, or {@code null} */
    public String getErrorMessage(Locale locale) {
        return error == null ? null : Messages.forLog(error.resolve(locale));
    }

    /** @return the first line of the Status section as HTML, with the version and the series in bold */
    public String getHeaderHtml(Locale locale) {
        switch (runningKind) {
            case UNKNOWN:
                return Messages.get(locale, "status.header.unknown");
            case DEVELOPMENT:
                return Messages.get(locale, "status.header.development", strong(runningVersion));
            default:
                String series =
                        runningPhase == null ? runningSeries : Messages.get(locale, "status.phase." + runningPhase);
                return Messages.get(locale, "status.header.release", strong(runningVersion), strong(series));
        }
    }

    private static String strong(String value) {
        return "<strong>" + Messages.escapeHtml(value) + "</strong>";
    }

    /** @return when the feed was last confirmed, or why there is nothing to show */
    public String getFeedLine(Locale locale) {
        if (haveFeed && checkedAtMillis > 0) {
            String line = Messages.get(
                    locale,
                    "status.feedChecked",
                    TIME.withLocale(locale)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(checkedAtMillis)));
            return fetchFailed ? line + Messages.get(locale, "status.feedStale") : line;
        }
        return Messages.get(locale, attempted ? "status.noFeed" : "status.notChecked");
    }

    public String getCheckFailedMessage(Locale locale) {
        return Messages.get(
                locale,
                "status.checkFailed",
                error == null
                        ? Messages.get(locale, "status.checkFailed.unknownReason")
                        : Messages.forLog(error.resolve(locale)));
    }

    /** @return what the notifications add up to; the caller handles a failed download, see {@link #isFetchFailed()} */
    public UpdateState getUpdateState() {
        UpdateState state = UpdateState.UP_TO_DATE;
        for (Notification n : notifications) {
            switch (n.getType()) {
                case VERSION_UNKNOWN:
                case FEED_UNAVAILABLE:
                case SCHEMA_TOO_NEW:
                    return UpdateState.CHECK_FAILED;
                case SECURITY_RELEASE:
                    return UpdateState.URGENT_UPDATE_AVAILABLE;
                case CVE_CONFIRMED:
                case CVE_UNVERIFIABLE:
                    if (n.getSeverity() == Severity.CRITICAL || n.getSeverity() == Severity.HIGH) {
                        return UpdateState.URGENT_UPDATE_AVAILABLE;
                    }
                    state = UpdateState.UPDATE_AVAILABLE;
                    break;
                case PATCH_AVAILABLE:
                case SERIES_ARCHIVE:
                    state = UpdateState.UPDATE_AVAILABLE;
                    break;
                default:
                    break;
            }
        }
        return state;
    }

    /** @return the version a patch or security release notice offers, or {@code null} */
    public String getNewerVersion() {
        for (Notification n : notifications) {
            if (n.getType() == Type.PATCH_AVAILABLE || n.getType() == Type.SECURITY_RELEASE) {
                return n.getTargetVersion();
            }
        }
        return null;
    }
}

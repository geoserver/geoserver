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
import org.geoserver.updateschecker.UpdatesCheckerNotification.Type;

/**
 * A snapshot of the update check at one moment: what is running, what the last good feed said, whether the latest
 * attempt failed, and the notifications that follow. Everything needed to draw the banner, the settings page's Status
 * section and the REST response, taken without touching the network.
 */
public final class UpdatesCheckerStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final UpdatesCheckerRunningVersion.Kind runningKind;
    private final String runningVersion;
    private final String runningSeries;
    private final String runningPhase;
    private final List<UpdatesCheckerNotification> notifications;
    private final long checkedAtMillis;
    private final boolean attempted;
    private final boolean fetchFailed;
    private final boolean haveFeed;
    private final UpdatesCheckerText error;

    UpdatesCheckerStatus(
            UpdatesCheckerRunningVersion running,
            UpdatesCheckerFeed feed,
            List<UpdatesCheckerNotification> notifications,
            UpdatesCheckerFeedClient.State state) {
        this.runningKind = running.getKind();
        this.runningVersion = running.getRaw();
        this.runningSeries = running.isRelease() ? running.getRelease().getSeries() : null;
        this.notifications = Collections.unmodifiableList(new ArrayList<>(notifications));
        this.checkedAtMillis = state.getCheckedAtMillis();
        this.attempted = state.isAttempted();
        this.fetchFailed = state.isLastAttemptFailed();
        this.haveFeed = feed != null;
        this.error = state.getLastError();
        String phase = null;
        if (feed != null && !feed.isSchemaTooNew() && runningSeries != null) {
            UpdatesCheckerFeed.Series own = feed.findSeries(runningSeries);
            if (own != null) {
                phase = own.getPhase().name().toLowerCase(Locale.ROOT);
            }
        }
        this.runningPhase = phase;
    }

    public UpdatesCheckerRunningVersion.Kind getRunningKind() {
        return runningKind;
    }

    /** @return the running version text, or {@code null} when it could not be determined. */
    public String getRunningVersion() {
        return runningVersion;
    }

    /** @return the notifications, most important first. */
    public List<UpdatesCheckerNotification> getNotifications() {
        return notifications;
    }

    /** @return when the feed was last confirmed (epoch milliseconds), or 0 if it never was. */
    public long getCheckedAtMillis() {
        return checkedAtMillis;
    }

    /** @return whether any fetch has been tried since the server started. */
    public boolean isAttempted() {
        return attempted;
    }

    /** @return whether the latest fetch failed; the feed shown, if any, is then the last good copy. */
    public boolean isFetchFailed() {
        return fetchFailed;
    }

    /** @return the reason for the latest failure in the given language, or {@code null}. */
    public String getErrorMessage(Locale locale) {
        return error == null ? null : UpdatesCheckerFeedText.forLog(error.resolve(locale));
    }

    /**
     * @return the line at the top of the Status section, in the given language, with the version and the series in
     *     {@code <strong>}; the values are HTML-escaped
     */
    public String getHeaderHtml(Locale locale) {
        switch (runningKind) {
            case UNKNOWN:
                return UpdatesCheckerMessages.get(locale, "status.header.unknown");
            case DEVELOPMENT:
                return UpdatesCheckerMessages.get(locale, "status.header.development", strong(runningVersion));
            default:
                String series = runningPhase == null
                        ? runningSeries
                        : UpdatesCheckerMessages.get(locale, "status.phase." + runningPhase);
                return UpdatesCheckerMessages.get(
                        locale, "status.header.release", strong(runningVersion), strong(series));
        }
    }

    private static String strong(String value) {
        return "<strong>" + UpdatesCheckerMessages.escapeHtml(value) + "</strong>";
    }

    /** @return when the feed was last confirmed or why there is nothing to show, in the given language. */
    public String getFeedLine(Locale locale) {
        if (haveFeed && checkedAtMillis > 0) {
            String line = UpdatesCheckerMessages.get(
                    locale,
                    "status.feedChecked",
                    TIME.withLocale(locale)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(checkedAtMillis)));
            return fetchFailed ? line + UpdatesCheckerMessages.get(locale, "status.feedStale") : line;
        }
        return UpdatesCheckerMessages.get(locale, attempted ? "status.noFeed" : "status.notChecked");
    }

    /** @return the "Check failed: reason." line, in the given language. */
    public String getCheckFailedMessage(Locale locale) {
        return UpdatesCheckerMessages.get(
                locale,
                "status.checkFailed",
                error == null
                        ? UpdatesCheckerMessages.get(locale, "status.checkFailed.unknownReason")
                        : UpdatesCheckerFeedText.forLog(error.resolve(locale)));
    }

    /**
     * @return what the notifications add up to. {@code CHECK_FAILED} when the check could not tell, because the version
     *     or the feed was unusable; a failed download is decided by the caller, see {@link #isFetchFailed()}.
     */
    public UpdatesCheckerState getUpdateState() {
        UpdatesCheckerState state = UpdatesCheckerState.UP_TO_DATE;
        for (UpdatesCheckerNotification n : notifications) {
            switch (n.getType()) {
                case VERSION_UNKNOWN:
                case FEED_UNAVAILABLE:
                case SCHEMA_TOO_NEW:
                    return UpdatesCheckerState.CHECK_FAILED;
                case SECURITY_RELEASE:
                    return UpdatesCheckerState.URGENT_UPDATE_AVAILABLE;
                case CVE_CONFIRMED:
                case CVE_UNVERIFIABLE:
                    if (n.getSeverity() == UpdatesCheckerSeverity.CRITICAL
                            || n.getSeverity() == UpdatesCheckerSeverity.HIGH) {
                        return UpdatesCheckerState.URGENT_UPDATE_AVAILABLE;
                    }
                    state = UpdatesCheckerState.UPDATE_AVAILABLE;
                    break;
                case PATCH_AVAILABLE:
                case SERIES_ARCHIVE:
                    state = UpdatesCheckerState.UPDATE_AVAILABLE;
                    break;
                default:
                    break;
            }
        }
        return state;
    }

    /** @return the version a patch or coordinated-release notice points to, or {@code null} if neither is active. */
    public String getNewerVersion() {
        for (UpdatesCheckerNotification n : notifications) {
            if (n.getType() == Type.PATCH_AVAILABLE || n.getType() == Type.SECURITY_RELEASE) {
                return n.getTargetVersion();
            }
        }
        return null;
    }
}

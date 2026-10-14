/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.geoserver.updateschecker.UpdatesCheckerMessages;
import org.geoserver.updateschecker.UpdatesCheckerNotification;
import org.geoserver.updateschecker.UpdatesCheckerRunningVersion;
import org.geoserver.updateschecker.UpdatesCheckerState;
import org.geoserver.updateschecker.UpdatesCheckerStatus;

/** JSON body returned by {@link UpdatesCheckerRestController}: the outcome of one check. */
public class UpdatesCheckerCheckResponse {

    /** One active notification. */
    public static class Notice {
        private final String type;
        private final String severity;
        private final String message;
        private final String discriminator;
        private final String link;

        Notice(UpdatesCheckerNotification n, Locale locale) {
            this.type = n.getType().name();
            this.severity = n.getSeverity().name();
            this.message = n.getMessage(locale);
            this.discriminator = n.getDiscriminator();
            this.link = n.getLink();
        }

        public String getType() {
            return type;
        }

        public String getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }

        public String getDiscriminator() {
            return discriminator;
        }

        public String getLink() {
            return link;
        }
    }

    private final String status;

    private final String message;

    private final String installedVersion;

    private final String newerVersion;

    private final String error;

    private final List<Notice> notifications;

    /** @return the response describing a status taken just after a check, with text in the given language. */
    static UpdatesCheckerCheckResponse of(UpdatesCheckerStatus s, Locale locale) {
        List<Notice> notices = new ArrayList<>();
        for (UpdatesCheckerNotification n : s.getNotifications()) {
            notices.add(new Notice(n, locale));
        }
        UpdatesCheckerState status = s.getUpdateState();
        String error = null;
        String message;
        if (s.isFetchFailed() && s.getRunningKind() != UpdatesCheckerRunningVersion.Kind.UNKNOWN) {
            status = UpdatesCheckerState.CHECK_FAILED;
            error = s.getErrorMessage(locale);
            message = UpdatesCheckerMessages.get(locale, "rest.checkFailed", error);
        } else if (!s.getNotifications().isEmpty()) {
            message = s.getNotifications().get(0).getMessage(locale);
            error = status == UpdatesCheckerState.CHECK_FAILED ? message : null;
        } else {
            boolean development = s.getRunningKind() == UpdatesCheckerRunningVersion.Kind.DEVELOPMENT;
            message = UpdatesCheckerMessages.get(
                    locale, development ? "rest.devBuild" : "rest.upToDate", s.getRunningVersion());
        }
        return new UpdatesCheckerCheckResponse(
                status.name(), message, s.getRunningVersion(), s.getNewerVersion(), error, notices);
    }

    private UpdatesCheckerCheckResponse(
            String status,
            String message,
            String installedVersion,
            String newerVersion,
            String error,
            List<Notice> notifications) {
        this.status = status;
        this.message = message;
        this.installedVersion = installedVersion;
        this.newerVersion = newerVersion;
        this.error = error;
        this.notifications = notifications;
    }

    /**
     * @return {@code URGENT_UPDATE_AVAILABLE}, {@code UPDATE_AVAILABLE}, {@code UP_TO_DATE} or {@code CHECK_FAILED}.
     */
    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getInstalledVersion() {
        return installedVersion;
    }

    public String getNewerVersion() {
        return newerVersion;
    }

    public String getError() {
        return error;
    }

    /** @return every active notification, most important first. */
    public List<Notice> getNotifications() {
        return notifications;
    }
}

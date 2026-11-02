/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.rest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.geoserver.updateschecker.Messages;
import org.geoserver.updateschecker.Notification;
import org.geoserver.updateschecker.RunningVersion;
import org.geoserver.updateschecker.Status;
import org.geoserver.updateschecker.Status.UpdateState;

/** Builds the JSON body of {@link CheckController}. */
final class CheckResponse {

    private CheckResponse() {}

    /**
     * @param suppressedBy {@code "request"} or {@code "environment"}, or {@code null} if nothing was suppressed
     * @return the fields in the order they are written: status, message, installedVersion, newerVersion, error,
     *     suppressed, suppressedBy and notifications
     */
    static Map<String, Object> of(Status s, Locale locale, String suppressedBy) {
        List<Map<String, Object>> notices = new ArrayList<>();
        for (Notification n : s.getNotifications()) {
            Map<String, Object> notice = new LinkedHashMap<>();
            notice.put("type", n.getType().name());
            notice.put("severity", n.getSeverity().name());
            notice.put("message", n.getMessage(locale));
            notice.put("discriminator", n.getDiscriminator());
            notice.put("link", n.getLink());
            notices.add(notice);
        }
        UpdateState status = s.getUpdateState();
        String error = null;
        String message;
        if (s.isFetchFailed() && s.getRunningKind() != RunningVersion.Kind.UNKNOWN) {
            status = UpdateState.CHECK_FAILED;
            error = s.getErrorMessage(locale);
            message = Messages.get(locale, "rest.checkFailed", error);
        } else if (!s.getNotifications().isEmpty()) {
            message = s.getNotifications().get(0).getMessage(locale);
            error = status == UpdateState.CHECK_FAILED ? message : null;
        } else {
            boolean development = s.getRunningKind() == RunningVersion.Kind.DEVELOPMENT;
            message = Messages.get(locale, development ? "rest.devBuild" : "rest.upToDate", s.getRunningVersion());
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status.name());
        response.put("message", message);
        response.put("installedVersion", s.getRunningVersion());
        response.put("newerVersion", s.getNewerVersion());
        response.put("error", error);
        response.put("suppressed", s.getSuppressed());
        response.put("suppressedBy", suppressedBy);
        response.put("notifications", notices);
        return response;
    }
}

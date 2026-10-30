/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

/** One thing to tell the administrator, with the dismiss and snooze actions it offers. Immutable. */
public final class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String KEY_PREFIX = "updates-checker|";

    /** The kinds of notification, with their display group: lower sorts first within one severity. */
    public enum Type {
        CVE_CONFIRMED(0, false),
        CVE_UNVERIFIABLE(0, false),
        SECURITY_RELEASE(0, false),
        PATCH_AVAILABLE(1, false),
        SERIES_MAINTENANCE(2, false),
        SERIES_ARCHIVE(2, false),
        FEED_UNAVAILABLE(3, false),
        SCHEMA_TOO_NEW(3, false),
        VERSION_UNKNOWN(3, true);

        private final int group;
        private final boolean sessionOnly;

        Type(int group, boolean sessionOnly) {
            this.group = group;
            this.sessionOnly = sessionOnly;
        }

        public int getGroup() {
            return group;
        }

        /** @return whether a dismissal lasts for the browser session only */
        public boolean isSessionOnly() {
            return sessionOnly;
        }
    }

    /** What the user can do about a notification: the id the browser script reads, and the label key. */
    public enum Action {
        REMIND_TOMORROW("remind-24h", "UpdatesChecker.action.remindTomorrow"),
        REMIND_IN_30_DAYS("remind-30d", "UpdatesChecker.action.remind30Days"),
        DISMISS_VERSION("dismiss", "UpdatesChecker.action.dismissVersion"),
        DISMISS_SERIES("dismiss", "UpdatesChecker.action.dismissSeries"),
        DISMISS("dismiss", "UpdatesChecker.action.dismiss");

        private final String id;
        private final String messageKey;

        Action(String id, String messageKey) {
            this.id = id;
            this.messageKey = messageKey;
        }

        public String getId() {
            return id;
        }

        public String getMessageKey() {
            return messageKey;
        }
    }

    private final Type type;
    private final Severity severity;
    private final String discriminator;
    private final Text message;
    private final String link;
    private final String targetVersion;
    private final List<Action> actions;

    /**
     * @param discriminator what a dismissal is tied to (version, series or advisory id), so a newer one shows again
     * @param link an allowed link, or {@code null}
     * @param targetVersion the version to upgrade to, named by "Dismiss for version X.Y.Z", or {@code null}
     */
    public Notification(
            Type type, Severity severity, String discriminator, Text message, String link, String targetVersion) {
        this.type = type;
        this.severity = severity;
        this.discriminator = discriminator;
        this.message = message;
        this.link = link;
        this.targetVersion = targetVersion;
        this.actions = actionsFor(type, severity);
    }

    /** A critical confirmed advisory can only be snoozed. */
    static List<Action> actionsFor(Type type, Severity severity) {
        switch (type) {
            case PATCH_AVAILABLE:
                return List.of(Action.REMIND_TOMORROW, Action.DISMISS_VERSION);
            case SECURITY_RELEASE:
                return List.of(Action.REMIND_TOMORROW, Action.DISMISS);
            case SERIES_MAINTENANCE:
                return List.of(Action.REMIND_IN_30_DAYS, Action.DISMISS_SERIES);
            case CVE_CONFIRMED:
                return List.of(severity == Severity.CRITICAL ? Action.REMIND_TOMORROW : Action.DISMISS);
            case CVE_UNVERIFIABLE:
            case VERSION_UNKNOWN:
                return List.of(Action.DISMISS);
            default:
                return List.of();
        }
    }

    public Type getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public String getMessage(Locale locale) {
        return message.resolve(locale);
    }

    /** @see Text#resolveHtml */
    public String getMessageHtml(Locale locale) {
        return message.resolveHtml(locale);
    }

    /** @return an allowed link, or {@code null} */
    public String getLink() {
        return link;
    }

    /** @return the version to upgrade to, or {@code null} */
    public String getTargetVersion() {
        return targetVersion;
    }

    public List<Action> getActions() {
        return actions;
    }

    /**
     * @return the browser storage key that remembers a dismissal, or {@code null} if there are no actions. A critical
     *     advisory gets its own key, so a dismissal from when it was less severe does not hide it.
     */
    public String getDismissKey() {
        if (actions.isEmpty()) {
            return null;
        }
        String key = KEY_PREFIX + type + "|" + discriminator;
        return type == Type.CVE_CONFIRMED && severity == Severity.CRITICAL ? key + "|CRITICAL" : key;
    }

    /** @return the key of the patch notice that dismissing this security release also dismisses, or {@code null} */
    public String getAlsoDismissKey() {
        return type == Type.SECURITY_RELEASE && targetVersion != null
                ? KEY_PREFIX + Type.PATCH_AVAILABLE + "|" + targetVersion
                : null;
    }

    @Override
    public String toString() {
        return type + "/" + severity + "/" + discriminator;
    }
}

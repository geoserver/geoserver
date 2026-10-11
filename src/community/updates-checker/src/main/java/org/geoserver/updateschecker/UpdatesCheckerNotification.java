/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * One thing the update check wants to tell an administrator: what it is, how urgent, the text, an optional link and
 * which dismiss actions it offers. Immutable and serializable, so it can sit in a Wicket component.
 */
public final class UpdatesCheckerNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The kinds of notification, with their display group (lower sorts first within one severity). */
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

        /** @return 0 for security notices, 1 for patches, 2 for series notices, 3 for everything else. */
        public int getGroup() {
            return group;
        }

        /** @return whether a dismissal is remembered for the browser session only, not across visits. */
        public boolean isSessionOnly() {
            return sessionOnly;
        }
    }

    /**
     * What the user can do about a notification. The {@link #getId() id} is what the browser script receives; the
     * {@link #getMessageKey() message key} is the label in {@code GeoServerApplication.properties}.
     */
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
    private final UpdatesCheckerSeverity severity;
    private final String discriminator;
    private final UpdatesCheckerText message;
    private final String link;
    private final String targetVersion;
    private final List<Action> actions;

    /**
     * @param discriminator what a remembered dismissal is tied to (target version, series or advisory id), so a newer
     *     version, series or advisory shows the notification again
     * @param message the full text
     * @param link an allowed link or {@code null}
     * @param targetVersion the version a "Dismiss for version X.Y.Z" action names, or {@code null}
     */
    public UpdatesCheckerNotification(
            Type type,
            UpdatesCheckerSeverity severity,
            String discriminator,
            UpdatesCheckerText message,
            String link,
            String targetVersion) {
        this.type = type;
        this.severity = severity;
        this.discriminator = discriminator;
        this.message = message;
        this.link = link;
        this.targetVersion = targetVersion;
        this.actions = actionsFor(type, severity);
    }

    /** The dismiss actions each notification offers; the wording is fixed. */
    static List<Action> actionsFor(Type type, UpdatesCheckerSeverity severity) {
        switch (type) {
            case PATCH_AVAILABLE:
                return Arrays.asList(Action.REMIND_TOMORROW, Action.DISMISS_VERSION);
            case SECURITY_RELEASE:
                return Arrays.asList(Action.REMIND_TOMORROW, Action.DISMISS);
            case SERIES_MAINTENANCE:
                return Arrays.asList(Action.REMIND_IN_30_DAYS, Action.DISMISS_SERIES);
            case CVE_CONFIRMED:
                return Collections.singletonList(
                        severity == UpdatesCheckerSeverity.CRITICAL ? Action.REMIND_TOMORROW : Action.DISMISS);
            case CVE_UNVERIFIABLE:
            case VERSION_UNKNOWN:
                return Collections.singletonList(Action.DISMISS);
            default:
                return Collections.emptyList();
        }
    }

    public Type getType() {
        return type;
    }

    public UpdatesCheckerSeverity getSeverity() {
        return severity;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    /** @return the text in the given language. */
    public String getMessage(Locale locale) {
        return message.resolve(locale);
    }

    /** @return the text in the given language as HTML, with the emphasis the message carries and escaped values. */
    public String getMessageHtml(Locale locale) {
        return message.resolveHtml(locale);
    }

    /** @return the base (English) text. */
    public String getMessage() {
        return message.resolve(Locale.ROOT);
    }

    /** @return an allowed link (release page or advisory page), or {@code null}. */
    public String getLink() {
        return link;
    }

    /** @return the version a "Dismiss for version X.Y.Z" action refers to, or {@code null}. */
    public String getTargetVersion() {
        return targetVersion;
    }

    public List<Action> getActions() {
        return actions;
    }

    @Override
    public String toString() {
        return type + "/" + severity + "/" + discriminator;
    }
}

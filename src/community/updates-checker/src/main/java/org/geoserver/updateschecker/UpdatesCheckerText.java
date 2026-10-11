/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.Serializable;
import java.util.Locale;

/**
 * Words that are shown to a person: either a key into {@link UpdatesCheckerMessages} with its arguments, resolved in
 * whatever language is asked for only when the text is needed, or a literal (text that needs no translation). Immutable
 * and serializable, so it can be kept in a notification, a failure reason or a Wicket component.
 */
public final class UpdatesCheckerText implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String key;
    private final Object[] args;
    private final String literal;

    private UpdatesCheckerText(String key, Object[] args, String literal) {
        this.key = key;
        this.args = args;
        this.literal = literal;
    }

    /**
     * @param args what fills {@code {0}}, {@code {1}}, ...: strings, or other text that is itself resolved in the
     *     reader's language (so a translated sentence can contain translated parts)
     * @return text taken from the message bundle
     */
    public static UpdatesCheckerText of(String key, Object... args) {
        return new UpdatesCheckerText(key, args, null);
    }

    /** @return a text that is shown exactly as given, in every language */
    public static UpdatesCheckerText literal(String literal) {
        return new UpdatesCheckerText(null, new Object[0], literal);
    }

    /** @return the text in the given language; {@code Locale.ROOT} gives the base (English) text. */
    public String resolve(Locale locale) {
        return key == null ? literal : UpdatesCheckerMessages.get(locale, key, resolveArgs(locale));
    }

    /** @return the text in the given language as HTML, see {@link UpdatesCheckerMessages#getHtml}. */
    public String resolveHtml(Locale locale) {
        return key == null
                ? UpdatesCheckerMessages.escapeHtml(literal)
                : UpdatesCheckerMessages.getHtml(locale, key, resolveArgs(locale));
    }

    /** The arguments as plain text: a nested text is resolved in the same language. */
    private Object[] resolveArgs(Locale locale) {
        Object[] resolved = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            resolved[i] =
                    args[i] instanceof UpdatesCheckerText ? ((UpdatesCheckerText) args[i]).resolve(locale) : args[i];
        }
        return resolved;
    }

    /** @return the base (English) text, for logs and other output that belongs to the server, not to a user. */
    public String base() {
        return resolve(Locale.ROOT);
    }

    @Override
    public String toString() {
        return base();
    }
}

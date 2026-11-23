/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.Serializable;
import java.util.Locale;

/**
 * Text for a person: a {@link Messages} key and its arguments, resolved in the reader's language when asked, or a
 * literal shown as is. Serializable, so a Wicket component can hold it.
 */
public final class Text implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String key;
    private final Object[] args;
    private final String literal;

    private Text(String key, Object[] args, String literal) {
        this.key = key;
        this.args = args;
        this.literal = literal;
    }

    /** @param args strings, or other {@code Text} resolved in the same language */
    public static Text of(String key, Object... args) {
        return new Text(key, args, null);
    }

    public static Text literal(String literal) {
        return new Text(null, new Object[0], literal);
    }

    public String resolve(Locale locale) {
        return key == null ? literal : Messages.get(locale, key, resolveArgs(locale));
    }

    /** @see Messages#getHtml */
    public String resolveHtml(Locale locale) {
        return key == null ? Messages.escapeHtml(literal) : Messages.getHtml(locale, key, resolveArgs(locale));
    }

    private Object[] resolveArgs(Locale locale) {
        Object[] resolved = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            resolved[i] = args[i] instanceof Text ? ((Text) args[i]).resolve(locale) : args[i];
        }
        return resolved;
    }

    /** @return the English text, for log lines */
    public String base() {
        return resolve(Locale.ROOT);
    }

    @Override
    public String toString() {
        return base();
    }
}

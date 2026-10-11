/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The module's user-facing text, looked up under {@code UpdatesChecker.text.} in
 * {@code GeoServerApplication.properties} (and a translation such as {@code GeoServerApplication_fr.properties}), like
 * the labels of the settings page.
 *
 * <p>Used wherever there is no Wicket component to ask: the notifications themselves, the REST response and the result
 * lines in the log. Placeholders are {@code {0}}, {@code {1}}, ... and are simply replaced, so quotes and other
 * {@code MessageFormat} syntax have no special meaning and named placeholders in a configured text such as
 * {@code {version}} are left alone. Log lines use the base (English) text, not the language of any one user.
 */
public final class UpdatesCheckerMessages {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)\\}");

    /** The only markup a message may carry: emphasis, which plain text drops. */
    private static final Pattern EMPHASIS_TAG = Pattern.compile("</?[ib]>");

    private static final String BUNDLE = "GeoServerApplication";

    private static final String KEY_PREFIX = "UpdatesChecker.text.";

    private static final int MAX_CACHED_LANGUAGES = 50;

    /** The texts of each language, read once: the file of every module on the classpath is merged, as Wicket does. */
    private static final Map<Locale, Map<String, String>> TEXTS = new ConcurrentHashMap<>();

    private UpdatesCheckerMessages() {}

    /**
     * @param locale the language to use; {@code Locale.ROOT} gives the base text. A missing translation falls back to
     *     the base text, never to the JVM's default language.
     * @return the text for the key with the arguments filled in; the key itself if it is not defined
     */
    public static String get(Locale locale, String key, Object... args) {
        return format(locale, key, false, args);
    }

    /**
     * @return the text as HTML: the emphasis markup written in the message itself is kept, and every argument is
     *     escaped, so a value that came from the feed can never add markup
     */
    public static String getHtml(Locale locale, String key, Object... args) {
        return format(locale, key, true, args);
    }

    /** @return the text with {@code &}, {@code <}, {@code >} and quotes made safe to put in HTML. */
    public static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String format(Locale locale, String key, boolean html, Object... args) {
        String text = texts(locale == null ? Locale.ROOT : locale).get(key);
        if (text == null) {
            return key;
        }
        if (!html) {
            text = EMPHASIS_TAG.matcher(text).replaceAll("");
        }
        // one pass, so an argument that happens to contain "{1}" is never expanded by a later argument
        Matcher placeholder = PLACEHOLDER.matcher(text);
        StringBuffer out = new StringBuffer();
        while (placeholder.find()) {
            int index = Integer.parseInt(placeholder.group(1));
            String replacement = index < args.length ? String.valueOf(args[index]) : placeholder.group();
            if (html && index < args.length) {
                replacement = escapeHtml(replacement);
            }
            placeholder.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        placeholder.appendTail(out);
        return out.toString();
    }

    /** The cache is capped, because the language of a REST call comes from the caller's Accept-Language header. */
    private static Map<String, String> texts(Locale locale) {
        Map<String, String> texts = TEXTS.get(locale);
        if (texts == null) {
            texts = load(locale);
            if (TEXTS.size() < MAX_CACHED_LANGUAGES) {
                TEXTS.put(locale, texts);
            }
        }
        return texts;
    }

    /** Reads the base text, then lets the file for the language and for the language with country replace it. */
    private static Map<String, String> load(Locale locale) {
        List<String> suffixes = new ArrayList<>();
        suffixes.add("");
        if (!locale.getLanguage().isEmpty()) {
            suffixes.add("_" + locale.getLanguage());
            if (!locale.getCountry().isEmpty()) {
                suffixes.add("_" + locale.getLanguage() + "_" + locale.getCountry());
            }
        }
        Map<String, String> texts = new HashMap<>();
        for (String suffix : suffixes) {
            Properties properties = read(BUNDLE + suffix + ".properties");
            for (String name : properties.stringPropertyNames()) {
                if (name.startsWith(KEY_PREFIX)) {
                    texts.put(name.substring(KEY_PREFIX.length()), properties.getProperty(name));
                }
            }
        }
        return texts;
    }

    private static Properties read(String resource) {
        Properties merged = new Properties();
        try {
            Enumeration<URL> urls =
                    UpdatesCheckerMessages.class.getClassLoader().getResources(resource);
            for (URL url : Collections.list(urls)) {
                try (InputStream in = url.openStream()) {
                    merged.load(in);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return merged;
    }

    /** @return the base (English) text, for log lines and other output that belongs to the server, not a user. */
    public static String base(String key, Object... args) {
        return get(Locale.ROOT, key, args);
    }
}

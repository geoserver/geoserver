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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The text under {@code UpdatesChecker.text.} in {@code GeoServerApplication.properties} and its translations, for the
 * notifications, the REST response and the log lines, which have no Wicket component to ask. Only {@code {0}},
 * {@code {1}}, ... are replaced; quotes have no special meaning, unlike in {@code MessageFormat}.
 */
public final class Messages {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)\\}");

    /** The only markup a message may carry. */
    private static final Pattern EMPHASIS_TAG = Pattern.compile("</?[ib]>");

    private static final String BUNDLE = "GeoServerApplication";

    private static final String KEY_PREFIX = "UpdatesChecker.text.";

    /** The language of a REST call comes from its Accept-Language header, so the cache is capped. */
    private static final int MAX_CACHED_LANGUAGES = 50;

    private static final Map<Locale, Map<String, String>> TEXTS = new ConcurrentHashMap<>();

    private Messages() {}

    /**
     * @param locale the language; {@code Locale.ROOT} or a missing translation gives the English text
     * @return the text with the arguments filled in, or the key if it is not defined
     */
    public static String get(Locale locale, String key, Object... args) {
        return format(locale, key, false, args);
    }

    /** @return the text as HTML: the message's own emphasis is kept and every argument is escaped */
    public static String getHtml(Locale locale, String key, Object... args) {
        return format(locale, key, true, args);
    }

    /** @return the English text, for log lines */
    public static String base(String key, Object... args) {
        return get(Locale.ROOT, key, args);
    }

    public static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** @return the text with control, line-break, zero-width and bidirectional characters replaced by spaces */
    public static String forLog(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append(isUnsafeInLog(c) ? ' ' : c);
        }
        return out.toString();
    }

    private static boolean isUnsafeInLog(char c) {
        return Character.isISOControl(c)
                || c == 0x2028
                || c == 0x2029
                || (c >= 0x200B && c <= 0x200F)
                || (c >= 0x202A && c <= 0x202E)
                || (c >= 0x2066 && c <= 0x2069)
                || c == 0xFEFF;
    }

    private static String format(Locale locale, String key, boolean html, Object... args) {
        String text = texts(locale == null ? Locale.ROOT : locale).get(key);
        if (text == null) {
            return key;
        }
        if (!html) {
            text = EMPHASIS_TAG.matcher(text).replaceAll("");
        }
        // one pass, so an argument containing "{1}" is never expanded by a later argument
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

    /** Reads the English text, then the language, then the language with country, each replacing the one before. */
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

    /** Merges the file of every module on the classpath, as Wicket does. */
    private static Properties read(String resource) {
        Properties merged = new Properties();
        try {
            for (URL url : Collections.list(Messages.class.getClassLoader().getResources(resource))) {
                try (InputStream in = url.openStream()) {
                    merged.load(in);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return merged;
    }
}

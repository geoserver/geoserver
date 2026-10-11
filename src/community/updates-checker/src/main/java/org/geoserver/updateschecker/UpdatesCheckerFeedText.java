/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Helpers for treating the downloaded feed as untrusted input: length limits, links restricted to a few hosts, and
 * log-safe text. Feed text is also always escaped when rendered; Wicket labels do that by default.
 */
public final class UpdatesCheckerFeedText {

    /** Longest feed string kept; anything longer is truncated. */
    public static final int MAX_STRING_LENGTH = 2000;

    /** The hosts links may point to, with the path every link on that host must start with. */
    private static final Map<String, String> ALLOWED_LINK_PATHS = Map.of(
            "geoserver.org", "/",
            "github.com", "/geoserver/",
            "cdn.jsdelivr.net", "/gh/geoserver/");

    private static final Pattern GHSA_ID = Pattern.compile("^GHSA-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}$");

    private static final Pattern CVE_ID = Pattern.compile("^CVE-\\d{4}-\\d{4,7}$");

    private UpdatesCheckerFeedText() {}

    /** @return the text cut to {@link #MAX_STRING_LENGTH} characters; {@code null} stays {@code null}. */
    public static String truncate(String text) {
        if (text == null || text.length() <= MAX_STRING_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_STRING_LENGTH);
    }

    /** @return the text with control characters and line breaks replaced by spaces, so it cannot forge log lines. */
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

    /** Control characters, line and paragraph separators, zero-width and bidirectional formatting characters. */
    private static boolean isUnsafeInLog(char c) {
        return Character.isISOControl(c)
                || c == 0x2028
                || c == 0x2029
                || (c >= 0x200B && c <= 0x200F)
                || (c >= 0x202A && c <= 0x202E)
                || (c >= 0x2066 && c <= 0x2069)
                || c == 0xFEFF;
    }

    /**
     * @return the link if it is {@code https}, has no user info, and points to the geoserver.org site or to a path of
     *     the {@code geoserver} organisation on GitHub or jsDelivr, otherwise {@code null}.
     */
    public static String safeLink(String link) {
        if (link == null || link.length() > MAX_STRING_LENGTH) {
            return null;
        }
        try {
            URI uri = new URI(link.trim());
            String host = uri.getHost();
            String prefix = host == null ? null : ALLOWED_LINK_PATHS.get(host.toLowerCase(Locale.ROOT));
            String path = uri.getPath();
            if ("https".equalsIgnoreCase(uri.getScheme())
                    && prefix != null
                    && uri.getUserInfo() == null
                    && path != null
                    && path.startsWith(prefix)
                    && Arrays.stream(path.split("/"))
                            .noneMatch(segment -> segment.equals("..") || segment.equals("."))) {
                return uri.toASCIIString();
            }
        } catch (Exception e) {
            // not a usable link
        }
        return null;
    }

    /** @return whether the text is a GitHub advisory id such as {@code GHSA-jvpx-6qxg-whgc}. */
    public static boolean isGhsaId(String text) {
        return text != null && GHSA_ID.matcher(text).matches();
    }

    /** @return whether the text is a CVE id such as {@code CVE-2026-76904}. */
    public static boolean isCveId(String text) {
        return text != null && CVE_ID.matcher(text).matches();
    }

    /** @return the advisory page link, built from a validated id and never taken from the feed text. */
    public static String ghsaLink(String ghsaId) {
        return isGhsaId(ghsaId) ? "https://github.com/geoserver/geoserver/security/advisories/" + ghsaId : null;
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * The security advisories (CVE or GHSA ids) that the server administrator has mitigated, so the module does not report
 * them. The list comes from the {@link #PROPERTY} application property, so only whoever starts the server can set it; a
 * REST call may replace it for that call alone, see {@link #fromRequest}.
 *
 * <p>Only advisory notices are affected. A newer release that was announced as a security release is still reported,
 * because the feed does not say which advisories it fixes.
 */
public final class UpdatesCheckerSuppression {

    /** Application property (system property, context parameter or environment variable) naming the advisories. */
    public static final String PROPERTY = "UPDATES_CHECKER_SUPPRESSED_CVES";

    /** The REST request member that replaces the property for one call. */
    public static final String REQUEST_MEMBER = "suppressedCves";

    static final int MAX_IDS = 100;

    private static final UpdatesCheckerSuppression NONE = new UpdatesCheckerSuppression(Collections.emptySet());

    private final Set<String> ids;

    private UpdatesCheckerSuppression(Set<String> ids) {
        this.ids = ids;
    }

    public static UpdatesCheckerSuppression none() {
        return NONE;
    }

    /**
     * @param text ids separated by commas, semicolons or white space; may be {@code null}
     * @param rejected receives what is not a CVE or GHSA id, and what is beyond {@link #MAX_IDS}
     */
    public static UpdatesCheckerSuppression parse(String text, List<String> rejected) {
        if (text == null || text.isBlank()) {
            return NONE;
        }
        return of(List.of(text.trim().split("[,;\\s]+")), rejected);
    }

    /** As {@link #parse}, for ids that are already separate. */
    static UpdatesCheckerSuppression of(List<String> tokens, List<String> rejected) {
        Set<String> ids = new TreeSet<>();
        for (String token : tokens) {
            String id = normalise(token.trim());
            if (id == null || ids.size() >= MAX_IDS && !ids.contains(id)) {
                rejected.add(UpdatesCheckerFeedText.forLog(UpdatesCheckerFeedText.truncate(token)));
            } else {
                ids.add(id);
            }
        }
        return ids.isEmpty() ? NONE : new UpdatesCheckerSuppression(Collections.unmodifiableSet(ids));
    }

    /** @return the id in the form the feed uses (CVE in capitals, GHSA with a lower case body), or {@code null}. */
    private static String normalise(String token) {
        String upper = token.toUpperCase(Locale.ROOT);
        String id = upper.startsWith("GHSA-") ? "GHSA-" + token.substring(5).toLowerCase(Locale.ROOT) : upper;
        return UpdatesCheckerFeedText.isCveId(id) || UpdatesCheckerFeedText.isGhsaId(id) ? id : null;
    }

    /**
     * Reads the body of a REST call.
     *
     * @param body the request body, or {@code null}
     * @return the list that replaces the property for this call, or {@code null} if the body does not supply one: an
     *     empty body, an object without {@link #REQUEST_MEMBER}, or one where it is JSON null. An empty array supplies
     *     an empty list, which suppresses nothing.
     * @throws IllegalArgumentException with a message for the caller if the body is not a JSON object, the member is
     *     not an array of text, or it holds something that is not a CVE or GHSA id
     */
    public static UpdatesCheckerSuppression fromRequest(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        FeedJson json;
        try {
            json = FeedJson.parse(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The request body is not valid JSON");
        }
        if (!json.isObject()) {
            throw new IllegalArgumentException("The request body must be a JSON object");
        }
        FeedJson member = json.get(REQUEST_MEMBER);
        if (member == null) {
            return null;
        }
        if (!member.isArray()) {
            throw new IllegalArgumentException(REQUEST_MEMBER + " must be an array of CVE or GHSA ids");
        }
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < member.size(); i++) {
            FeedJson element = member.get(i);
            if (element == null || !element.isTextual()) {
                throw new IllegalArgumentException(REQUEST_MEMBER + " must hold only text");
            }
            tokens.add(element.textValue());
        }
        List<String> rejected = new ArrayList<>();
        UpdatesCheckerSuppression suppression = of(tokens, rejected);
        if (!rejected.isEmpty()) {
            throw new IllegalArgumentException("Not CVE or GHSA ids, or more than " + MAX_IDS + ": " + rejected);
        }
        return suppression;
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    /** @return the ids, sorted. */
    public List<String> ids() {
        return new ArrayList<>(ids);
    }

    /** @return whether the advisory carries one of the ids, as its CVE id or its GHSA id. */
    boolean suppresses(UpdatesCheckerFeed.Advisory advisory) {
        return advisory.getCveId() != null && ids.contains(advisory.getCveId())
                || advisory.getGhsaId() != null && ids.contains(advisory.getGhsaId());
    }

    @Override
    public String toString() {
        return String.join(", ", ids);
    }
}

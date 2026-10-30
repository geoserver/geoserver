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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The advisories (CVE or GHSA ids) the server administrator has mitigated, so they are not reported. They come from the
 * {@link #PROPERTY} application property, which only whoever starts the server can set; a REST call may replace them
 * for that call alone. Security release notices are still shown, because the feed does not say which advisories a
 * release fixes.
 */
public final class Suppression {

    /** Application property: system property, context parameter or environment variable. */
    public static final String PROPERTY = "UPDATES_CHECKER_SUPPRESSED_CVES";

    /** The REST request member that replaces the property for one call. */
    public static final String REQUEST_MEMBER = "suppressedCves";

    static final int MAX_IDS = 100;

    private static final Suppression NONE = new Suppression(Collections.emptySet());

    private final Set<String> ids;

    private Suppression(Set<String> ids) {
        this.ids = ids;
    }

    public static Suppression none() {
        return NONE;
    }

    /**
     * @param text ids separated by commas, semicolons or white space; may be {@code null}
     * @param rejected receives what is not a CVE or GHSA id, and what is beyond {@link #MAX_IDS}
     */
    public static Suppression parse(String text, List<String> rejected) {
        if (text == null || text.isBlank()) {
            return NONE;
        }
        return of(List.of(text.trim().split("[,;\\s]+")), rejected);
    }

    static Suppression of(List<String> tokens, List<String> rejected) {
        Set<String> ids = new TreeSet<>();
        for (String token : tokens) {
            String id = normalise(token.trim());
            if (id == null || ids.size() >= MAX_IDS && !ids.contains(id)) {
                rejected.add(Messages.forLog(Feed.truncate(token)));
            } else {
                ids.add(id);
            }
        }
        return ids.isEmpty() ? NONE : new Suppression(Collections.unmodifiableSet(ids));
    }

    /** @return the id as the feed writes it (CVE in capitals, GHSA body in lower case), or {@code null} */
    private static String normalise(String token) {
        String upper = token.toUpperCase(Locale.ROOT);
        String id = upper.startsWith("GHSA-") ? "GHSA-" + token.substring(5).toLowerCase(Locale.ROOT) : upper;
        return Feed.isCveId(id) || Feed.isGhsaId(id) ? id : null;
    }

    /**
     * @param body the REST request body, or {@code null}
     * @return what replaces the property for this call, or {@code null} if the body names nothing; an empty array
     *     suppresses nothing
     * @throws IllegalArgumentException with a message for the caller if the body is not as documented
     */
    public static Suppression fromRequest(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Object json;
        try {
            json = Feed.parseJson(body);
        } catch (JSONException | StackOverflowError e) {
            throw new IllegalArgumentException("The request body is not valid JSON");
        }
        if (!(json instanceof JSONObject)) {
            throw new IllegalArgumentException("The request body must be a JSON object");
        }
        Object member = ((JSONObject) json).opt(REQUEST_MEMBER);
        if (member == null || JSONObject.NULL.equals(member)) {
            return null;
        }
        if (!(member instanceof JSONArray)) {
            throw new IllegalArgumentException(REQUEST_MEMBER + " must be an array of CVE or GHSA ids");
        }
        List<String> tokens = new ArrayList<>();
        for (Object element : (JSONArray) member) {
            if (!(element instanceof String)) {
                throw new IllegalArgumentException(REQUEST_MEMBER + " must hold only text");
            }
            tokens.add((String) element);
        }
        List<String> rejected = new ArrayList<>();
        Suppression suppression = of(tokens, rejected);
        if (!rejected.isEmpty()) {
            throw new IllegalArgumentException("Not CVE or GHSA ids, or more than " + MAX_IDS + ": " + rejected);
        }
        return suppression;
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    /** @return whether the advisory's CVE id or GHSA id is one of the ids */
    boolean suppresses(Feed.Advisory advisory) {
        return advisory.getCveId() != null && ids.contains(advisory.getCveId())
                || advisory.getGhsaId() != null && ids.contains(advisory.getGhsaId());
    }

    @Override
    public String toString() {
        return String.join(", ", ids);
    }
}

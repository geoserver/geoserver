/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;

/**
 * Support class for building links required for pagination (plus the self link) based on <code>
 * startIndex</code> and <code>limit</code> parameters
 */
public class PaginationLinksBuilder {

    private final String path;
    private final long startIndex;
    private final int maxFeatures;
    private final int returned;
    private final boolean hasNextPage;

    /**
     * Constructor variant that can be used when the number of matched features is known exactly, will use it to
     * determine the presence of a next page
     */
    public PaginationLinksBuilder(String path, long startIndex, int limit, int returned, long matched) {
        this.path = path;
        this.startIndex = startIndex;
        this.maxFeatures = limit;
        this.returned = returned;
        // if nothing is returned or we returned less than a page, there is no next page
        this.hasNextPage = returned > 0 && (startIndex + returned < matched);
    }

    /**
     * Constructor variant that can take the number of matched features as a BigInteger and a flag for the presence of a
     * next page, and will use the one that's not null to compute the next link
     */
    public PaginationLinksBuilder(
            String path, long startIndex, int limit, int returned, BigInteger matched, Boolean hasNextPage) {
        this.path = path;
        this.startIndex = startIndex;
        this.maxFeatures = limit;
        this.returned = returned;
        if (matched != null) {
            // if nothing is returned, or we returned less than a page, there is no next page
            this.hasNextPage = returned > 0 && (startIndex + returned < matched.longValue());
        } else {
            this.hasNextPage = Boolean.TRUE.equals(hasNextPage);
        }
    }

    /**
     * Constructor variant that can be used when the number of matched features is not known, but we know whether there
     * is a next page or not
     */
    public PaginationLinksBuilder(String path, long startIndex, int limit, int returned, boolean hasNextPage) {
        this.path = path;
        this.startIndex = startIndex;
        this.maxFeatures = limit;
        this.returned = returned;
        this.hasNextPage = hasNextPage;
    }

    /** Returns a HREF to the previous page */
    public String getPrevious() {
        Map<String, Object> kvp = getPreviousMap(true);
        if (kvp == null) return null;

        return buildURL(kvp);
    }

    /**
     * Map of KVP for the previous link. Can be used in POST links generation.
     *
     * @return
     */
    public Map<String, Object> getPreviousMap(boolean includeQueryMap) {
        // if first page, no previous link
        if (startIndex <= 0) return null;

        // previous offset calculated as the current offset - maxFeatures, or 0 if this is a
        // negative value, while  previous count should be current offset - previousOffset
        Map<String, Object> kvp = new LinkedHashMap<>();
        if (includeQueryMap) kvp.putAll(APIRequestInfo.get().getSimpleQueryMap());
        long prevOffset = Math.max(startIndex - maxFeatures, 0);
        kvp.put("startIndex", prevOffset);
        kvp.put("limit", startIndex - prevOffset);
        return kvp;
    }

    /** Returns a HREF to the next page */
    public String getNext() {
        Map<String, Object> kvp = getNextMap(true);
        if (kvp == null) return null;

        return buildURL(kvp);
    }

    /** Map of KVP for the next link. Can be used in POST links generation. */
    public Map<String, Object> getNextMap(boolean includeQueryMap) {
        // if nothing is returned or we returned less than a page, there is no next page
        if (!hasNextPage) return null;

        Map<String, Object> kvp = new LinkedHashMap<>();
        if (includeQueryMap) kvp.putAll(APIRequestInfo.get().getSimpleQueryMap());
        kvp.put("startIndex", startIndex + returned);
        kvp.put("limit", maxFeatures);
        return kvp;
    }

    /** Returns a HREF to the current page. */
    public String getSelf() {
        Map<String, Object> kvp = new LinkedHashMap<>();
        kvp.putAll(APIRequestInfo.get().getSimpleQueryMap());
        return buildURL(kvp);
    }

    private String buildURL(Map<String, Object> kvp) {
        Map<String, String> kvps = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : kvp.entrySet()) {
            kvps.put(
                    e.getKey(),
                    Optional.ofNullable(e.getValue())
                            .map(v -> String.valueOf(v))
                            .orElse(null));
        }
        return ResponseUtils.buildURL(APIRequestInfo.get().getBaseURL(), path, kvps, URLMangler.URLType.SERVICE);
    }
}

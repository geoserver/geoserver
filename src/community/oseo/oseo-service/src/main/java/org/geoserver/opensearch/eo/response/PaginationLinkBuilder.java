/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import static org.geoserver.ows.URLMangler.URLType.SERVICE;

import java.util.LinkedHashMap;
import java.util.Map;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchParameters;
import org.geoserver.opensearch.eo.SearchRequest;
import org.geoserver.opensearch.eo.SearchResults;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.data.Parameter;

/**
 * Can build all the pagination links, given the search results and a target mime type. HREFs
 * returned can be null, in that case the link is not meant to be produced (e.g., previous page when
 * the current request is hitting the first page)
 */
public class PaginationLinkBuilder {

    private final SearchRequest request;
    private final String mimeType;
    private final OSEOInfo info;
    private String first;
    private String self;
    private String previous;
    private String next;
    private String last;

    public PaginationLinkBuilder(SearchResults results, OSEOInfo info, String mimeType) {
        this.request = results.getRequest();
        this.mimeType = mimeType;
        this.info = info;

        int total = results.getTotalResults();
        int startIndex = getQueryStartIndex(results) + 1;
        int itemsPerPage = request.getQuery().getMaxFeatures();

        // warning, opensearch is 1-based, geotools is 0 based
        self = encodePaginationLink(startIndex, itemsPerPage);
        first = encodePaginationLink(1, itemsPerPage);
        if (startIndex > 1) {
            previous = encodePaginationLink(Math.max(startIndex - itemsPerPage, 1), itemsPerPage);
        }
        if (startIndex + itemsPerPage <= total) {
            next = encodePaginationLink(startIndex + itemsPerPage, itemsPerPage);
        }
        last = encodePaginationLink(getLastPageStart(total, itemsPerPage), itemsPerPage);
    }

    private String encodePaginationLink(int startIndex, int itemsPerPage) {
        String baseURL = request.getBaseUrl();
        Map<String, String> kvp = new LinkedHashMap<>();
        for (Map.Entry<Parameter, String> entry : request.getSearchParameters().entrySet()) {
            Parameter parameter = entry.getKey();
            String value = entry.getValue();
            String key = OpenSearchParameters.getQualifiedParamName(info, parameter, false);
            kvp.put(key, value);
        }
        kvp.put("startIndex", "" + startIndex);
        kvp.put("count", "" + itemsPerPage);
        kvp.put("httpAccept", mimeType);
        return ResponseUtils.buildURL(baseURL, "oseo/search", kvp, SERVICE);
    }

    private int getLastPageStart(int total, int itemsPerPage) {
        // all in one page?
        if (total <= itemsPerPage || itemsPerPage == 0) {
            return 1;
        }
        // check how many items in the last page, is the last page partial or full?
        int lastPageItems = total % itemsPerPage;
        if (lastPageItems == 0) {
            lastPageItems = itemsPerPage;
        }
        return total - lastPageItems + 1;
    }

    private int getQueryStartIndex(SearchResults results) {
        Integer startIndex = results.getRequest().getQuery().getStartIndex();
        if (startIndex == null) {
            startIndex = 0;
        }
        return startIndex;
    }

    public String getFirst() {
        return first;
    }

    public String getSelf() {
        return self;
    }

    public String getPrevious() {
        return previous;
    }

    public String getNext() {
        return next;
    }

    public String getLast() {
        return last;
    }
}

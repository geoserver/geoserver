/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.geotools.feature.FeatureCollection;

/**
 * Represents the result of a search request against collections or products
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SearchResults {

    SearchRequest request;

    FeatureCollection results;

    private Integer totalResults;

    private final boolean nextPage;

    public SearchResults(SearchRequest request, FeatureCollection results, Integer totalResults, boolean nextPage) {
        super();
        this.request = request;
        this.results = results;
        this.totalResults = totalResults;
        this.nextPage = nextPage;
    }

    /** The originating request */
    public SearchRequest getRequest() {
        return request;
    }

    /** The search results */
    public FeatureCollection getResults() {
        return results;
    }

    /** Total number of matched features */
    public Integer getTotalResults() {
        return totalResults;
    }

    public boolean hasNextPage() {
        return nextPage;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

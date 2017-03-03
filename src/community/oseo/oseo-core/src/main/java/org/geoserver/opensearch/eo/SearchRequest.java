/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.geotools.data.Parameter;
import org.geotools.data.Query;

/**
 * A OpenSearch EO query, for either collections (no parent id) or products (with parent id)
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class SearchRequest {

    String parentId;

    Query query;

    String httpAccept;

    private Map<String, String> searchParameters;

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public String getHttpAccept() {
        return httpAccept;
    }

    public void setHttpAccept(String httpAccept) {
        this.httpAccept = httpAccept;
    }

    public void setSearchParameters(Map<String, String> searchParameters) {
        this.searchParameters = searchParameters;
    }
    
    public Map<String, String> getSearchParameters() {
        return searchParameters;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

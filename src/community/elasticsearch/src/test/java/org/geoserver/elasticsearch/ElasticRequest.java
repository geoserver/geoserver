/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** ElasticSearch request */
class ElasticRequest {

    private Map<String, Object> query;

    private Map<String, Map<String, Map<String, Object>>> aggregations;

    private Integer size;

    private Integer from;

    private Integer scroll;

    private final List<Map<String, Object>> sorts;

    private final List<String> sourceIncludes;

    private final List<String> fields;

    public ElasticRequest() {
        this.sorts = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.sourceIncludes = new ArrayList<>();
    }

    public Map<String, Object> getQuery() {
        return query;
    }

    public void setQuery(Map<String, Object> query) {
        this.query = query;
    }

    public Map<String, Map<String, Map<String, Object>>> getAggregations() {
        return aggregations;
    }

    public void setAggregations(Map<String, Map<String, Map<String, Object>>> aggregations) {
        this.aggregations = aggregations;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getScroll() {
        return scroll;
    }

    public void setScroll(Integer scroll) {
        this.scroll = scroll;
    }

    public List<Map<String, Object>> getSorts() {
        return sorts;
    }

    public void addSort(String key, String order) {
        this.sorts.add(Collections.singletonMap(key, Collections.singletonMap("order", order)));
    }

    public List<String> getSourceIncludes() {
        return sourceIncludes;
    }

    public void addSourceInclude(String sourceInclude) {
        this.sourceIncludes.add(sourceInclude);
    }

    public List<String> getFields() {
        return fields;
    }

    public void addField(String field) {
        this.fields.add(field);
    }

    @Override
    public String toString() {
        return "ElasticRequest{"
                + "query="
                + query
                + ", aggregations="
                + aggregations
                + ", size="
                + size
                + ", from="
                + from
                + ", scroll="
                + scroll
                + ", sorts="
                + sorts
                + ", sourceIncludes="
                + sourceIncludes
                + ", fields="
                + fields
                + '}';
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.locationtech.jts.geom.Geometry;

/** Represents a STAC query used in POST requests */
public class SearchQuery {

    private List<String> collections;
    private double[] bbox;
    private Integer limit;
    private String datetime;
    private String filter;
    private Integer startIndex;
    Geometry intersects;

    @JsonProperty("filter-lang")
    String filterLang;

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> collections) {
        this.collections = collections;
    }

    public double[] getBbox() {
        return bbox;
    }

    public void setBbox(double[] bbox) {
        this.bbox = bbox;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getFilter() {
        return filter;
    }

    // Using JsonNode to prevent Jackson from trying to parse into object if is CQL2-JSON
    public void setFilter(JsonNode node) {
        if (node instanceof ObjectNode) {
            this.filter = node.toString();
        } else {
            this.filter = node.textValue();
        }
    }

    public String getFilterLang() {
        return filterLang;
    }

    public void setFilterLang(String filterLang) {
        this.filterLang = filterLang;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Geometry getIntersects() {
        return intersects;
    }

    public void setIntersects(Geometry intersects) {
        this.intersects = intersects;
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    Geometry intersection;

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

    public void setFilter(String filter) {
        this.filter = filter;
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

    public Geometry getIntersection() {
        return intersection;
    }

    public void setIntersection(Geometry intersection) {
        this.intersection = intersection;
    }
}

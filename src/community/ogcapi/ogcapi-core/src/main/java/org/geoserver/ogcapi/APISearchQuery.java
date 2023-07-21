/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opengis.filter.sort.SortBy;

/** Represents a search query used in POST requests */
public class APISearchQuery {

    private BigInteger limit;
    private BigInteger startIndex;
    private String crs;
    private String bbox;

    @JsonProperty("bbox-crs")
    private String bboxCRS;

    private String datetime;
    private List<String> ids;
    private SortBy[] sortBy;
    private String filter;

    @JsonProperty("filter-crs")
    private String filterCRS;

    @JsonProperty("filter-lang")
    private String filterLang;

    public BigInteger getLimit() {
        return limit;
    }

    public void setLimit(BigInteger limit) {
        this.limit = limit;
    }

    public BigInteger getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(BigInteger startIndex) {
        this.startIndex = startIndex;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public String getBbox() {
        return bbox;
    }

    // support passing bbox as array and as string
    public void setBbox(JsonNode node) {
        if (node instanceof ArrayNode) {
            this.bbox = arrayNodeToString((ArrayNode) node);
        } else {
            this.bbox = node.textValue();
        }
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getBboxCRS() {
        return bboxCRS;
    }

    public void setBboxCRS(String bboxCRS) {
        this.bboxCRS = bboxCRS;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ImmutableList.copyOf(ids.split(","));
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public SortBy[] getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortBy[] sortBy) {
        this.sortBy = sortBy;
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

    public String getFilterCRS() {
        return filterCRS;
    }

    public void setFilterCRS(String filterCRS) {
        this.filterCRS = filterCRS;
    }

    private String arrayNodeToString(ArrayNode node) {
        final List<String> values = new ArrayList<>(node.size());
        node.forEach(childNode -> values.add(childNode.toString()));
        return values.stream().collect(Collectors.joining(","));
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.math.BigInteger;
import java.util.List;
import org.geoserver.ogcapi.APISearchQuery;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.SortByImpl;
import org.locationtech.jts.geom.Geometry;

/** Represents a STAC query used in POST requests */
public class STACSearchQuery extends APISearchQuery {
    public static final String STAC_SORTBY_FIELD = "field";
    public static final String STAC_SORTBY_DIRECTION = "direction";
    public static final String STAC_SORTBY_ASC = "asc";
    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private List<String> collections;
    Geometry intersects;

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> collections) {
        this.collections = collections;
    }

    public Geometry getIntersects() {
        return intersects;
    }

    public void setIntersects(Geometry intersects) {
        this.intersects = intersects;
    }

    public Integer getLimitAsInt() {
        BigInteger limit = getLimit();
        if (limit == null) {
            return null;
        }
        return limit.intValue();
    }

    public Integer getStartIndexAsInt() {
        BigInteger startIndex = getStartIndex();
        if (startIndex == null) {
            return null;
        }
        return startIndex.intValue();
    }

    @Override
    public void setSortBy(JsonNode node) {
        // Based on STAC API Extension for Sort
        // https://github.com/stac-api-extensions/sort?tab=readme-ov-file#http-post-json-entity
        if (node instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) node;
            SortBy[] sortBIES = new SortBy[arrayNode.size()];
            for (int i = 0; i < arrayNode.size(); i++) {
                if (arrayNode.get(i).isObject()
                        && arrayNode.get(i).has(STAC_SORTBY_FIELD)
                        && arrayNode.get(i).has(STAC_SORTBY_DIRECTION)) {
                    SortOrder direction =
                            arrayNode
                                            .get(i)
                                            .get(STAC_SORTBY_DIRECTION)
                                            .asText()
                                            .equalsIgnoreCase(STAC_SORTBY_ASC)
                                    ? SortOrder.ASCENDING
                                    : SortOrder.DESCENDING;
                    PropertyName field =
                            FF.property(arrayNode.get(i).get(STAC_SORTBY_FIELD).asText());
                    SortBy sortBy = new SortByImpl(field, direction);
                    sortBIES[i] = sortBy;
                } else {
                    throw new IllegalArgumentException("Invalid sortBy parameter");
                }
            }
            this.sortBy = sortBIES;
        } else {
            throw new IllegalArgumentException("Invalid sortBy parameter");
        }
    }
}

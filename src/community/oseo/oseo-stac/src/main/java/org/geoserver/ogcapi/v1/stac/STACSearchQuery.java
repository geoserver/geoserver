/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import java.math.BigInteger;
import java.util.List;
import org.geoserver.ogcapi.APISearchQuery;
import org.locationtech.jts.geom.Geometry;

/** Represents a STAC query used in POST requests */
public class STACSearchQuery extends APISearchQuery {

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
}

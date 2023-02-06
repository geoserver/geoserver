/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import java.math.BigInteger;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;

public class QueryResult extends AbstractQueryResult {

    private final Query query;

    public QueryResult(
            Query query, FeatureCollection items, BigInteger numberMatched, int returned) {
        super(items, numberMatched, returned);
        this.query = query;
    }

    public Query getQuery() {
        return query;
    }
}

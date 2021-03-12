/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.math.BigInteger;
import org.geotools.feature.FeatureCollection;

/**
 * Wrapper for the returned FeatureCollection containing the STAC Items definitions, along with the
 * counts of numbers matched and returned
 */
public abstract class AbstractQueryResult {

    private FeatureCollection items;
    private final int returned;
    private BigInteger numberMatched;

    public AbstractQueryResult(FeatureCollection items, BigInteger numberMatched, int returned) {
        this.items = items;
        this.numberMatched = numberMatched;
        this.returned = returned;
    }

    public FeatureCollection getItems() {
        return items;
    }

    public BigInteger getNumberMatched() {
        return numberMatched;
    }

    public int getReturned() {
        return returned;
    }
}

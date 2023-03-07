/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import java.math.BigInteger;
import org.geotools.feature.FeatureCollection;

/**
 * Wrapper for the returned FeatureCollection containing the STAC Items definitions, for a single
 * collection search.
 */
@JsonIgnoreType
public class ItemsResponse extends AbstractItemsResponse {

    private String collectionId;

    public ItemsResponse(
            String collectionId, FeatureCollection items, BigInteger numberMatched, int returned) {
        super(items, numberMatched, returned);
        this.collectionId = collectionId;
    }

    public String getCollectionId() {
        return collectionId;
    }
}

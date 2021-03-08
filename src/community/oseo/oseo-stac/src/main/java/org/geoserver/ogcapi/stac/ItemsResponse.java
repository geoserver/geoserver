/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.geotools.feature.FeatureCollection;

/**
 * Wrapper for the returned FeatureCollection containing the STAC Items definitions. Used because
 * Spring MVC response binding is driven by the response type.
 */
@JsonIgnoreType
public class ItemsResponse {

    private String collectionId;
    private FeatureCollection items;

    public ItemsResponse(String collectionId, FeatureCollection items) {
        this.items = items;
        this.collectionId = collectionId;
    }

    public FeatureCollection getItems() {
        return items;
    }

    public String getCollectionId() {
        return collectionId;
    }
}

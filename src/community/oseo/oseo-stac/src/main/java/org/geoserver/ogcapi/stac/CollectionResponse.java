/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.opengis.feature.Feature;

/**
 * Wrapper for the returned FeatureCollection containing a single STAC Collection definition. Used
 * because Spring MVC response binding is driven by the response type.
 */
@JsonIgnoreType
public class CollectionResponse {

    Feature collection;

    public CollectionResponse(Feature collection) {
        this.collection = collection;
    }

    public Feature getCollection() {
        return collection;
    }
}

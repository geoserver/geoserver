/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.geotools.feature.FeatureCollection;

/**
 * Wrapper for the returned FeatureCollection containing the STAC Collection definitions. Used
 * because Spring MVC response binding is driven by the response type.
 */
@JsonIgnoreType
public class CollectionsResponse {

    FeatureCollection collections;

    public CollectionsResponse(FeatureCollection collections) {
        this.collections = collections;
    }

    public FeatureCollection getCollections() {
        return collections;
    }
}

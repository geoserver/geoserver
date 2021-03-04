package org.geoserver.ogcapi.stac;

import org.geotools.feature.FeatureCollection;

/**
 * Wrapper for the returned FeatureCollection containing the STAC Collection definitions. Used
 * because Spring MVC response binding is driven by the response type.
 */
public class CollectionsResponse {

    FeatureCollection collections;

    public CollectionsResponse(FeatureCollection collections) {
        this.collections = collections;
    }

    public FeatureCollection getCollections() {
        return collections;
    }
}

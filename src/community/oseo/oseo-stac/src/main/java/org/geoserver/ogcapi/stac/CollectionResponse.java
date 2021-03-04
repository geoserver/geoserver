package org.geoserver.ogcapi.stac;

import org.opengis.feature.Feature;

/**
 * Wrapper for the returned FeatureCollection containing the STAC Collection definitions. Used
 * because Spring MVC response binding is driven by the response type.
 */
public class CollectionResponse {

    Feature collection;

    public CollectionResponse(Feature collection) {
        this.collection = collection;
    }

    public Feature getCollection() {
        return collection;
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.v1.stac.functions.EoSummaries;
import org.geotools.api.feature.Feature;

/**
 * Wrapper for the returned FeatureCollection containing a single STAC Collection definition. Used
 * because Spring MVC response binding is driven by the response type.
 */
@JsonIgnoreType
public class CollectionResponse extends AbstractDocument {

    Feature collection;
    EoSummaries eoSummaries;

    public CollectionResponse(Feature collection) {
        this.collection = collection;
        eoSummaries = new EoSummaries();
    }

    public Feature getCollection() {
        return collection;
    }

    /**
     * Returns the min, max, bounds, or distinct values of a property in a collection.
     *
     * @param aggregate min, max, bounds, or distinct
     * @param collectionIdentifier the collection identifier
     * @param property the property to aggregate
     * @return the min, max, bounds, or distinct values of a property in a collection
     */
    public Object eoSummaries(String aggregate, String collectionIdentifier, String property) {
        return eoSummaries.evaluate(aggregate, collectionIdentifier, property);
    }
}

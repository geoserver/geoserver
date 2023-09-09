/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.v1.stac.functions.EoSummaries;
import org.geotools.feature.FeatureCollection;

/**
 * Wrapper for the returned FeatureCollection containing the STAC Collection definitions. Used
 * because Spring MVC response binding is driven by the response type.
 */
@JsonIgnoreType
public class CollectionsResponse extends AbstractDocument {

    FeatureCollection collections;
    EoSummaries eoSummaries;

    public CollectionsResponse(FeatureCollection collections) {
        this.collections = collections;
        addSelfLinks("ogc/stac/v1/collections");
        eoSummaries = new EoSummaries();
    }

    public FeatureCollection getCollections() {
        return collections;
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

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.opengis.feature.Feature;

/**
 * Wrapper for the returned FeatureCollection containing a single STAC item definitions. Used
 * because Spring MVC response binding is driven by the response type.
 */
@JsonIgnoreType
public class ItemResponse {

    private Feature item;
    private String collectionId;
    private RootBuilder template;

    public ItemResponse(String collectionId, Feature item) {
        this.item = item;
        this.collectionId = collectionId;
    }

    public Feature getItem() {
        return item;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public RootBuilder getTemplate() {
        return template;
    }

    public void setTemplate(RootBuilder template) {
        this.template = template;
    }
}

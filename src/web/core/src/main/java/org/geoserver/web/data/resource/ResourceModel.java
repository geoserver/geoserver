/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * A model that serializes the layer fully, and re-attaches it to the catalog on deserialization
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class ResourceModel implements IModel<ResourceInfo> {
    ResourceInfo resourceInfo;

    public ResourceModel(ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    @Override
    public ResourceInfo getObject() {
        if (resourceInfo.getCatalog() == null)
            new CatalogBuilder(GeoServerApplication.get().getCatalog()).attach(resourceInfo);
        return resourceInfo;
    }

    @Override
    public void setObject(ResourceInfo object) {
        this.resourceInfo = object;
    }
}

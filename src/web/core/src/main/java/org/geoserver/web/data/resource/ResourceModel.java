package org.geoserver.web.data.resource;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * A model that serializes the layer fully, and re-attaches it to the catalog
 * on deserialization
 * @author Andrea Aime - OpenGeo
 *
 */
@SuppressWarnings("serial")
public class ResourceModel implements IModel {
    ResourceInfo resourceInfo;
    
    public ResourceModel(ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public Object getObject() {
        if(resourceInfo.getCatalog() == null)
            new CatalogBuilder(GeoServerApplication.get().getCatalog()).attach(resourceInfo);
        return resourceInfo;
    }

    public void setObject(Object object) {
        this.resourceInfo = (ResourceInfo) object;
    }

    public void detach() {
        // nothing specific to do
    }

    

}

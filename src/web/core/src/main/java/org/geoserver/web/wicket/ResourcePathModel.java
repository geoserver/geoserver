/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.web.GeoServerApplication;

/**
 * A model allowing to use a non serializable {@link Resource} in GeoServer. This class assumes the
 * resource store is the GeoServer main one, subclass in case you need to work against resources
 * coming from a different store
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class ResourcePathModel extends LoadableDetachableModel<Resource> {

    String path;

    public ResourcePathModel(Resource resource) {
        super(resource);
        this.path = resource.path();
    }

    @Override
    protected Resource load() {
        ResourceStore store = getResourceStore();
        return store.get(path);
    }

    /**
     * Returns the main {@link ResourceStore} for GeoServer. Subclasses can override in case they
     * are using a different resource store
     */
    protected ResourceStore getResourceStore() {
        ResourceStore store = (ResourceStore) GeoServerApplication.get().getBean("resourceStore");
        return store;
    }
}

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * A loadable model for layers. Warning, don't use it in a tabbed form or in any other places where
 * you might need to keep the modifications in a resource stable across page loads.
 */
@SuppressWarnings("serial")
public class LayerDetachableModel extends LoadableDetachableModel<LayerInfo> {
    String id;

    public LayerDetachableModel(LayerInfo layer) {
        super(layer);
        this.id = layer.getId();
    }

    @Override
    protected LayerInfo load() {
        return GeoServerApplication.get().getCatalog().getLayer(id);
    }
}

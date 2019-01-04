/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.GeoServerApplication;

/** Model for layer groups */
public class LayerGroupDetachableModel extends LoadableDetachableModel<LayerGroupInfo> {

    private static final long serialVersionUID = 1945014162826151239L;

    String id;
    LayerGroupInfo layerGroup;

    public LayerGroupDetachableModel(LayerGroupInfo layerGroup) {
        this.id = layerGroup.getId();
        if (id == null) {
            this.layerGroup = layerGroup;
        }
    }

    @Override
    protected LayerGroupInfo load() {
        if (id != null) {
            return GeoServerApplication.get().getCatalog().getLayerGroup(id);
        } else {
            return layerGroup;
        }
    }
}

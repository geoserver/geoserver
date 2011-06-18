/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Model for layer groups
 */
@SuppressWarnings("serial")
public class LayerGroupDetachableModel extends LoadableDetachableModel<LayerGroupInfo> {

    String id;
    LayerGroupInfo layerGroup;
    
    public LayerGroupDetachableModel(LayerGroupInfo layerGroup) {
        this.id = layerGroup.getId();
        if(id == null) {
            this.layerGroup = layerGroup;
        }
    }
    
    @Override
    protected LayerGroupInfo load() {
        if(id != null) {
            return GeoServerApplication.get().getCatalog().getLayerGroup( id );
        } else {
            return layerGroup;
        }
        
    }

}

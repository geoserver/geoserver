/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerGroupInfo;


/**
 * Simple detachable model listing all the available LayerGroup types.
 */
@SuppressWarnings({ "serial" })
public class LayerGroupTypesModel extends LoadableDetachableModel<List<LayerGroupInfo.Type>> {
    
    @Override
    protected List<LayerGroupInfo.Type> load() {
        List<LayerGroupInfo.Type> types = new ArrayList<LayerGroupInfo.Type>();
        types.add(LayerGroupInfo.Type.SINGLE);
        types.add(LayerGroupInfo.Type.NAMED);
        types.add(LayerGroupInfo.Type.CONTAINER);
        types.add(LayerGroupInfo.Type.EO);        
        return types;
    }
}
/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Provides a table model for listing layer groups
 */
@SuppressWarnings("serial")
public class LayerGroupProvider extends GeoServerDataProvider<LayerGroupInfo> {

    public static Property<LayerGroupInfo> NAME = 
        new BeanProperty<LayerGroupInfo>( "name", "name" );

    static List PROPERTIES = Arrays.asList( NAME);
    
    @Override
    protected List<LayerGroupInfo> getItems() {
        return getCatalog().getLayerGroups();
    }

    @Override
    protected List<Property<LayerGroupInfo>> getProperties() {
        return PROPERTIES;
    }

    public IModel newModel(Object object) {
        return new LayerGroupDetachableModel( (LayerGroupInfo) object );
    }

}

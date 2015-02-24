/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.List;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;

public class SecuredLayerGroupInfo extends DecoratingLayerGroupInfo {

    private LayerInfo rootLayer;
    private List<PublishedInfo> layers;

    /**
     * Overrides the layer group layer list with the one provided (which is
     * supposed to have been wrapped so that each layer can be accessed only
     * accordingly to the current user privileges)
     * 
     * @param delegate
     * @param layers
     */
    public SecuredLayerGroupInfo(LayerGroupInfo delegate, LayerInfo rootLayer, List<PublishedInfo> layers) {
        super(delegate);
        this.rootLayer = rootLayer;
        this.layers = layers;
    }

    @Override
    public LayerInfo getRootLayer() {
        return rootLayer;
    }
    
    @Override
    public List<PublishedInfo> getLayers() {
        return layers;
    }
}

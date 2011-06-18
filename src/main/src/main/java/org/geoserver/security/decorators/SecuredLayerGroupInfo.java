/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.List;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;

public class SecuredLayerGroupInfo extends DecoratingLayerGroupInfo {

    private List<LayerInfo> layers;

    /**
     * Overrides the layer group layer list with the one provided (which is
     * supposed to have been wrapped so that each layer can be accessed only
     * accordingly to the current user privileges)
     * 
     * @param delegate
     * @param layers
     */
    public SecuredLayerGroupInfo(LayerGroupInfo delegate, List<LayerInfo> layers) {
        super(delegate);
        this.layers = layers;
    }

    @Override
    public List<LayerInfo> getLayers() {
        return layers;
    }

}

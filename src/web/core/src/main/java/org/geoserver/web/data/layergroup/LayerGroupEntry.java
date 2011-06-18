/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.io.Serializable;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Represents one layer in the layer group
 */
@SuppressWarnings("serial")
public class LayerGroupEntry implements Serializable {

    String sid;
    String lid;
    
    public LayerGroupEntry( LayerInfo layer, StyleInfo style ) {
        setLayer(layer);
        setStyle(style);
    }
    
    public StyleInfo getStyle() {
        if(sid == null)
            return null;
        else
            return GeoServerApplication.get().getCatalog().getStyle( sid );
    }
    
    public boolean isDefaultStyle() {
        return sid == null;
    }
    
    public void setDefaultStyle(boolean defaultStyle) {
        if(defaultStyle) {
            setStyle(null);
        } else {
            setStyle(getLayer().getDefaultStyle());
        }
    }
    
    public void setStyle( StyleInfo style ) {
        if(style == null)
            sid = null;
        else
            sid = style.getId();
    }
    
    public LayerInfo getLayer() {
        return GeoServerApplication.get().getCatalog().getLayer( lid );
    }
    
    public void setLayer( LayerInfo layer ) {
        lid = layer.getId();
    }
    
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.io.Serializable;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Represents one layer in the layer group
 */
public class LayerGroupEntry implements Serializable {

    private static final long serialVersionUID = -2212620293553872451L;
	
    String styleId;
    String layerId;
    String layerGroupId;
    
    public LayerGroupEntry(PublishedInfo layer, StyleInfo style ) {
        setLayer(layer);
        setStyle(style);
    }
    
    public StyleInfo getStyle() {
        if(styleId == null)
            return null;
        else
            return GeoServerApplication.get().getCatalog().getStyle( styleId );
    }
    
    public boolean isDefaultStyle() {
        return styleId == null;
    }
    
    public void setDefaultStyle(boolean defaultStyle) {
        if(defaultStyle || (getLayer() instanceof LayerGroupInfo)) {
            setStyle(null);
        } else {
            setStyle(((LayerInfo) getLayer()).getDefaultStyle());
        }
    }
    
    public void setStyle( StyleInfo style ) {
        if(style == null)
            styleId = null;
        else
            styleId = style.getId();
    }
    
    public PublishedInfo getLayer() {
        if (layerGroupId != null) {
            return GeoServerApplication.get().getCatalog().getLayerGroup( layerGroupId );
        } else {
            return GeoServerApplication.get().getCatalog().getLayer( layerId );
        }
    }
    
    public void setLayer( PublishedInfo publishedInfo ) {
        if (publishedInfo instanceof LayerGroupInfo) {
            layerGroupId = publishedInfo.getId();
        } else {
            layerId = publishedInfo.getId();
        }
    }
    
}

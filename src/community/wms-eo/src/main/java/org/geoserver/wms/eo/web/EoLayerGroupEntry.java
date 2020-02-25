/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.io.Serializable;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.wms.eo.EoLayerType;

/** Represents one layer in the layer group */
@SuppressWarnings("serial")
public class EoLayerGroupEntry implements Serializable {

    String styleId;

    String layerId;

    String layerSubName;

    EoLayerType layerType;

    public EoLayerGroupEntry(LayerInfo layer, StyleInfo style, String layerGroupName) {
        setLayer(layer, layerGroupName);
        setStyle(style);
    }

    public StyleInfo getStyle() {
        if (styleId == null) return null;
        else return GeoServerApplication.get().getCatalog().getStyle(styleId);
    }

    public void setStyle(StyleInfo style) {
        if (style == null) styleId = null;
        else styleId = style.getId();
    }

    public PublishedInfo getLayer() {
        return GeoServerApplication.get().getCatalog().getLayer(layerId);
    }

    public void setLayer(LayerInfo layer, String layerGroupName) {
        layerId = layer.getId();
        MetadataMap metadata = layer.getMetadata();
        layerType = metadata.get(EoLayerType.KEY, EoLayerType.class);
        if (layerType == EoLayerType.BAND_COVERAGE) {
            layerSubName = "bands";
        } else if (layerType == EoLayerType.BROWSE_IMAGE) {
            layerSubName = "browse";
        } else {
            layerSubName = layer.getName();
            if (layerGroupName != null) {
                String prefix = layerGroupName + "_";
                if (layerSubName.startsWith(prefix)) {
                    layerSubName = layerSubName.substring(prefix.length());
                }
            }
        }
    }

    public EoLayerType getLayerType() {
        return layerType;
    }

    public String getLayerSubName() {
        return layerSubName;
    }

    @Override
    public String toString() {
        return "[LayerGroupEntry: " + getLayer().prefixedName() + ", " + getStyle().getName() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((layerId == null) ? 0 : layerId.hashCode());
        result = prime * result + ((layerSubName == null) ? 0 : layerSubName.hashCode());
        result = prime * result + ((layerType == null) ? 0 : layerType.hashCode());
        result = prime * result + ((styleId == null) ? 0 : styleId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        EoLayerGroupEntry other = (EoLayerGroupEntry) obj;
        if (layerId == null) {
            if (other.layerId != null) return false;
        } else if (!layerId.equals(other.layerId)) return false;
        if (layerSubName == null) {
            if (other.layerSubName != null) return false;
        } else if (!layerSubName.equals(other.layerSubName)) return false;
        if (layerType != other.layerType) return false;
        if (styleId == null) {
            if (other.styleId != null) return false;
        } else if (!styleId.equals(other.styleId)) return false;
        return true;
    }
}

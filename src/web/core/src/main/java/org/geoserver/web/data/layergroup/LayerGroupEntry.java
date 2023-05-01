/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.io.Serializable;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.web.GeoServerApplication;

/** Represents one layer in the layer group */
public class LayerGroupEntry implements Serializable {

    private static final long serialVersionUID = -2212620293553872451L;

    enum Type {
        LAYER("Layer"),
        LAYER_GROUP("Layer Group"),
        STYLE_GROUP("Style Group");

        String type;

        Type(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    String styleId;
    String layerGroupStyle;
    String layerId;
    String layerGroupId;

    public LayerGroupEntry(PublishedInfo layer, StyleInfo style) {
        setLayer(layer);
        setStyle(style);
    }

    public Type getType() {
        if (layerId == null && layerGroupId == null) {
            return Type.STYLE_GROUP;
        } else if (layerGroupId != null) {
            return Type.LAYER_GROUP;
        } else {
            return Type.LAYER;
        }
    }

    public StyleInfo getStyle() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        StyleInfo result = null;
        if (styleId != null) result = catalog.getStyle(styleId);
        else if (layerGroupStyle != null) {
            StyleInfo lgStyleName = new StyleInfoImpl(catalog);
            lgStyleName.setName(layerGroupStyle);
            result = lgStyleName;
        }
        return result;
    }

    public boolean isDefaultStyle() {
        return styleId == null && layerGroupStyle == null;
    }

    public void setDefaultStyle(boolean defaultStyle) {
        if (getLayer() == null) {
            setStyle(getStyle());
        } else if (defaultStyle) {
            setStyle(null);
        } else if (getLayer() instanceof LayerInfo) {
            setStyle(((LayerInfo) getLayer()).getDefaultStyle());
        } else if (getLayer() instanceof LayerGroupInfo) {
            List<LayerGroupStyle> styleList = ((LayerGroupInfo) getLayer()).getLayerGroupStyles();
            if (styleList != null && !styleList.isEmpty()) setStyle(styleList.get(0).getName());
        }
    }

    public void setStyle(StyleInfo style) {
        if (style == null) styleId = null;
        else if (layerGroupId != null) layerGroupStyle = style.getName();
        else styleId = style.getId();
    }

    public PublishedInfo getLayer() {
        if (layerGroupId != null) {
            return GeoServerApplication.get().getCatalog().getLayerGroup(layerGroupId);
        } else if (layerId != null) {
            return GeoServerApplication.get().getCatalog().getLayer(layerId);
        } else {
            return null;
        }
    }

    public void setLayer(PublishedInfo publishedInfo) {
        if (publishedInfo != null) {
            if (publishedInfo instanceof LayerGroupInfo) {
                layerGroupId = publishedInfo.getId();
            } else {
                layerId = publishedInfo.getId();
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((layerGroupId == null) ? 0 : layerGroupId.hashCode());
        result = prime * result + ((layerId == null) ? 0 : layerId.hashCode());
        result = prime * result + ((styleId == null) ? 0 : styleId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        LayerGroupEntry other = (LayerGroupEntry) obj;
        if (layerGroupId == null) {
            if (other.layerGroupId != null) return false;
        } else if (!layerGroupId.equals(other.layerGroupId)) return false;
        if (layerId == null) {
            if (other.layerId != null) return false;
        } else if (!layerId.equals(other.layerId)) return false;
        if (styleId == null) {
            if (other.styleId != null) return false;
        } else if (!styleId.equals(other.styleId)) return false;
        return true;
    }
}

/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.util.logging.Logging;

public class LayerInfoImpl implements LayerInfo {
    
    static final Logger LOGGER = Logging.getLogger(LayerInfoImpl.class);
    
    static final String KEY_ADVERTISED = "advertised"; 

    protected String id;

    // this property has been left to ensure backwards compatibility with xstream but it's marked transient
    // to avoid its value being serialized.
    // TODO: revert to normal property when the resource/publishing split is done
    transient protected String name;

    protected String path;

    protected LayerInfo.Type type;

    protected StyleInfo defaultStyle;

    protected Set styles = new HashSet();

    protected ResourceInfo resource;

    protected LegendInfo legend;

    protected boolean enabled;

    protected Boolean queryable;

    protected MetadataMap metadata = new MetadataMap();

    protected AttributionInfo attribution;
    
    
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        if (resource == null) {
            throw new NullPointerException("Unable to get Layer name without an underlying resource");
        }
        return resource.getName();
        // TODO: uncomment back when resource/publish split is complete
        // return name;
    }

    public void setName(String name) {
        // TODO: remove this log and reinstate field assignment when resource/publish split is complete
        LOGGER.log(Level.FINE, "Warning, some code is setting the LayerInfo name, but that will be ignored");
        this.name = name;
        
        if (resource == null) {
            throw new NullPointerException("Layer name must not be set without an underlying resource");
        }
        resource.setName(name);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public StyleInfo getDefaultStyle() {
        return defaultStyle;
    }

    public void setDefaultStyle(StyleInfo defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    public Set getStyles() {
        return styles;
    }

    public void setStyles(Set styles) {
        this.styles = styles;
    }

    public ResourceInfo getResource() {
        return resource;
    }

    public void setResource(ResourceInfo resource) {
        this.resource = resource;
    }

    public LegendInfo getLegend() {
        return legend;
    }

    public void setLegend(LegendInfo legend) {
        this.legend = legend;
    }

    public AttributionInfo getAttribution() {
        return attribution;
    }

    public void setAttribution(AttributionInfo attribution) {
        this.attribution = attribution;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @see LayerInfo#enabled()
     */
    public boolean enabled() {
        ResourceInfo resource = getResource();
        boolean resourceEnabled = resource != null && resource.enabled();
        boolean thisEnabled = this.isEnabled();
        return resourceEnabled && thisEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        ResourceInfo resource = getResource();
        if (resource != null) {
            resource.setEnabled(enabled);
        }
    }

    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }
    
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((defaultStyle == null) ? 0 : defaultStyle.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((legend == null) ? 0 : legend.hashCode());
        // TODO: add back when resource publish split is in place
        // result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result
                + ((resource == null) ? 0 : resource.hashCode());
        result = prime * result + ((styles == null) ? 0 : styles.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((attribution == null) ? 0 : attribution.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof LayerInfo))
            return false;
        final LayerInfo other = (LayerInfo) obj;
        if (defaultStyle == null) {
            if (other.getDefaultStyle() != null)
                return false;
        } else if (!defaultStyle.equals(other.getDefaultStyle()))
            return false;
        if (enabled != other.isEnabled())
            return false;
        if (id == null) {
            if (other.getId() != null)
                return false;
        } else if (!id.equals(other.getId()))
            return false;
        if (legend == null) {
            if (other.getLegend() != null)
                return false;
        } else if (!legend.equals(other.getLegend()))
            return false;
        // TODO: add back when resource/publish split is in place
//        if (name == null) {
//            if (other.getName() != null)
//                return false;
//        } else if (!name.equals(other.getName()))
//            return false;
        if (path == null) {
            if (other.getPath() != null)
                return false;
        } else if (!path.equals(other.getPath()))
            return false;
        if (resource == null) {
            if (other.getResource() != null)
                return false;
        } else if (!resource.equals(other.getResource()))
            return false;
        if (styles == null) {
            if (other.getStyles() != null)
                return false;
        } else if (!styles.equals(other.getStyles()))
            return false;
        if (type == null) {
            if (other.getType() != null)
                return false;
        } else if (!type.equals(other.getType()))
            return false;
        if (attribution == null) {
            if (other.getAttribution() != null)
                return false;
        } else if (!attribution.equals(other.getAttribution()))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[').append(getName()).append(
                ", resource:").append(resource).append(']').toString();
    }

    public void setQueryable(boolean queryable) {
        this.queryable = queryable;
    }

    public boolean isQueryable() {
        return this.queryable == null? true : this.queryable.booleanValue();
    }

    public boolean isAdvertised() {
        // new property, it's in the metadata on 2.1.x series (there is a filed in
        MetadataMap md = getMetadata();
        if(md == null) {
            return true;
        }
        Boolean metadataAdvertised = md.get(KEY_ADVERTISED, Boolean.class);
        if(metadataAdvertised == null) {
            metadataAdvertised = true;
        }
        return metadataAdvertised;
    }

    public void setAdvertised(boolean advertised) {
        if(metadata == null) {
            metadata = new MetadataMap();
        }
        metadata.put(KEY_ADVERTISED, advertised);
        if(resource != null) {
            resource.setAdvertised(advertised);
        }
    }
}

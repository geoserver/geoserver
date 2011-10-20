/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StyleInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class LayerGroupInfoImpl implements LayerGroupInfo {

    protected String id;
    protected String name;
    protected String path;
    protected List<LayerInfo> layers = new ArrayList<LayerInfo>();
    protected List<StyleInfo> styles = new ArrayList<StyleInfo>();
    protected ReferencedEnvelope bounds;
    protected MetadataMap metadata = new MetadataMap();

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "authorityURLs", and a not transient in the 2.2.x series.
     * 
     * @since 2.1.3
     */
    protected transient List<AuthorityURLInfo> authorityURLs = new ArrayList<AuthorityURLInfo>(2);

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "identifiers", and a not transient in the 2.2.x series.
     * 
     * @since 2.1.3
     */
    protected transient List<LayerIdentifierInfo> identifiers = new ArrayList<LayerIdentifierInfo>(2);
    
    public LayerGroupInfoImpl() {
    }
    
    public String getId() {
        return id;
    }
    
    public void setId( String id ) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    public List<LayerInfo> getLayers() {
        return layers;
    }

    public void setLayers(List<LayerInfo> layers) {
        this.layers = layers;
    }
    
    public List<StyleInfo> getStyles() {
        return styles;
    }
    
    public void setStyles(List<StyleInfo> styles) {
        this.styles = styles;
    }
    
    public ReferencedEnvelope getBounds() {
        return bounds;
    }
    
    public void setBounds(ReferencedEnvelope bounds) {
        this.bounds = bounds;
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
        result = prime * result + ((bounds == null) ? 0 : bounds.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((layers == null) ? 0 : layers.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((styles == null) ? 0 : styles.hashCode());
        result = prime * result + ((authorityURLs == null) ? 0 : authorityURLs.hashCode());
        result = prime * result + ((identifiers == null) ? 0 : identifiers.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!( obj instanceof LayerGroupInfo) ) 
            return false;
        LayerGroupInfo other = (LayerGroupInfo) obj;
        if (bounds == null) {
            if (other.getBounds() != null)
                return false;
        } else if (!bounds.equals(other.getBounds()))
            return false;
        if (id == null) {
            if (other.getId() != null)
                return false;
        } else if (!id.equals(other.getId()))
            return false;
        if (layers == null) {
            if (other.getLayers() != null)
                return false;
        } else if (!layers.equals(other.getLayers()))
            return false;
        if (metadata == null) {
            if (other.getMetadata() != null)
                return false;
        } else if (!metadata.equals(other.getMetadata()))
            return false;
        if (name == null) {
            if (other.getName() != null)
                return false;
        } else if (!name.equals(other.getName()))
            return false;
        if (styles == null) {
            if (other.getStyles() != null)
                return false;
        } else if (!styles.equals(other.getStyles()))
            return false;
        if(authorityURLs == null){
            if (other.getAuthorityURLs() != null)
                return false;
        } else if (!authorityURLs.equals(other.getAuthorityURLs()))
            return false;
        
        if(identifiers == null){
            if (other.getIdentifiers() != null)
                return false;
        } else if (!identifiers.equals(other.getIdentifiers()))
            return false;
        
        return true;
    }
    

    // @Override
    public List<AuthorityURLInfo> getAuthorityURLs() {
        return authorityURLs;
    }

    public void setAuthorityURLs(List<AuthorityURLInfo> authorities){
        this.authorityURLs = authorities;
    }
    
    // @Override
    public List<LayerIdentifierInfo> getIdentifiers() {
        return identifiers;
    }
    
    public void setIdentifiers(List<LayerIdentifierInfo> identifiers){
        this.identifiers = identifiers;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[').append(name).append(']')
                .toString();
    }
}

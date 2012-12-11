/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.List;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AbstractDecorator;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Delegates every method to the wrapped {@link LayerGroupInfo}. Subclasses will
 * override selected methods to perform their "decoration" job
 * 
 * @author Andrea Aime
 */
public class DecoratingLayerGroupInfo extends AbstractDecorator<LayerGroupInfo> implements LayerGroupInfo {

    public DecoratingLayerGroupInfo(LayerGroupInfo delegate) {
        super(delegate);
    }

    public ReferencedEnvelope getBounds() {
        return delegate.getBounds();
    }

    public String getId() {
        return delegate.getId();
    }

    public List<LayerInfo> getLayers() {
        return delegate.getLayers();
    }

    public String getName() {
        return delegate.getName();
    }

    public WorkspaceInfo getWorkspace() {
        return delegate.getWorkspace();
    }

    public String prefixedName() {
        return delegate.prefixedName();
    }

    public List<StyleInfo> getStyles() {
        return delegate.getStyles();
    }

    public void setBounds(ReferencedEnvelope bounds) {
        delegate.setBounds(bounds);
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public void setWorkspace(WorkspaceInfo workspace) {
        delegate.setWorkspace(workspace);
    }

    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }
    
    public String getTitle() {
        return delegate.getTitle();
    }
    
    public void setTitle(String title) {
        delegate.setTitle(title);
    }
    
    public String getAbstract() {
        return delegate.getAbstract();
    }
    
    public void setAbstract(String abstractTxt) {
        delegate.setAbstract(abstractTxt);
    }    
    
    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }
    
    @Override
    public List<AuthorityURLInfo> getAuthorityURLs() {
        return delegate.getAuthorityURLs();
    }

    @Override
    public List<LayerIdentifierInfo> getIdentifiers() {
        return delegate.getIdentifiers();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[').append(delegate).append(
                ']').toString();
    }

}

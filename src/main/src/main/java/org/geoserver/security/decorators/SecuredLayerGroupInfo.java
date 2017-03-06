/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.AbstractList;
import java.util.List;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.AbstractDecorator;

public class SecuredLayerGroupInfo extends DecoratingLayerGroupInfo {

    private LayerInfo rootLayer;
    private List<PublishedInfo> layers;
    private List<StyleInfo> styles;

    /**
     * Overrides the layer group layer list with the one provided (which is
     * supposed to have been wrapped so that each layer can be accessed only
     * accordingly to the current user privileges)
     * 
     * @param delegate
     * @param layers
     */
    public SecuredLayerGroupInfo(LayerGroupInfo delegate, LayerInfo rootLayer, List<PublishedInfo> layers, List<StyleInfo> styles) {
        super(delegate);
        this.rootLayer = rootLayer;
        this.layers = layers;
        this.styles = styles;
    }

    @Override
    public LayerInfo getRootLayer() {
        return rootLayer;
    }
    
    @Override 
    public void setRootLayer(LayerInfo rootLayer) {
        //keep synchronised
        this.rootLayer = rootLayer;
        delegate.setRootLayer((LayerInfo) unwrap(rootLayer));
    }
    
    @Override
    public List<PublishedInfo> getLayers() {        
        // keep synchronised
        return new AbstractList<PublishedInfo>() {
            @Override
            public PublishedInfo get(int index) {
                return layers.get(index);
            }

            @Override
            public int size() {
                return layers.size();
            }

            @Override
            public void add(int index, PublishedInfo element) {
                delegate.getLayers().add(index, unwrap(element));
                layers.add(index, element);
            }

            public PublishedInfo set(int index, PublishedInfo element) {
                delegate.getLayers().set(index, unwrap(element));
                return layers.set(index, element);                
            }

            public PublishedInfo remove(int index) {
                delegate.getLayers().remove(index);
                return layers.remove(index);
            }
            
            @Override
            public boolean remove(Object o) {
                delegate.getLayers().remove(o);
                return layers.remove(o);
            }

        };
    }
    
    @Override
    public List<StyleInfo> getStyles() {
        // keep synchronised
        return new AbstractList<StyleInfo>() {
            @Override
            public StyleInfo get(int index) {
                return styles.get(index);
            }

            @Override
            public int size() {
                return styles.size();
            }

            @Override
            public void add(int index, StyleInfo element) {
                delegate.getStyles().add(index, element);
                styles.add(index, element);
            }

            public StyleInfo set(int index, StyleInfo element) {
                delegate.getStyles().set(index, element);
                return styles.set(index, element);
            }

            public StyleInfo remove(int index) {
                delegate.getStyles().remove(index);
                return styles.remove(index);
            }

            @Override
            public boolean remove(Object o) {
                delegate.getStyles().remove(o);
                return styles.remove(o);
            }
        };
    }
    
    private static PublishedInfo unwrap(PublishedInfo pi) {
        if (pi instanceof SecuredLayerInfo || pi instanceof SecuredLayerGroupInfo) {
            @SuppressWarnings("unchecked")
            AbstractDecorator<? extends PublishedInfo> decorator = (AbstractDecorator<? extends PublishedInfo>) pi;

            return decorator.unwrap(PublishedInfo.class);
        } else {
            return pi;
        }
    }

}

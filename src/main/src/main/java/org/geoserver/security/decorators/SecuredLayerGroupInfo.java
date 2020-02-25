/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.FilteredList;
import org.geotools.util.decorate.AbstractDecorator;

public class SecuredLayerGroupInfo extends DecoratingLayerGroupInfo {

    private LayerInfo rootLayer;
    private List<PublishedInfo> layers;
    private List<StyleInfo> styles;

    /**
     * Overrides the layer group layer list with the one provided (which is supposed to have been
     * wrapped so that each layer can be accessed only accordingly to the current user privileges)
     */
    public SecuredLayerGroupInfo(
            LayerGroupInfo delegate,
            LayerInfo rootLayer,
            List<PublishedInfo> layers,
            List<StyleInfo> styles) {
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
        // keep synchronised
        this.rootLayer = rootLayer;
        delegate.setRootLayer((LayerInfo) unwrap(rootLayer));
    }

    @Override
    public List<PublishedInfo> getLayers() {
        return new FilteredList<PublishedInfo>(layers, delegate.getLayers()) {
            @Override
            protected PublishedInfo unwrap(PublishedInfo element) {
                return SecuredLayerGroupInfo.unwrap(element);
            }
        };
    }

    @Override
    public List<StyleInfo> getStyles() {
        return new FilteredList<StyleInfo>(styles, delegate.getStyles());
    }

    private static PublishedInfo unwrap(PublishedInfo pi) {
        if (pi instanceof SecuredLayerInfo || pi instanceof SecuredLayerGroupInfo) {
            @SuppressWarnings("unchecked")
            AbstractDecorator<? extends PublishedInfo> decorator =
                    (AbstractDecorator<? extends PublishedInfo>) pi;

            return decorator.unwrap(PublishedInfo.class);
        } else {
            return pi;
        }
    }
}

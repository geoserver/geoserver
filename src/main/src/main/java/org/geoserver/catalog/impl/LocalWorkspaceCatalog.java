/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.LocalWorkspaceCatalogFilter;

/**
 * Catalog decorator handling cases when a {@link LocalWorkspace} is set.
 * <p>
 * This wrapper handles some additional cases that {@link LocalWorkspaceCatalogFilter} can not 
 * handle by simple filtering.
 * </p> 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class LocalWorkspaceCatalog extends AbstractCatalogDecorator implements Catalog {

    public LocalWorkspaceCatalog(Catalog delegate) {
        super(delegate);
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        if (LocalWorkspace.get() != null) {
            StyleInfo style = super.getStyleByName(LocalWorkspace.get(), name);
            if (style != null) {
                return style;
            }
        }
        return super.getStyleByName(name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        if (LocalWorkspace.get() != null) {
            LayerGroupInfo layerGroup = super.getLayerGroupByName(LocalWorkspace.get(), name);
            if (layerGroup != null) {
                return layerGroup;
            }
        }
        return super.getLayerGroupByName(name);
    }

}

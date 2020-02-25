/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Dispatcher;
import org.opengis.filter.Filter;

/**
 * A pluggable extension point that can filter out catalog items you don't want the user to see, so
 * that upper levels in the code believe the item is not there.
 *
 * <p>Instances of this class have to be registered as beans in the Spring context in order to be
 * picked up
 *
 * <p>The {@link Dispatcher#REQUEST} and Spring current user can be used to gather context about the
 * current request
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface CatalogFilter {
    /** Return true to hide the specified layer from the catalog */
    boolean hideLayer(LayerInfo layer);

    /**
     * Return true to hide the specified style from the catalog
     *
     * @param style The style to potentially hide.
     */
    boolean hideStyle(StyleInfo style);

    /**
     * Return true to hide the specified layer group from the catalog
     *
     * @param layerGroup The layer group to potentially hide.
     */
    boolean hideLayerGroup(LayerGroupInfo layerGroup);

    /**
     * Return true to hide the specified workspace from the catalog
     *
     * @param workspace Workspace to hide
     */
    boolean hideWorkspace(WorkspaceInfo workspace);

    /**
     * Return true to hide the specified resource from the catalog
     *
     * @param resource Resource (layer) to hide
     */
    boolean hideResource(ResourceInfo resource);

    /**
     * Returns a Filter equivalent to this CatalogFilter when applied to an object of the specified
     * type.
     *
     * @param clazz CatalogInfo type to check against security filters
     */
    Filter getSecurityFilter(final Class<? extends CatalogInfo> clazz);
}

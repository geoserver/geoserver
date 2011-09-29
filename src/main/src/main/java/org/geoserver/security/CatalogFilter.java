/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Dispatcher;

/**
 * A pluggable extension point that can filter out catalog items you don't want the user to see, so
 * that upper levels in the code believe the item is not there.
 * <p>
 * Instances of this class have to be registered as beans in the Spring context in order to be
 * picked up
 * <p>
 * The {@link Dispatcher#REQUEST} and Spring current user can be used to gather context about the
 * current request
 * </p>
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public interface CatalogFilter {
    /**
     * Return true to hide the specified layer from the catalog
     * 
     * @param layer
     * @return
     */
    boolean hideLayer(LayerInfo layer);

    /**
     * Return true to hide the specified workspace from the catalog
     * 
     * @param layer
     * @return
     */
    boolean hideWorkspace(WorkspaceInfo workspace);

    /**
     * Return true to hide the specified resource from the catalog
     * 
     * @param layer
     * @return
     */
    boolean hideResource(ResourceInfo resource);
}

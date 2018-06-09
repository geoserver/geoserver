/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.List;

/**
 * A grouping of layers.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface MapInfo extends CatalogInfo {

    /** The name of the map. */
    String getName();

    /** Sets the name of the map. */
    void setName(String name);

    /** Flag indicating if the map is enabled. */
    boolean isEnabled();

    /** Sets flag indicating if the map is enabled. */
    void setEnabled(boolean enabled);

    /** The layers that compose the map. */
    List<LayerInfo> getLayers();
}

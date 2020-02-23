/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.geoserver.GeoServerNodeData;

/**
 * Provides identification and styling for the GeoServer id element showing right below the
 * GeoServer logo in the GeoServer GUI. A system variable based implementation is provided by
 * default, just register another one in the web-app context in order to override it
 *
 * @author Andrea Aime - GeoSolutions
 */
interface GeoServerNodeInfo {

    /**
     * The node id, displayed as a label in the GUI. If null is returned, the element will be
     * hidden.
     */
    default String getId() {
        return getData().getId();
    }

    /** Returns the data object containing the node info. */
    default GeoServerNodeData getData() {
        return GeoServerNodeData.createFromEnvironment();
    }

    /**
     * Allows customization of the label container, in particular, its style and visibility, but
     * also any other attribute. Implementors are suggested to hide the id element unless an
     * administrator is logged in, but specific implementations can use different policies. See
     * {@link DefaultGeoServerNodeInfo} for the default implementation of the visibility logic
     */
    void customize(WebMarkupContainer nodeInfoContainer);
}

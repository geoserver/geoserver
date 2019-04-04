/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Map;

/** Callback that can change the contents of the baseURL, the path or the KVP map. */
public interface URLMangler {
    public enum URLType {
        /** The link points outside Geoserver * */
        EXTERNAL,
        /** The link points to a static resource (image, ogc schema, etc. * */
        RESOURCE,
        /** The link points to a dynamic service provided by Geoserver (WFS, WMS, WCS, etc.) */
        SERVICE
    };

    /**
     * Callback that can change the contents of the baseURL, the path or the KVP map
     *
     * @param baseURL the base URL, containing host, port and application
     * @param path after the application name
     * @param kvp the GET request parameters
     * @param type URL type (External, resource or service) for consideration during mangling
     */
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type);
}

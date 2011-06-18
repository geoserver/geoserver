/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Map;

public interface URLMangler {
    public enum URLType {
        /** The link points outside Geoserver **/
        EXTERNAL,
        /** The link points to a static resource (image, ogc schema, etc. **/
        RESOURCE,
        /**
         * The link points to a dynamic service provided by Geoserver (WFS, WMS, WCS, etc.)
         **/
        SERVICE
    };

    /**
     * Callback that can change the contents of the baseURL, the path or the KVP map
     * @param baseURL the base URL, containing host, port and application
     * @param path after the application name
     * @param kvp the GET request parameters
     * @param the URL type
     */
    public void mangleURL(StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type);

}

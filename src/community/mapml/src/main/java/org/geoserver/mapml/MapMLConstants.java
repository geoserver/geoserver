/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.text.SimpleDateFormat;

public final class MapMLConstants {

    /** format mime type */
    public static final String MIME_TYPE = "text/mapml";

    /** format name */
    public static final String FORMAT_NAME = "MAPML";

    public static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
}

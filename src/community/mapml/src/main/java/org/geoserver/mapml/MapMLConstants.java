/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import org.springframework.http.MediaType;

public final class MapMLConstants {

    /** format mime type */
    public static final String MAPML_MIME_TYPE = "text/mapml";

    /** format MediaType */
    public static final MediaType MAPML_MEDIA_TYPE =
            new MediaType("text", "mapml", Charset.forName("UTF-8"));

    /** format name */
    public static final String FORMAT_NAME = "MAPML";

    public static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
}

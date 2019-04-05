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

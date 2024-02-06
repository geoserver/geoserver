/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;

/**
 * @author Chris Hodgson
 * @author prushforth
 */
public final class MapMLConstants {

    /** format mime type */
    public static final String MAPML_MIME_TYPE = "text/mapml";

    /** HTML format mime type */
    public static final String MAPML_HTML_MIME_TYPE = "text/html; subtype=mapml";

    /** WMS format_options parameter */
    public static final String MAPML_WMS_MIME_TYPE_OPTION = "mapml-wms-format";

    /** format MediaType */
    public static final MediaType MAPML_MEDIA_TYPE =
            new MediaType("text", "mapml", StandardCharsets.UTF_8);

    /** format name */
    public static final String FORMAT_NAME = "MAPML";

    /** MapML format options */
    public static final String MAPML_FEATURE_FORMAT_OPTIONS = "mapmlfeatures:true";

    /** MapML layer metadata use features */
    public static final String MAPML_USE_FEATURES = "mapml.useFeatures";

    /** MapML layer metadata use tiles */
    public static final String MAPML_USE_TILES = "mapml.useTiles";

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String REL_ZOOMIN = "zoomin";
    public static final String REL_ZOOMOUT = "zoomout";
    public static final String REL_NEXT = "next";
    public static final String REL_LICENSE = "license";

    public static final List<String> ZOOM_RELS = Arrays.asList(REL_ZOOMIN, REL_ZOOMOUT);

    public static int PAGESIZE = 100;
}

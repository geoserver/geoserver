/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.geoserver.mapml.tcrs.Bounds;
import org.geoserver.mapml.tcrs.Point;
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

    /** format name needed to have a parseable format in WFS 1.0.0 capabilities */
    public static final String HTML_FORMAT_NAME = "MAPML-HTML";

    /** MapML format option enabling features */
    public static final String MAPML_FEATURE_FO = "mapmlfeatures";

    /** MapML format option enabling attribute skipping */
    public static final String MAPML_SKIP_ATTRIBUTES_FO = "mapmlskipattributes";

    public static final String MAPML_SKIP_STYLES_FO = "mapmlskipheadstyles";

    /** MapML layer metadata use features */
    public static final String MAPML_USE_FEATURES = "mapml.useFeatures";

    /** MapML layer metadata use tiles */
    public static final String MAPML_USE_TILES = "mapml.useTiles";

    /** MapML layer resource metadata */
    public static final String RESOURCE_METADATA = "resource.metadata";

    /** LIST FAILED */
    public static final String LIST_FAILED = "Grabbing the attribute list failed";

    /** ATTRIBUTE_LIST_FAILED */
    public static final String ATTRIBUTE_LIST_FAILED = "attributeListingFailed";

    /** FEATURE_CAPTION_TEMPLATE */
    public static final String FEATURE_CAPTION_TEMPLATE = "featureCaptionTemplate";

    /** FEATURE_CAPTION */
    public static final String FEATURE_CAPTION = "mapml.featureCaption";

    /** FEATURE_CAPTION_ATTRIBUTES */
    public static final String FEATURE_CAPTION_ATTRIBUTES = "featurecaptionattributes";

    /** MAPML_PREFIX */
    public static final String MAPML_PREFIX = "mapml.";

    /** DIMENSION */
    public static final String DIMENSION = "dimension";

    /** DEFAULT MIME TYPE */
    public static final String MIME = "mime";

    /** MAPML_DIMENSION */
    public static final String MAPML_DIMENSION = MAPML_PREFIX + DIMENSION;

    /** MAPML_DIMENSION */
    public static final String MAPML_MIME = MAPML_PREFIX + MIME;

    /** SHARD_LIST */
    public static final String SHARD_LIST = "shardList";

    /** ENABLE_SHARDING */
    public static final String ENABLE_SHARDING = "enableSharding";

    /** USE_TILES */
    public static final String USE_TILES = "useTiles";

    /** LICENSE_LINK */
    public static final String LICENSE = "licenseLink";

    /** LICENSE_TITLE2 */
    public static final String LICENSE_TITLE2 = "licenseTitle";

    /** USE_FEATURES */
    public static final String USE_FEATURES = "useFeatures";

    /** SHARD_SERVER_PATTERN */
    public static final String SHARD_SERVER_PATTERN = "shardServerPattern";

    /** LICENSE_TITLE */
    public static final String LICENSE_TITLE = "mapml.licenseTitle";

    /** LICENSE_LINK */
    public static final String LICENSE_LINK = "mapml.licenseLink";

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String REL_ZOOMIN = "zoomin";
    public static final String REL_ZOOMOUT = "zoomout";
    public static final String REL_NEXT = "next";
    public static final String REL_LICENSE = "license";

    public static final List<String> ZOOM_RELS = Arrays.asList(REL_ZOOMIN, REL_ZOOMOUT);
    public static final Bounds DISPLAY_BOUNDS_DESKTOP_LANDSCAPE =
            new Bounds(new Point(0, 0), new Point(768, 1024));

    public static int PAGESIZE = 100;
}

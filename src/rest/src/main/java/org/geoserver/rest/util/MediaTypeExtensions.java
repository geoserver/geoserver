/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.util;

import org.springframework.http.MediaType;

/**
 * Container class for {@link org.springframework.http.MediaType} definitions that are not an
 * official MIME type, but nevertheless supported by GeoServer.
 */
public class MediaTypeExtensions {
    public static final String TEXT_JSON_VALUE = "text/json";
    public static final MediaType TEXT_JSON = MediaType.valueOf(TEXT_JSON_VALUE);

    public static final String APPLICATION_ZIP_VALUE = "application/zip";
    public static final MediaType APPLICATION_ZIP = MediaType.valueOf(APPLICATION_ZIP_VALUE);

    public static final String FTL_EXTENSION = "ftl";
    public static final String TEXT_FTL_VALUE = "text/plain";
    public static final MediaType TEXT_FTL = MediaType.valueOf(TEXT_FTL_VALUE);

    public static final String APPLICATION_XSLT_VALUE = "application/xslt+xml";
    public static final MediaType APPLICATION_XSLT = MediaType.valueOf(APPLICATION_XSLT_VALUE);
}

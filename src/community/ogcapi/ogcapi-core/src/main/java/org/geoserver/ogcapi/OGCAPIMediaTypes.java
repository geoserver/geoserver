/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import org.springframework.http.MediaType;

/** Collects common media type definitions for the OGC APIs */
public class OGCAPIMediaTypes {

    /** GeoJSON media type */
    public static final String GEOJSON_VALUE = "application/geo+json";
    /** GeoJSON media type */
    public static MediaType GEOJSON = MediaType.parseMediaType(GEOJSON_VALUE);

    /** Not meant to be instantiated */
    private OGCAPIMediaTypes() {}
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.springframework.http.MediaType;

/**
 * Support object to encode HTML maps (needed to respect Spring class oriented wiring. The object is
 * encoded by {@link HTMLMapMessageConverter}
 */
public class HTMLMap extends WebMap {
    /** @param context the map context, can be {@code null} is there's _really_ no context around */
    public HTMLMap(WMSMapContent context) {
        super(context);
        setMimeType(MediaType.TEXT_HTML_VALUE);
    }
}

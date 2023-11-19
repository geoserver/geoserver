/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

/** A WebMap containing a MapML HTML document */
public class MapMLHTMLMap extends WebMap {
    String mapmlHTML;

    /** @param context the map context, can be {@code null} is there's _really_ no context around */
    public MapMLHTMLMap(WMSMapContent context, String mapmlHTML) {
        super(context);
        this.mapmlHTML = mapmlHTML;
        super.setMimeType(MapMLConstants.MAPML_HTML_MIME_TYPE);
    }

    /**
     * Get the MapML HTML document
     *
     * @return the MapML HTML document
     */
    public String getMapmlHTML() {
        return mapmlHTML;
    }
}

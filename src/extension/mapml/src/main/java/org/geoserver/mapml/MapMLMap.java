/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import org.geoserver.mapml.xml.Mapml;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

public class MapMLMap extends WebMap {
    Mapml mapml;

    /** @param context the map context, can be {@code null} is there's _really_ no context around */
    public MapMLMap(WMSMapContent context, Mapml mapml) {
        super(context);
        this.mapml = mapml;
        super.setMimeType(MapMLConstants.MAPML_MIME_TYPE);
    }

    /**
     * Get the MapML document
     *
     * @return the MapML document
     */
    public Mapml getMapml() {
        return mapml;
    }
}

/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import org.geoserver.config.GeoServer;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;
import org.geoserver.wms.WMSInfo;

public class MapsServiceDescriptionProvider
        extends OgcApiServiceDescriptionProvider<WMSInfo, MapsService> {

    public MapsServiceDescriptionProvider(GeoServer gs) {
        super(gs, "WMS", "Maps", "Maps");
    }
}

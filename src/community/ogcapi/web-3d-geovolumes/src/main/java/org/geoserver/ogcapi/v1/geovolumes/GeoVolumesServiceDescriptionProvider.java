/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import org.geoserver.config.GeoServer;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;

public class GeoVolumesServiceDescriptionProvider
        extends OgcApiServiceDescriptionProvider<GeoVolumesServiceInfo, GeoVolumesService> {

    public GeoVolumesServiceDescriptionProvider(GeoServer gs) {
        super(gs, "3D-GeoVolumes", "3D-GeoVolumes", "3D-GeoVolumes");
    }
}

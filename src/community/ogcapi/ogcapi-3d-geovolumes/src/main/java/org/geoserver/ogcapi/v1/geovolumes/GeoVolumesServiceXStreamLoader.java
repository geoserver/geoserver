/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class GeoVolumesServiceXStreamLoader extends XStreamServiceLoader<GeoVolumesServiceInfo> {

    public GeoVolumesServiceXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "3dgeovolumes");
    }

    @Override
    protected GeoVolumesServiceInfo createServiceFromScratch(GeoServer gs) {
        GeoVolumesServiceInfo info = new GeoVolumesServiceInfoImpl();
        info.setName("3dgeovolumes");
        if (info.getTitle() == null) {
            info.setTitle("3D GeoVolumes Service");
            info.setAbstract(
                    "OGCAPI 3D GeoVolumes organizes access to a variety of 2D / 3D content according to a hierarchy of 3D geospatial volumes (GeoVolumes).");
        }
        return info;
    }

    @Override
    public Class<GeoVolumesServiceInfo> getServiceClass() {
        return GeoVolumesServiceInfo.class;
    }
}

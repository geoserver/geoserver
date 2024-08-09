/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import org.geoserver.config.ServiceFactoryExtension;
import org.springframework.stereotype.Component;

@Component
public class GeoVolumesFactoryExtension extends ServiceFactoryExtension<GeoVolumesServiceInfo> {

    protected GeoVolumesFactoryExtension() {
        super(GeoVolumesServiceInfo.class);
    }

    @Override
    public <T> T create(Class<T> clazz) {
        return clazz.cast(new GeoVolumesServiceInfoImpl());
    }
}

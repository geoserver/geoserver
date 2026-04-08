/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import org.geoserver.config.impl.ServiceInfoImpl;

public class GeoVolumesServiceInfoImpl extends ServiceInfoImpl implements GeoVolumesServiceInfo {
    @Override
    public String getType() {
        return "3D-GeoVolumes";
    }
}

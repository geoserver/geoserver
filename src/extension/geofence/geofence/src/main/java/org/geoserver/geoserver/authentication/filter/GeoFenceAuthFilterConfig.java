/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoserver.authentication.filter;

import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.config.SecurityFilterConfig;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceAuthFilterConfig extends SecurityFilterConfig
        implements SecurityAuthFilterConfig {

    // just testing...
    private String geofenceUrl;

    // just testing...
    private String geoserverName;

    public String getGeofenceUrl() {
        return geofenceUrl;
    }

    public void setGeofenceUrl(String geofenceUrl) {
        this.geofenceUrl = geofenceUrl;
    }

    public String getGeoserverName() {
        return geoserverName;
    }

    public void setGeoserverName(String geoserverName) {
        this.geoserverName = geoserverName;
    }
}

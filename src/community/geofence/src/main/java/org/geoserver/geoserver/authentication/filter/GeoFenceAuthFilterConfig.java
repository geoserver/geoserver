/*
 *  Copyright (C) 2007 - 2013 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

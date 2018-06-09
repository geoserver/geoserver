/*
 *  Copyright (C) 2014 GeoSolutions S.A.S.
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

package org.geoserver.geofence.web.authentication;

import org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProvider;
import org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProviderConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanelInfo;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceAuthProviderPanelInfo
        extends AuthenticationProviderPanelInfo<
                GeoFenceAuthenticationProviderConfig, GeoFenceAuthProviderPanel> {

    private static final long serialVersionUID = 8491501364970390005L;

    public GeoFenceAuthProviderPanelInfo() {
        setComponentClass(GeoFenceAuthProviderPanel.class);
        setServiceClass(GeoFenceAuthenticationProvider.class);
        setServiceConfigClass(GeoFenceAuthenticationProviderConfig.class);
    }
}

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

package org.geoserver.geoserver.authentication.auth;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class GeoFenceAuthenticationProviderConfig extends BaseSecurityNamedServiceConfig
        implements SecurityAuthProviderConfig {

    public GeoFenceAuthenticationProviderConfig() {
    }

    public GeoFenceAuthenticationProviderConfig(GeoFenceAuthenticationProviderConfig other) {
        super(other);
    }

    @Override
    public String getUserGroupServiceName() {
        return null;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {
    }

}

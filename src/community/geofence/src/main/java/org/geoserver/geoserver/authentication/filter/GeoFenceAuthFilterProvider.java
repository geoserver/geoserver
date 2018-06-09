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

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geoserver.authentication.auth.GeoFenceSecurityProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceAuthFilterProvider extends AbstractFilterProvider {

    private RuleReaderService ruleReaderService;

    private GeoFenceSecurityProvider geofenceAuth;

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("geofenceFilter", GeoFenceAuthFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoFenceAuthFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        GeoFenceAuthFilter filter = new GeoFenceAuthFilter();

        filter.setRuleReaderService(ruleReaderService);
        filter.setGeofenceAuth(geofenceAuth);

        return filter;
    }

    public void setRuleReaderService(RuleReaderService ruleReaderService) {
        this.ruleReaderService = ruleReaderService;
    }

    public void setGeofenceAuth(GeoFenceSecurityProvider geofenceAuth) {
        this.geofenceAuth = geofenceAuth;
    }
}

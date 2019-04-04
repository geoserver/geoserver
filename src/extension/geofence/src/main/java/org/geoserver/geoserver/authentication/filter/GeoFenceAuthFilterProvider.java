/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoserver.authentication.filter;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.geoserver.authentication.auth.GeoFenceSecurityProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceAuthFilterProvider extends AbstractFilterProvider {

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

        filter.setGeofenceAuth(geofenceAuth);

        return filter;
    }

    public void setGeofenceAuth(GeoFenceSecurityProvider geofenceAuth) {
        this.geofenceAuth = geofenceAuth;
    }
}

/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
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

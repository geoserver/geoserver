/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.keycloak;

import org.geoserver.security.keycloak.GeoServerKeycloakFilter;
import org.geoserver.security.keycloak.GeoServerKeycloakFilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

public class KeycloakAuthFilterPanelInfo
        extends AuthenticationFilterPanelInfo<
                GeoServerKeycloakFilterConfig, KeycloakAuthFilterPanel> {

    private static final long serialVersionUID = 1L;

    public KeycloakAuthFilterPanelInfo() {
        setComponentClass(KeycloakAuthFilterPanel.class);
        setServiceClass(GeoServerKeycloakFilter.class);
        setServiceConfigClass(GeoServerKeycloakFilterConfig.class);
    }
}

/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web.authentication;

import org.apache.wicket.model.IModel;
import org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProviderConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanel;

public class GeoFenceAuthProviderPanel
        extends AuthenticationProviderPanel<GeoFenceAuthenticationProviderConfig> {

    private static final long serialVersionUID = 4454241105050831394L;

    public GeoFenceAuthProviderPanel(
            String id, IModel<GeoFenceAuthenticationProviderConfig> model) {
        super(id, model);
    }
}

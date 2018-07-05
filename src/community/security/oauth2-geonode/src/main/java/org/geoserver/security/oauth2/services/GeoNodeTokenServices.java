/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import java.util.Map;
import org.geoserver.security.oauth2.GeoServerAccessTokenConverter;
import org.geoserver.security.oauth2.GeoServerOAuthRemoteTokenServices;

/**
 * Remote Token Services for GeoNode token details.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GeoNodeTokenServices extends GeoServerOAuthRemoteTokenServices {

    public GeoNodeTokenServices() {
        super(new GeoServerAccessTokenConverter());
    }

    protected void transformNonStandardValuesToStandardValues(Map<String, Object> map) {
        LOGGER.debug("Original map = " + map);
        map.put("user_name", map.get("issued_to")); // GeoNode sends 'client_id' as 'issued_to'
        LOGGER.debug("Transformed = " + map);
    }
}

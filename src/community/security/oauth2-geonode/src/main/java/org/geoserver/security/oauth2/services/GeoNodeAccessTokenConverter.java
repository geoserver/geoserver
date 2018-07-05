/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import org.geoserver.security.oauth2.GeoServerAccessTokenConverter;

/**
 * Access Token Converter for GeoNode token details.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GeoNodeAccessTokenConverter extends GeoServerAccessTokenConverter {

    public GeoNodeAccessTokenConverter() {
        super(new GeoNodeUserAuthenticationConverter());
    }
}

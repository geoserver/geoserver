/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import org.geoserver.config.GeoServer;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;

public class StylesServiceDescriptionProvider
        extends OgcApiServiceDescriptionProvider<StylesServiceInfo, StylesService> {

    public StylesServiceDescriptionProvider(GeoServer gs) {
        super(gs, "Styles", "Styles", "Styles");
    }
}

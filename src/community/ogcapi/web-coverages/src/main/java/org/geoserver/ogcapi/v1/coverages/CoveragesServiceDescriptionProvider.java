/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import org.geoserver.config.GeoServer;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;

public class CoveragesServiceDescriptionProvider
        extends OgcApiServiceDescriptionProvider<WCSInfo, CoveragesService> {

    public CoveragesServiceDescriptionProvider(GeoServer gs) {
        super(gs, "WCS", "Coverages", "Coverages");
    }
}

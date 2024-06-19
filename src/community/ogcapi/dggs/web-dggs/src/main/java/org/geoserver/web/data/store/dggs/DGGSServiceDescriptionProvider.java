/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.dggs;

import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.v1.dggs.DGGSInfo;
import org.geoserver.ogcapi.v1.dggs.DGGSService;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;

/** Provide description of DGGS services for welcome page. */
public class DGGSServiceDescriptionProvider
        extends OgcApiServiceDescriptionProvider<DGGSInfo, DGGSService> {

    public DGGSServiceDescriptionProvider(GeoServer gs) {
        super(gs, "DGGS", "DGGS", "DGGS");
    }
}

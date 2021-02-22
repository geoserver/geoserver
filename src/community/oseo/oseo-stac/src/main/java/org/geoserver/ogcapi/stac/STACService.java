/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geotools.util.logging.Logging;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.logging.Logger;

/** Implementation of OGC Features API service */
@APIService(
        service = "STAC",
        version = "1.0",
        landingPage = "ogc/stac",
        serviceClass = OSEOInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/stac")
public class STACService {

    static final Logger LOGGER = Logging.getLogger(STACService.class);
    private final GeoServer geoServer;

    public STACService(GeoServer geoServer, APIFilterParser filterParser) {
        this.geoServer = geoServer;
    }

    public OSEOInfo getService() {
        return geoServer.getService(OSEOInfo.class);
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public STACLandingPage getLandingPage() {
        return new STACLandingPage(getService(), getCatalog(), "ogc/features");
    }

}

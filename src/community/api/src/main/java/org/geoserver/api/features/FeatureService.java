/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.api.APIDispatcher;
import org.geoserver.api.APIService;
import org.geoserver.api.BaseURL;
import org.geoserver.api.HTMLResponseBody;
import org.geoserver.api.NegotiatedContentType;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory2;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(service = "Feature", version = "1.0", landingPage = "api/features")
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/features")
public class FeatureService {

    private static final Logger LOGGER = Logging.getLogger(FeatureService.class);

    private final GeoServerDataDirectory dataDirectory;
    private FilterFactory2 filterFactory;
    private final GeoServer geoServer;

    public FeatureService(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.dataDirectory = new GeoServerDataDirectory(geoServer.getCatalog().getResourceLoader());
    }

    private WFSInfo getService() {
        return geoServer.getService(WFSInfo.class);
    }

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    @GetMapping(name = "landingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "./landingPage.ftl", fileName = "landingPage.html")
    public LandingPageDocument getLandingPage(
            @BaseURL String baseURL) {
        return new LandingPageDocument(getService(), getCatalog(), "api/features");
    }

}

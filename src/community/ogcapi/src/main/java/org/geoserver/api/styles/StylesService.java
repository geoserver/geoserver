/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import org.geoserver.api.APIDispatcher;
import org.geoserver.api.APIService;
import org.geoserver.api.HTMLResponseBody;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(
    service = "Style",
    version = "1.0",
    landingPage = "ogc/styles",
    serviceClass = StylesServiceInfo.class
)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/styles")
public class StylesService {

    private final GeoServer geoServer;
    private final Catalog catalog;

    public StylesService(GeoServer geoServer, Catalog catalog) {
        this.catalog = catalog;
        this.geoServer = geoServer;
    }

    @GetMapping(name = "landingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public StylesLandingPage getLandingPage() {
        StylesServiceInfo styles = geoServer.getService(StylesServiceInfo.class);
        return new StylesLandingPage(
                (styles.getTitle() == null) ? "Styles server" : styles.getTitle(),
                (styles.getAbstract() == null) ? "" : styles.getAbstract());
    }
}

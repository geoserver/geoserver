/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.Arrays;
import java.util.List;
import org.geoserver.api.APIDispatcher;
import org.geoserver.api.APIService;
import org.geoserver.api.ConformanceDocument;
import org.geoserver.api.HTMLResponseBody;
import org.geoserver.api.OpenAPIMessageConverter;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.springframework.http.MediaType;
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

    public static final String CORE = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/core";
    public static final String HTML = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/html";
    public static final String JSON = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/json";
    public static final String MANAGE =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-styles";
    public static final String VALIDATION =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/style-validation";
    public static final String RESOURCES =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/resources";
    public static final String MANAGE_RESOURCES =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-resources";
    public static final String MAPBOX =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/mapbox-styles";
    public static final String SLD10 = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/sld-10";
    public static final String SLD11 = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/sld-11";
    public static final String CSS = "http://www.geoserver.org/opf-styles-1/1.0/conf/geocss";

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
        StylesServiceInfo styles = getService();
        return new StylesLandingPage(
                (styles.getTitle() == null) ? "Styles server" : styles.getTitle(),
                (styles.getAbstract() == null) ? "" : styles.getAbstract());
    }

    public StylesServiceInfo getService() {
        return geoServer.getService(StylesServiceInfo.class);
    }

    @GetMapping(path = "conformance", name = "conformance")
    @ResponseBody
    public ConformanceDocument conformance() {
        List<String> classes = Arrays.asList(CORE, HTML, JSON, MAPBOX, SLD10, SLD11);
        return new ConformanceDocument(classes);
    }

    @GetMapping(
        path = "api",
        name = "api",
        produces = {
            OpenAPIMessageConverter.OPEN_API_VALUE,
            "application/x-yaml",
            MediaType.TEXT_XML_VALUE
        }
    )
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() {
        return new StylesAPIBuilder().build(getService());
    }
}

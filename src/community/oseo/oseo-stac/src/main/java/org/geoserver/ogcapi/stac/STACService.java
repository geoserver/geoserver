/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geotools.util.logging.Logging;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** Implementation of OGC Features API service */
@APIService(
    service = "STAC",
    version = "1.0",
    landingPage = "ogc/stac",
    serviceClass = OSEOInfo.class
)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/stac")
public class STACService {

    public static final String STAC_VERSION = "1.0.0-beta.2";

    public static final String FEATURE_CORE =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core";
    public static final String FEATURE_HTML =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html";
    public static final String FEATURE_GEOJSON =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson";
    public static final String FEATURE_OAS30 =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30";
    public static final String FEATURE_CQL_TEXT =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/x-cql-text";
    public static final String STAC_CORE = "https://api.stacspec.org/v1.0.0-beta.1/core";
    public static final String STAC_SEARCH = "https://api.stacspec.org/v1.0.0-beta.1/item-search";
    public static final String STAC_FEATURES =
            "https://api.stacspec.org/spec/v1.0.0-beta.1/ogcapi-features";

    private static final String DISPLAY_NAME = "SpatioTemporal Asset Catalog";

    static final Logger LOGGER = Logging.getLogger(STACService.class);

    private final GeoServer geoServer;
    private final OpenSearchAccessProvider accessProvider;

    public STACService(GeoServer geoServer, OpenSearchAccessProvider accessProvider) {
        this.geoServer = geoServer;
        this.accessProvider = accessProvider;
    }

    public OSEOInfo getService() {
        return geoServer.getService(OSEOInfo.class);
    }

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public STACLandingPage getLandingPage() throws IOException {
        return new STACLandingPage(getService(), "ogc/stac", conformance().getConformsTo());
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes =
                Arrays.asList(
                        FEATURE_CORE,
                        FEATURE_OAS30,
                        FEATURE_HTML,
                        FEATURE_GEOJSON,
                        FEATURE_CQL_TEXT,
                        STAC_CORE,
                        STAC_FEATURES,
                        STAC_SEARCH);
        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(
        path = "api",
        name = "getApi",
        produces = {
            OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE,
            "application/x-yaml",
            MediaType.TEXT_XML_VALUE
        }
    )
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() throws IOException {
        return new STACAPIBuilder(accessProvider).build(getService());
    }
}

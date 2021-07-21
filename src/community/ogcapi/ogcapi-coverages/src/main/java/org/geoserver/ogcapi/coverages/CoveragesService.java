/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.coverages;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.wcs.WCSInfo;
import org.geotools.referencing.CRS;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** Implementation of OGC Coverages API service */
@APIService(
    service = "Coverages",
    version = "1.0",
    landingPage = "ogc/coverages",
    serviceClass = WCSInfo.class
)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/coverages")
public class CoveragesService {

    static final Pattern INTEGER = Pattern.compile("\\d+");
    public static final String CRS_PREFIX = "http://www.opengis.net/def/crs/EPSG/0/";
    public static final String DEFAULT_CRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

    private final GeoServer geoServer;

    public CoveragesService(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public static List<String> getCoverageCRS(CoverageInfo coverage, List<String> defaultCRS) {
        if (coverage.getResponseSRS() != null) {
            List<String> result =
                    coverage.getResponseSRS()
                            .stream()
                            // the GUI allows to enter codes as "EPSG:XYZW"
                            .map(c -> c.startsWith("EPSG:") ? c.substring(5) : c)
                            .map(c -> CRS_PREFIX + c)
                            .collect(Collectors.toList());
            result.remove(CoveragesService.DEFAULT_CRS);
            result.add(0, CoveragesService.DEFAULT_CRS);
            return result;
        }
        return defaultCRS;
    }

    public WCSInfo getService() {
        return geoServer.getService(WCSInfo.class);
    }

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public CoveragesLandingPage getLandingPage() {
        return new CoveragesLandingPage(getService(), getCatalog(), "ogc/coverages");
    }

    @GetMapping(path = "collections", name = "getCollections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsDocument getCollections() {
        return new CollectionsDocument(geoServer, getServiceCRSList());
    }

    protected List<String> getServiceCRSList() {
        List<String> result = getService().getSRS();

        if (result == null || result.isEmpty()) {
            // consult the EPSG databasee
            result =
                    CRS.getSupportedCodes("EPSG")
                            .stream()
                            .filter(c -> INTEGER.matcher(c).matches())
                            .map(c -> CRS_PREFIX + c)
                            .collect(Collectors.toList());
        } else {
            // the configured ones are just numbers, prefix
            result = result.stream().map(c -> CRS_PREFIX + c).collect(Collectors.toList());
        }
        // the Features API default CRS (cannot be contained due to the different prefixing)
        result.add(0, DEFAULT_CRS);
        return result;
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.maps;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.List;

@APIService(
        service = "Maps",
        version = "1.0",
        landingPage = "ogc/maps",
        serviceClass = WMSInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/maps")
public class MapsService {

    public static final String CORE = "http://www.opengis.net/spec/ogcapi-maps-1/1.0/req/core";
    public static final String GEODATA =
            "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/geodata";

    private static final String DISPLAY_NAME = "OGC API Maps";

    private final GeoServer geoServer;

    public MapsService(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public WMSInfo getService() {
        return geoServer.getService(WMSInfo.class);
    }

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public MapsLandingPage landingPage() {
        return new MapsLandingPage(getService(), getCatalog(), "ogc/maps");
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes =
                Arrays.asList(ConformanceClass.CORE, ConformanceClass.COLLECTIONS, CORE, GEODATA);
        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(path = "collections", name = "getCollections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsDocument getCollections() {
        return new CollectionsDocument(geoServer);
    }

    @GetMapping(path = "collections/{collectionId}", name = "describeCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionDocument collection(@PathVariable(name = "collectionId") String collectionId) {
        PublishedInfo p = getPublished(collectionId);
        CollectionDocument collection = new CollectionDocument(geoServer, p);

        return collection;
    }

    private PublishedInfo getPublished(String collectionId) {
        // single collection
        PublishedInfo p = getCatalog().getLayerByName(collectionId);
        if (p == null) getCatalog().getLayerGroupByName(collectionId);

        if (p == null)
            throw new ServiceException(
                    "Unknown collection " + collectionId,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "collectionId");

        return p;
    }
}

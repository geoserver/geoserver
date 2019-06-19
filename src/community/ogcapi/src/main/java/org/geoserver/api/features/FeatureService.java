/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

package org.geoserver.api.features;

import static org.geoserver.api.features.ConformanceDocument.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.geoserver.api.APIDispatcher;
import org.geoserver.api.APIService;
import org.geoserver.api.BaseURL;
import org.geoserver.api.HTMLResponseBody;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(service = "Feature", version = "1.0", landingPage = "ogc/features")
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
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public LandingPageDocument getLandingPage(@BaseURL String baseURL) {
        return new LandingPageDocument(getService(), getCatalog(), "ogc/features");
    }

    @GetMapping(path = "collections", name = "collections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsDocument getCollections(@BaseURL String baseURL) {
        return new CollectionsDocument(geoServer);
    }

    @GetMapping(path = "collections/{collectionId}", name = "collection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionDocument collection(@PathVariable(name = "collectionId") String collectionId) {
        // single collection
        Optional<FeatureTypeInfo> featureType =
                NCNameResourceCodec.getLayers(getCatalog(), collectionId)
                        .stream()
                        .filter(l -> l.getResource() instanceof FeatureTypeInfo)
                        .map(l -> (FeatureTypeInfo) l.getResource())
                        .findFirst();
        if (!featureType.isPresent()) {
            throw new ServiceException(
                    "Unknown collection " + collectionId,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "collectionId");
        }
        CollectionsDocument collections = new CollectionsDocument(geoServer, featureType.get());
        CollectionDocument collection = collections.getCollections().next();

        return collection;
    }

    @GetMapping(path = "conformance", name = "conformance")
    @ResponseBody
    public ConformanceDocument confrmance() {
        List<String> classes = Arrays.asList(CORE, OAS30, GEOJSON, GMLSF0);
        return new ConformanceDocument(classes);
    }
}

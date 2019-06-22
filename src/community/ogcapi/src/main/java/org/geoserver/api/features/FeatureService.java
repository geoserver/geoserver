/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */
package org.geoserver.api.features;

import static org.geoserver.api.features.ConformanceDocument.*;

import io.swagger.v3.oas.models.OpenAPI;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.api.*;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.StoredQueryProvider;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory2;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.xml.namespace.QName;

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
    public LandingPageDocument getLandingPage() {
        return new LandingPageDocument(getService(), getCatalog(), "ogc/features");
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
        return new OpenAPIBuilder().build(getService());
    }

    @GetMapping(path = "collections", name = "collections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsDocument getCollections() {
        return new CollectionsDocument(geoServer);
    }

    @GetMapping(path = "collections/{collectionId}", name = "collection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionDocument collection(@PathVariable(name = "collectionId") String collectionId) {
        FeatureTypeInfo ft = getFeatureType(collectionId);
        CollectionsDocument collections = new CollectionsDocument(geoServer, ft);
        CollectionDocument collection = collections.getCollections().next();

        return collection;
    }

    private FeatureTypeInfo getFeatureType(String collectionId) {
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
        return featureType.get();
    }

    @GetMapping(path = "conformance", name = "conformance")
    @ResponseBody
    public ConformanceDocument conformance() {
        List<String> classes = Arrays.asList(CORE, OAS30, GEOJSON, GMLSF0);
        return new ConformanceDocument(classes);
    }

    @GetMapping(path = "collections/{collectionId}/items", name = "items")
    @ResponseBody
    public FeaturesResponse getFeature(@PathVariable(name = "collectionId") String collectionId,
                                       @RequestParam(name = "startIndex", required = false, defaultValue = "0") BigInteger startIndex,
                                       @RequestParam(name = "limit", required = false) BigInteger limit) {
        // build the request in a way core WFS can understand it
        FeatureTypeInfo ft = getFeatureType(collectionId);
        GetFeatureRequest request = GetFeatureRequest.adapt(Wfs20Factory.eINSTANCE.createGetFeatureType());
        Query query = request.createQuery();
        query.setTypeNames(Arrays.asList(new QName(ft.getNamespace().getURI(), ft.getName())));
        request.setStartIndex(startIndex);
        request.setMaxFeatures(limit);
        request.setBaseUrl(RequestInfo.get().getBaseURL());
        request.getAdaptedQueries().add(query.getAdaptee());

        WFS3GetFeature gf = new WFS3GetFeature(getService(), getCatalog());
        gf.setFilterFactory(filterFactory);
        gf.setStoredQueryProvider(getStoredQueryProvider());
        FeatureCollectionResponse response = gf.run(request);

        return new FeaturesResponse(request.getAdaptee(), response);
    }

    private StoredQueryProvider getStoredQueryProvider() {
        return new StoredQueryProvider(getCatalog());
    }

}

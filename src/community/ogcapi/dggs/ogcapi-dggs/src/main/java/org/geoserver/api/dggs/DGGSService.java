/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.dggs;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.api.APIDispatcher;
import org.geoserver.api.APIException;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.APIService;
import org.geoserver.api.ConformanceDocument;
import org.geoserver.api.DefaultContentType;
import org.geoserver.api.HTMLResponseBody;
import org.geoserver.api.Link;
import org.geoserver.api.OpenAPIMessageConverter;
import org.geoserver.api.features.FeaturesGetFeature;
import org.geoserver.api.features.FeaturesResponse;
import org.geoserver.api.features.RFCGeoJSONFeaturesResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.EMFUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Implementation of OGC Features API service */
@APIService(
    service = "DGGS",
    version = "1.0",
    landingPage = "ogc/dggs",
    serviceClass = DGGSInfo.class
)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/dggs")
public class DGGSService {

    static final Logger LOGGER = Logging.getLogger(DGGSService.class);

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    public static final String CORE = "http://www.opengis.net/spec/ogcapi-dggs-1/1.0/conf/core";

    private final GeoServer gs;

    public DGGSService(GeoServer gs) {
        this.gs = gs;
    }

    private Catalog getCatalog() {
        return gs.getCatalog();
    }

    public DGGSInfo getService() {
        return gs.getService(DGGSInfo.class);
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public DGGSLandingPage getLandingPage() {
        return new DGGSLandingPage(getService(), getCatalog(), "ogc/dggs");
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    public ConformanceDocument conformance() {
        List<String> classes = Arrays.asList(CORE);
        return new ConformanceDocument(classes);
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
    public OpenAPI api() {
        return new DGGSAPIBuilder().build(getService());
    }

    @GetMapping(path = "collections", name = "getCollections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsDocument getCollections() {
        return new CollectionsDocument(gs);
    }

    @GetMapping(path = "collections/{collectionId}", name = "describeCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionDocument collection(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        FeatureTypeInfo ft = getFeatureType(collectionId);
        CollectionDocument collection = new CollectionDocument(gs, ft);

        return collection;
    }

    private FeatureTypeInfo getFeatureType(String collectionId) throws IOException {
        // single collection
        FeatureTypeInfo featureType = getCatalog().getFeatureTypeByName(collectionId);
        if (featureType == null) {
            throw new ServiceException(
                    "Unknown collection " + collectionId,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "collectionId");
        }
        if (!isDGGSType(featureType)) {
            throw new ServiceException(
                    "Collection " + collectionId + " is not backed by a DGGS data source",
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "collectionId");
        }
        return featureType;
    }

    /**
     * Returns true if the feature type is associated to a DGGSStore
     *
     * @param featureType
     * @return
     */
    public static boolean isDGGSType(FeatureTypeInfo featureType) {
        try {
            return featureType.getStore().getDataStore(null) instanceof DGGSStore;
        } catch (Exception e) {
            // stores that are not working are rather common, log at finer level
            LOGGER.log(Level.FINER, "Failed to grab store for " + featureType);
            return false;
        }
    }

    @GetMapping(path = "collections/{collectionId}/zones", name = "getZones")
    @ResponseBody
    @DefaultContentType(RFCGeoJSONFeaturesResponse.MIME)
    public FeaturesResponse zones(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "resolution", required = false, defaultValue = "0")
                    Integer resolution,
            @RequestParam(
                        name = "f",
                        required = false,
                        defaultValue = RFCGeoJSONFeaturesResponse.MIME
                    )
                    String format)
            throws Exception {
        // build the request in a way core WFS machinery can understand it
        return runGetFeature(
                collectionId,
                startIndex,
                limit,
                format,
                request -> {
                    // add the resolution hint
                    if (resolution != null) {
                        request.setViewParams(
                                Collections.singletonList(
                                        Collections.singletonMap(
                                                DGGSStore.VP_RESOLUTION,
                                                String.valueOf(resolution))));
                    }
                },
                collectionName ->
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionName)
                                + "/zones");
    }

    private void customizeByFormat(Query query, FeatureTypeInfo ft, String format)
            throws IOException {
        if (DGGSJSONMessageConverter.DGGS_JSON_MIME.equals(format)
                && query.getPropertyNames().isEmpty()) {
            // TODO: add support for complex features
            SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
            List<QName> attributes = new ArrayList<>();
            for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
                if (!Boolean.TRUE.equals(ad.getUserData().get(DGGSStore.DGGS_INTRINSIC)))
                    attributes.add(new QName(ad.getLocalName()));
            }
            EMFUtils.set(query.getAdaptee(), "propertyNames", attributes);
        }
    }

    @GetMapping(path = "collections/{collectionId}/neighbors", name = "getNeighbors")
    @ResponseBody
    @DefaultContentType(RFCGeoJSONFeaturesResponse.MIME)
    public FeaturesResponse neighbors(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "zone_id") String zoneId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "distance", required = false, defaultValue = "1") int distance,
            @RequestParam(
                        name = "f",
                        required = false,
                        defaultValue = RFCGeoJSONFeaturesResponse.MIME
                    )
                    String format)
            throws Exception {
        if (distance <= 0)
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Neighboring distance must be positive",
                    HttpStatus.BAD_REQUEST);
        int maxNeighborDistance = getService().getMaxNeighborDistance();
        if (maxNeighborDistance > 0 && distance > maxNeighborDistance) {
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Neighboring distance exceeds maximum value: " + maxNeighborDistance,
                    HttpStatus.BAD_REQUEST);
        }
        DGGSInstance ddgs = getDGGSInstance(collectionId);
        return runGetFeature(
                collectionId,
                startIndex,
                limit,
                format,
                request -> {
                    Query query = request.getQueries().get(0);
                    query.setFilter(
                            FF.equals(
                                    FF.function(
                                            "neighbor",
                                            FF.property("zoneId"),
                                            FF.literal(zoneId),
                                            FF.literal(distance)),
                                    FF.literal(true)));
                },
                collectionName ->
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionName)
                                + "/neighbors");
    }

    @GetMapping(path = "collections/{collectionId}/zone", name = "getZone")
    @ResponseBody
    @DefaultContentType(RFCGeoJSONFeaturesResponse.MIME)
    @HTMLResponseBody(templateName = "zone.ftl", fileName = "zone.html")
    public FeaturesResponse zone(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "zone_id") String zoneId,
            @RequestParam(
                        name = "f",
                        required = false,
                        defaultValue = RFCGeoJSONFeaturesResponse.MIME
                    )
                    String format)
            throws Exception {
        DGGSInstance ddgs = getDGGSInstance(collectionId);
        FeaturesResponse response =
                runGetFeature(
                        collectionId,
                        null,
                        null,
                        format,
                        request -> {
                            Query query = request.getQueries().get(0);
                            query.setFilter(FF.equals(FF.property("zoneId"), FF.literal(zoneId)));
                        },
                        collectionName ->
                                "ogc/dggs/collections/"
                                        + ResponseUtils.urlEncode(collectionName)
                                        + "/zones/"
                                        + ResponseUtils.urlEncode(zoneId));

        response.addLink(getParentsLink(collectionId, zoneId));
        response.addLink(getChildrenLink(collectionId, zoneId));
        response.addLink(getNeighborLink(collectionId, zoneId));

        return response;
    }

    private Link getParentsLink(String collectionId, String zoneId) {
        Map<String, String> params = new HashMap<>();
        params.put("f", "text/html");
        params.put("zone_id", zoneId);
        String url =
                buildURL(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionId)
                                + "/parents",
                        params,
                        URLMangler.URLType.SERVICE);
        Link link = new Link(url, "parents", "text/html", "Zone parents");
        link.setClassification("parents");
        return link;
    }

    private Link getChildrenLink(String collectionId, String zoneId) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("f", "text/html");
        params.put("zone_id", zoneId);
        params.put(
                "resolution",
                String.valueOf(getDGGSInstance(collectionId).getZone(zoneId).getResolution() + 1));
        String url =
                buildURL(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionId)
                                + "/children",
                        params,
                        URLMangler.URLType.SERVICE);
        Link link = new Link(url, "children", "text/html", "Zone immediate children");
        link.setClassification("children");
        return link;
    }

    private Link getNeighborLink(String collectionId, String zoneId) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("f", "text/html");
        params.put("zone_id", zoneId);
        params.put("distance", "1");
        String url =
                buildURL(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionId)
                                + "/neighbors",
                        params,
                        URLMangler.URLType.SERVICE);
        Link link = new Link(url, "neighbors", "text/html", "Zone immediate neighbors");
        link.setClassification("neighbors");
        return link;
    }

    private DGGSInstance getDGGSInstance(String collectionId) throws IOException {
        FeatureTypeInfo featureType = getFeatureType(collectionId);
        DGGSStore dggsStore = (DGGSStore) featureType.getStore().getDataStore(null);
        return dggsStore.getDGGSFeatureSource(featureType.getNativeName()).getDGGS();
    }

    @GetMapping(path = "collections/{collectionId}/children", name = "getChildren")
    @ResponseBody
    @DefaultContentType(RFCGeoJSONFeaturesResponse.MIME)
    public FeaturesResponse children(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "zone_id") String zoneId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "resolution", required = false) Integer resolution,
            @RequestParam(
                        name = "f",
                        required = false,
                        defaultValue = RFCGeoJSONFeaturesResponse.MIME
                    )
                    String format)
            throws Exception {
        return runGetFeature(
                collectionId,
                startIndex,
                limit,
                format,
                request -> {
                    Query query = request.getQueries().get(0);
                    query.setFilter(
                            FF.equals(
                                    FF.function(
                                            "children",
                                            FF.property("zoneId"),
                                            FF.literal(zoneId),
                                            FF.literal(resolution)),
                                    FF.literal(true)));
                },
                collectionName ->
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionName)
                                + "/children");
    }

    @GetMapping(path = "collections/{collectionId}/parents", name = "getParents")
    @ResponseBody
    @DefaultContentType(RFCGeoJSONFeaturesResponse.MIME)
    public FeaturesResponse parents(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "zone_id") String zoneId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(
                        name = "f",
                        required = false,
                        defaultValue = RFCGeoJSONFeaturesResponse.MIME
                    )
                    String format)
            throws Exception {
        return runGetFeature(
                collectionId,
                startIndex,
                limit,
                format,
                request -> {
                    Query query = request.getQueries().get(0);
                    query.setFilter(
                            FF.equals(
                                    FF.function(
                                            "parents", FF.property("zoneId"), FF.literal(zoneId)),
                                    FF.literal(true)));
                },
                collectionName ->
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionName)
                                + "/parents");
    }

    public FeaturesResponse runGetFeature(
            String collectionId,
            BigInteger startIndex,
            BigInteger limit,
            String format,
            Consumer<GetFeatureRequest> requestCustomizer,
            Function<String, String> pathBuilder)
            throws IOException {
        // build the request in a way core WFS machinery can understand it
        FeatureTypeInfo ft = getFeatureType(collectionId);
        GetFeatureRequest request =
                GetFeatureRequest.adapt(Wfs20Factory.eINSTANCE.createGetFeatureType());
        Query query = request.createQuery();
        query.setTypeNames(Arrays.asList(new QName(ft.getNamespace().getURI(), ft.getName())));
        customizeByFormat(query, ft, format);
        List<Filter> filters = new ArrayList<>();
        request.setStartIndex(startIndex);
        request.setMaxFeatures(getLimit(limit));
        request.setBaseUrl(APIRequestInfo.get().getBaseURL());
        request.getAdaptedQueries().add(query.getAdaptee());
        requestCustomizer.accept(request);

        // run it
        FeaturesGetFeature gf =
                new FeaturesGetFeature(gs.getService(WFSInfo.class), getCatalog()) {
                    @Override
                    protected String getItemsPath(String collectionName) {
                        return pathBuilder.apply(collectionName);
                    }
                };
        gf.setFilterFactory(FF);
        FeatureCollectionResponse response = gf.run(request);

        // build a response tracking both results and request to allow reusing the existing WFS
        // output formats
        return new FeaturesResponse(request.getAdaptee(), response);
    }

    private BigInteger getLimit(BigInteger limit) {
        int max = getService().getMaxNumberOfZonesForPreview();
        if (limit == null) return BigInteger.valueOf(max);
        if (limit.longValue() > max)
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Invalid limit value, maximum is " + max,
                    HttpStatus.BAD_REQUEST);
        return limit;
    }

    @GetMapping(path = "collections/{collectionId}/point", name = "point")
    @ResponseBody
    @DefaultContentType(RFCGeoJSONFeaturesResponse.MIME)
    @HTMLResponseBody(templateName = "zone.ftl", fileName = "zone.html")
    public FeaturesResponse zone(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "point") String pointSpec,
            @RequestParam(name = "resolution") int resolution,
            @RequestParam(
                        name = "f",
                        required = false,
                        defaultValue = RFCGeoJSONFeaturesResponse.MIME
                    )
                    String format)
            throws Exception {
        Point point = getPoint(pointSpec);
        DGGSInstance dggs = getDGGSInstance(collectionId);
        Zone zone = dggs.point(point, resolution);
        String zoneId = zone.getId();
        // we have the zoneId, now to and access the data for it
        FeaturesResponse response =
                runGetFeature(
                        collectionId,
                        null,
                        null,
                        format,
                        request -> {
                            Query query = request.getQueries().get(0);
                            query.setFilter(
                                    FF.equals(FF.property("zoneId"), FF.literal(zone.getId())));
                        },
                        collectionName ->
                                "ogc/dggs/collections/"
                                        + ResponseUtils.urlEncode(collectionName)
                                        + "/zones/"
                                        + ResponseUtils.urlEncode(zoneId));

        response.addLink(getParentsLink(collectionId, zoneId));
        response.addLink(getChildrenLink(collectionId, zoneId));
        response.addLink(getNeighborLink(collectionId, zoneId));

        return response;
    }

    @GetMapping(path = "collections/{collectionId}/polygon", name = "polygon")
    @ResponseBody
    @DefaultContentType(RFCGeoJSONFeaturesResponse.MIME)
    public FeaturesResponse polygon(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "polygon") String polygonWKT,
            @RequestParam(name = "resolution") int resolution,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(
                        name = "f",
                        required = false,
                        defaultValue = RFCGeoJSONFeaturesResponse.MIME
                    )
                    String format)
            throws Exception {
        Polygon polygon = (Polygon) new WKTReader().read(polygonWKT);
        // we have the zoneId, now to and access the data for it
        FeaturesResponse response =
                runGetFeature(
                        collectionId,
                        startIndex,
                        limit,
                        format,
                        request -> {
                            Query query = request.getQueries().get(0);
                            query.setFilter(
                                    FF.equals(
                                            FF.function(
                                                    "dggsPolygon",
                                                    FF.property("zoneId"),
                                                    FF.literal(polygon),
                                                    FF.literal(resolution)),
                                            FF.literal("true")));
                        },
                        collectionName ->
                                "ogc/dggs/collections/"
                                        + ResponseUtils.urlEncode(collectionName)
                                        + "/zones/");

        return response;
    }

    private Point getPoint(String pointSpec) throws ParseException {
        String spec = pointSpec.toUpperCase();
        if (spec.startsWith("POINT")) {
            return (Point) new WKTReader().read(spec);
        } else {
            String[] split = spec.split("\\s*,\\s*");
            if (split.length != 2) {
                throw new APIException(
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "Invalid point specification, should be a longitude and a latitude separated by a comma",
                        HttpStatus.BAD_REQUEST);
            }
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            return new GeometryFactory().createPoint(new Coordinate(x, y));
        }
    }
}

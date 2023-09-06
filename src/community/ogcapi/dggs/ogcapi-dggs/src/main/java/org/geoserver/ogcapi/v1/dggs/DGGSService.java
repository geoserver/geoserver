/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geotools.dggs.gstore.DGGSStore.RESOLUTION;

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
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.DimensionFilterBuilder;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.DateTimeList;
import org.geoserver.ogcapi.DefaultContentType;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ogcapi.PropertiesParser;
import org.geoserver.ogcapi.v1.features.FeaturesGetFeature;
import org.geoserver.ogcapi.v1.features.FeaturesResponse;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wms.WMS;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.EMFUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Implementation of OGC API - DGGS */
@APIService(
        service = "DGGS",
        version = "1.0.1",
        landingPage = "ogc/dggs/v1",
        serviceClass = DGGSInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/dggs/v1")
public class DGGSService {

    static final Logger LOGGER = Logging.getLogger(DGGSService.class);

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    public static final String CORE = "http://www.opengis.net/spec/ogcapi-dggs-1/1.0/conf/core";

    private final GeoServer gs;

    private static final String DISPLAY_NAME = "DGGS";

    /**
     * This is used for time support, default time and time filtering.
     *
     * <p>TODO: dimension support should be factored out of WMS and moved to a class in gs-main,
     * then the dependency to gs-wms can be removed.
     */
    private final WMS wms;

    DimensionFilterBuilder fb = new DimensionFilterBuilder(FF);

    public DGGSService(GeoServer gs, WMS wms) {
        this.gs = gs;
        this.wms = wms;
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
        return new DGGSLandingPage(getService(), getCatalog(), "ogc/dggs/v1");
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes = Arrays.asList(CORE);
        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(
            path = {"openapi", "openapi.json", "openapi.yaml"},
            name = "getApi",
            produces = {
                OPEN_API_MEDIA_TYPE_VALUE,
                APPLICATION_YAML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() throws IOException {
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

    /**
     * Returns the feature type for the specified collection, checking it's a valid DGGS collection
     */
    protected FeatureTypeInfo getFeatureType(String collectionId) throws IOException {
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
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse zones(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "resolution", required = false, defaultValue = "0")
                    Integer resolution,
            @RequestParam(name = "datetime", required = false) DateTimeList datetime,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "geom", required = false) String wkt,
            @RequestParam(name = "zones", required = false) String zones,
            @RequestParam(name = "properties", required = false) String properties,
            @RequestParam(
                            name = "f",
                            required = false,
                            defaultValue = OGCAPIMediaTypes.GEOJSON_VALUE)
                    String format)
            throws Exception {
        // handle possible geometry filters
        DGGSGeometryFilterParser geometryParser =
                new DGGSGeometryFilterParser(FF, getDGGSInstance(collectionId));
        geometryParser.setBBOX(bbox);
        geometryParser.setGeometry(wkt);
        geometryParser.setZoneIds(zones, resolution);

        // build the request in a way core WFS machinery can understand it
        return runGetFeature(
                collectionId,
                datetime,
                properties,
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
                        Filter resolutionFilter =
                                FF.equals(FF.property(RESOLUTION), FF.literal(resolution));
                        mixFilter(request, resolutionFilter);

                        Filter geometryFilter = geometryParser.getFilter();
                        if (geometryFilter != null && geometryFilter != Filter.INCLUDE) {
                            mixFilter(request, geometryFilter);
                        }
                    }
                },
                collectionName ->
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionName)
                                + "/zones");
    }

    void mixFilter(GetFeatureRequest request, Filter mix) {
        Query query = request.getQueries().get(0);
        Filter filter = query.getFilter();
        if (filter == Filter.INCLUDE || filter == null) {
            query.setFilter(mix);
        } else {
            query.setFilter(FF.and(mix, filter));
        }
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
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse neighbors(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "zone_id") String zoneId,
            @RequestParam(name = "properties", required = false) String properties,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "datetime", required = false) DateTimeList datetime,
            @RequestParam(name = "distance", required = false, defaultValue = "1") int distance,
            @RequestParam(
                            name = "f",
                            required = false,
                            defaultValue = OGCAPIMediaTypes.GEOJSON_VALUE)
                    String format)
            throws Exception {
        if (distance <= 0)
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "Neighboring distance must be positive",
                    HttpStatus.BAD_REQUEST);
        int maxNeighborDistance = getService().getMaxNeighborDistance();
        if (maxNeighborDistance > 0 && distance > maxNeighborDistance) {
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "Neighboring distance exceeds maximum value: " + maxNeighborDistance,
                    HttpStatus.BAD_REQUEST);
        }
        PropertyIsEqualTo neighborFilter =
                FF.equals(
                        FF.function(
                                "neighbor",
                                FF.property("zoneId"),
                                FF.literal(zoneId),
                                FF.literal(distance)),
                        FF.literal(true));
        return runGetFeature(
                collectionId,
                datetime,
                properties,
                startIndex,
                limit,
                format,
                request -> {
                    mixFilter(request, neighborFilter);
                },
                collectionName ->
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionName)
                                + "/neighbors");
    }

    @GetMapping(path = "collections/{collectionId}/zone", name = "getZone")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    @HTMLResponseBody(templateName = "zone.ftl", fileName = "zone.html")
    public FeaturesResponse zone(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "zone_id") String zoneId,
            @RequestParam(name = "datetime", required = false) DateTimeList datetime,
            @RequestParam(name = "properties", required = false) String properties,
            @RequestParam(
                            name = "f",
                            required = false,
                            defaultValue = OGCAPIMediaTypes.GEOJSON_VALUE)
                    String format)
            throws Exception {
        FeaturesResponse response =
                runGetFeature(
                        collectionId,
                        datetime,
                        properties,
                        null,
                        null,
                        format,
                        request -> {
                            mixFilter(
                                    request, FF.equals(FF.property("zoneId"), FF.literal(zoneId)));
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
        reflectDatetime(params);
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

    private void reflectDatetime(Map<String, String> params) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        String datetime = requestInfo.getRequest().getParameter("datetime");
        if (datetime != null) {
            params.put("datetime", datetime);
        }
    }

    private Link getChildrenLink(String collectionId, String zoneId) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("f", "text/html");
        params.put("zone_id", zoneId);
        reflectDatetime(params);
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
        reflectDatetime(params);
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

    DGGSInstance getDGGSInstance(String collectionId) throws IOException {
        FeatureTypeInfo featureType = getFeatureType(collectionId);
        DGGSStore dggsStore = (DGGSStore) featureType.getStore().getDataStore(null);
        return dggsStore.getDGGSFeatureSource(featureType.getNativeName()).getDGGS();
    }

    @GetMapping(path = "collections/{collectionId}/children", name = "getChildren")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse children(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "zone_id") String zoneId,
            @RequestParam(name = "properties", required = false) String properties,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "resolution", required = false) Integer resolution,
            @RequestParam(name = "datetime", required = false) DateTimeList datetime,
            @RequestParam(
                            name = "f",
                            required = false,
                            defaultValue = OGCAPIMediaTypes.GEOJSON_VALUE)
                    String format)
            throws Exception {
        PropertyIsEqualTo childFilter =
                FF.equals(
                        FF.function(
                                "children",
                                FF.property("zoneId"),
                                FF.literal(zoneId),
                                FF.literal(resolution)),
                        FF.literal(true));
        return runGetFeature(
                collectionId,
                datetime,
                properties,
                startIndex,
                limit,
                format,
                request -> {
                    mixFilter(request, childFilter);
                },
                collectionName ->
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionName)
                                + "/children");
    }

    @GetMapping(path = "collections/{collectionId}/parents", name = "getParents")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse parents(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "zone_id") String zoneId,
            @RequestParam(name = "datetime", required = false) DateTimeList datetime,
            @RequestParam(name = "properties", required = false) String properties,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(
                            name = "f",
                            required = false,
                            defaultValue = OGCAPIMediaTypes.GEOJSON_VALUE)
                    String format)
            throws Exception {
        PropertyIsEqualTo parentFilter =
                FF.equals(
                        FF.function("parents", FF.property("zoneId"), FF.literal(zoneId)),
                        FF.literal(true));
        // another filter to help implementation that cannot optimize out the above call
        return runGetFeature(
                collectionId,
                datetime,
                properties,
                startIndex,
                limit,
                format,
                request -> {
                    mixFilter(request, parentFilter);
                },
                collectionName ->
                        "ogc/dggs/collections/"
                                + ResponseUtils.urlEncode(collectionName)
                                + "/parents");
    }

    public FeaturesResponse runGetFeature(
            String collectionId,
            DateTimeList dateTimeList,
            String properties,
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
        if (properties != null) {
            List<String> propertyNames = (new PropertiesParser(ft)).parse(properties);
            query.setPropertyNames(propertyNames);
        }
        customizeByFormat(query, ft, format);
        query.setFilter(buildDateTimeFilter(ft, dateTimeList));
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

    protected Filter buildDateTimeFilter(FeatureTypeInfo ft, DateTimeList dateTimeList)
            throws IOException {
        DimensionInfo time = ft.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null) return Filter.INCLUDE;
        if (dateTimeList == null || dateTimeList.isEmpty()) {
            dateTimeList = new DateTimeList();
            dateTimeList.add(wms.getDefaultTime(ft));
        }
        return wms.getTimeElevationToFilter(dateTimeList, null, ft);
    }

    private BigInteger getLimit(BigInteger limit) {
        int max = getService().getMaxNumberOfZonesForPreview();
        if (limit == null) return BigInteger.valueOf(max);
        return limit;
    }

    @GetMapping(path = "collections/{collectionId}/point", name = "point")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    @HTMLResponseBody(templateName = "zone.ftl", fileName = "zone.html")
    public FeaturesResponse point(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "point") String pointSpec,
            @RequestParam(name = "resolution") int resolution,
            @RequestParam(name = "properties", required = false) String properties,
            @RequestParam(name = "datetime", required = false) DateTimeList datetime,
            @RequestParam(
                            name = "f",
                            required = false,
                            defaultValue = OGCAPIMediaTypes.GEOJSON_VALUE)
                    String format)
            throws Exception {
        Point point = getPoint(pointSpec);
        @SuppressWarnings("PMD.CloseResource") // managed by the store
        DGGSInstance dggs = getDGGSInstance(collectionId);
        Zone zone = dggs.point(point, resolution);
        String zoneId = zone.getId();
        // we have the zoneId, now to and access the data for it
        FeaturesResponse response =
                runGetFeature(
                        collectionId,
                        datetime,
                        properties,
                        null,
                        null,
                        format,
                        request -> {
                            mixFilter(
                                    request,
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
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse polygon(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "polygon") String polygonWKT,
            @RequestParam(name = "resolution") int resolution,
            @RequestParam(name = "properties", required = false) String properties,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "compact", required = true, defaultValue = "true") boolean compact,
            @RequestParam(name = "datetime", required = false) DateTimeList datetime,
            @RequestParam(
                            name = "f",
                            required = false,
                            defaultValue = OGCAPIMediaTypes.GEOJSON_VALUE)
                    String format)
            throws Exception {
        Polygon polygon = getPolygon(polygonWKT);
        // Filter resolutionFilter = FF.lessOrEqual(FF.property(RESOLUTION),
        // FF.literal(resolution));
        PropertyIsEqualTo polygonFilter =
                FF.equals(
                        FF.function(
                                "dggsPolygon",
                                FF.property("zoneId"),
                                FF.literal(polygon),
                                FF.literal(resolution),
                                FF.literal(compact)),
                        FF.literal("true"));
        // we have the zoneId, now to and access the data for it
        FeaturesResponse response =
                runGetFeature(
                        collectionId,
                        datetime,
                        properties,
                        startIndex,
                        limit,
                        format,
                        request -> {
                            Query query = request.getQueries().get(0);
                            query.setFilter(polygonFilter);
                        },
                        collectionName ->
                                "ogc/dggs/collections/"
                                        + ResponseUtils.urlEncode(collectionName)
                                        + "/polygon/");

        return response;
    }

    public Polygon getPolygon(String polygonWKT) {
        try {
            Polygon polygon = (Polygon) new WKTReader().read(polygonWKT);
            polygon.setUserData(DefaultGeographicCRS.WGS84);
            return polygon;
        } catch (ParseException e) {
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Invalid WKT specification for the polygon parameter",
                    HttpStatus.BAD_REQUEST,
                    e);
        }
    }

    private Point getPoint(String pointSpec) throws ParseException {
        String spec = pointSpec.toUpperCase();
        if (spec.startsWith("POINT")) {
            return (Point) new WKTReader().read(spec);
        } else {
            String[] split = spec.split("\\s*,\\s*");
            if (split.length != 2) {
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "Invalid point specification, should be a longitude and a latitude separated by a comma",
                        HttpStatus.BAD_REQUEST);
            }
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            return new GeometryFactory().createPoint(new Coordinate(x, y));
        }
    }
}

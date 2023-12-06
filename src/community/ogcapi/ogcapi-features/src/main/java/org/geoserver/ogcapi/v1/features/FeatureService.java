/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.geoserver.ogcapi.ConformanceClass.CQL2_ADVANCED;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_ARITHMETIC;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_BASIC;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_BASIC_SPATIAL;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_FUNCTIONS;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_PROPERTY_PROPERTY;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_SPATIAL;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_TEXT;
import static org.geoserver.ogcapi.ConformanceClass.ECQL;
import static org.geoserver.ogcapi.ConformanceClass.ECQL_TEXT;
import static org.geoserver.ogcapi.ConformanceClass.FEATURES_FILTER;
import static org.geoserver.ogcapi.ConformanceClass.FILTER;
import static org.geoserver.ogcapi.ConformanceClass.IDS;
import static org.geoserver.ogcapi.ConformanceClass.SEARCH;
import static org.geoserver.ogcapi.ConformanceClass.SORTBY;
import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import net.opengis.wfs20.Wfs20Factory;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.config.GeoServer;
import org.geoserver.crs.CapabilitiesCRSProvider;
import org.geoserver.ogcapi.APIBBoxParser;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APISearchQuery;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.DefaultContentType;
import org.geoserver.ogcapi.FunctionsDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.JSONSchemaMessageConverter;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.QueryablesBuilder;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.StoredQueryProvider;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.DateRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Implementation of OGC Features API service */
@APIService(
        service = "Features",
        version = "1.0.1",
        landingPage = "ogc/features/v1",
        serviceClass = WFSInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/features/v1")
public class FeatureService {

    static final Pattern INTEGER = Pattern.compile("\\d+");

    public static final String CORE = "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core";
    public static final String HTML = "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html";
    public static final String GEOJSON =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson";
    public static final String GMLSF0 =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf0";
    public static final String GMLSF2 =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf2";
    public static final String OAS30 =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30";

    public static final String CRS_BY_REFERENCE =
            "http://www.opengis.net/spec/ogcapi-features-2/1.0/conf/crs";

    public static final String CRS_PREFIX = "http://www.opengis.net/def/crs/EPSG/0/";
    public static final String DEFAULT_CRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

    public static String ITEM_ID = "OGCFeatures:ItemId";

    private static final String DISPLAY_NAME = "OGC API Features";

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private final GeoServer geoServer;
    private final APIFilterParser filterParser;

    private TimeParser timeParser = new TimeParser();

    public FeatureService(GeoServer geoServer, APIFilterParser filterParser) {
        this.geoServer = geoServer;
        this.filterParser = filterParser;
    }

    /** Returns the provided CRS list, unless the feature type has its own local override */
    public static List<String> getFeatureTypeCRS(
            FeatureTypeInfo featureType, List<String> defaultCRS) {
        // by default use the provided list, unless there is an override
        if (featureType.isOverridingServiceSRS()) {
            List<String> result =
                    featureType.getResponseSRS().stream()
                            .map(c -> mapResponseSRS(c))
                            .collect(Collectors.toList());
            result.remove(FeatureService.DEFAULT_CRS);
            result.add(0, FeatureService.DEFAULT_CRS);
            return result;
        }
        return defaultCRS;
    }

    private static String mapResponseSRS(String srs) {
        int idx = srs.indexOf(":");
        if (idx == -1) return mapCRSCode("EPSG", srs);
        String authority = srs.substring(0, idx);
        String code = srs.substring(idx + 1);
        return mapCRSCode(authority, code);
    }

    /** Returns the CRS-URI for a given CRS. */
    public static String getCRSURI(CoordinateReferenceSystem crs) throws FactoryException {
        if (CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) {
            return FeatureService.DEFAULT_CRS;
        }
        String identifier = ResourcePool.lookupIdentifier(crs, false);
        return mapResponseSRS(identifier);
    }

    /** Maps authority and code to a CRS URI */
    static String mapCRSCode(String authority, String code) {
        return "http://www.opengis.net/def/crs/" + authority + "/0/" + code;
    }

    protected List<String> getServiceCRSList() {
        List<String> result = getService().getSRS();

        if (result == null || result.isEmpty()) {
            // consult the referencing database
            CapabilitiesCRSProvider provider = new CapabilitiesCRSProvider();
            provider.getAuthorityExclusions().add("CRS");
            provider.setCodeMapper(FeatureService::mapCRSCode);
            result = new ArrayList<>(provider.getCodes());
        } else {
            // the configured ones are just numbers, prefix
            result = result.stream().map(c -> mapResponseSRS(c)).collect(Collectors.toList());
        }
        // the Features API default CRS (cannot be contained due to the different prefixing)
        result.add(0, DEFAULT_CRS);
        return result;
    }

    public WFSInfo getService() {
        return geoServer.getService(WFSInfo.class);
    }

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public FeaturesLandingPage getLandingPage() {
        return new FeaturesLandingPage(getService(), getCatalog(), "ogc/features/v1");
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
        return new FeaturesAPIBuilder().build(getService());
    }

    @GetMapping(path = "collections", name = "getCollections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsDocument getCollections() {
        return new CollectionsDocument(geoServer, getServiceCRSList());
    }

    @GetMapping(path = "functions", name = "getFunctions")
    @ResponseBody
    @HTMLResponseBody(templateName = "functions.ftl", fileName = "functions.html")
    public FunctionsDocument getFunctions() {
        return new FunctionsDocument();
    }

    @GetMapping(path = "collections/{collectionId}", name = "describeCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionDocument collection(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        FeatureTypeInfo ft = getFeatureType(collectionId);
        CollectionDocument collection =
                new CollectionDocument(geoServer, ft, getFeatureTypeCRS(ft, getServiceCRSList()));

        return collection;
    }

    @GetMapping(
            path = "collections/{collectionId}/queryables",
            name = "getQueryables",
            produces = JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE)
    @ResponseBody
    @HTMLResponseBody(templateName = "queryables.ftl", fileName = "queryables.html")
    public Queryables queryables(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        FeatureTypeInfo ft = getFeatureType(collectionId);
        String id =
                ResponseUtils.buildURL(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/features/v1/collections/"
                                + ResponseUtils.urlEncode(collectionId)
                                + "/queryables",
                        null,
                        URLMangler.URLType.RESOURCE);
        Queryables queryables = new QueryablesBuilder(id).forType(ft).build();
        queryables.addSelfLinks("collections/" + collectionId + "/queryables");
        return queryables;
    }

    @GetMapping(
            path = "collections/{collectionId}/schemas/fg/{schemaId}.json",
            name = "getJSONFGSchemas",
            produces = JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE)
    public void getJSONFGSchemas(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "schemaId") String schemaId,
            HttpServletResponse response)
            throws IOException {
        FeatureTypeInfo ft = getFeatureType(collectionId);
        FeatureType featureType = ft.getFeatureType();
        String schema = new JSONFGSchemaBuilder(featureType, schemaId).build();
        response.setContentType(JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE);
        IOUtils.write(schema, response.getOutputStream(), StandardCharsets.UTF_8);
    }

    private FeatureTypeInfo getFeatureType(String collectionId) {
        // single collection
        FeatureTypeInfo featureType = getCatalog().getFeatureTypeByName(collectionId);
        if (featureType == null) {
            throw new APIException(
                    APIException.NOT_FOUND,
                    "Unknown collection " + collectionId,
                    HttpStatus.NOT_FOUND);
        }
        return featureType;
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes =
                Arrays.asList(
                        CORE,
                        OAS30,
                        HTML,
                        GEOJSON,
                        /* GMLSF0, GS does not use the gmlsf namespace */
                        CRS_BY_REFERENCE,
                        FEATURES_FILTER,
                        FILTER,
                        SEARCH,
                        ECQL,
                        ECQL_TEXT,
                        CQL2_BASIC,
                        CQL2_ADVANCED,
                        CQL2_ARITHMETIC,
                        CQL2_PROPERTY_PROPERTY,
                        CQL2_BASIC_SPATIAL,
                        CQL2_SPATIAL,
                        CQL2_FUNCTIONS,
                        /* CQL2_TEMPORAL excluded for now, no support for all operators */
                        /* CQL2_ARRAY excluded, no support for array operations now */
                        CQL2_TEXT,
                        /* CQL2_JSON very different from the binding we have */
                        SORTBY,
                        IDS);
        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(path = "collections/{collectionId}/items/{itemId:.+}", name = "getFeature")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse item(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "bbox-crs", required = false) String bboxCRS,
            @RequestParam(name = "time", required = false) String time,
            @PathVariable(name = "itemId") String itemId,
            @RequestParam(name = "crs", required = false) String crs)
            throws Exception {
        return items(
                collectionId,
                startIndex,
                limit,
                bbox,
                bboxCRS,
                time,
                null, /* filter */
                null, /* filter-lang */
                null, /* filter-crs */
                null, /* sortby */
                crs,
                null, /* ids */
                itemId);
    }

    @GetMapping(path = "collections/{collectionId}/items", name = "getFeatures")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse items(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "bbox-crs", required = false) String bboxCRS,
            @RequestParam(name = "datetime", required = false) String datetime,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "filter-lang", required = false) String filterLanguage,
            @RequestParam(name = "filter-crs", required = false) String filterCRS,
            @RequestParam(name = "sortby", required = false) SortBy[] sortBy,
            @RequestParam(name = "crs", required = false) String crs,
            @RequestParam(name = "ids", required = false) List<String> ids,
            String itemId)
            throws Exception {
        // build the request in a way core WFS machinery can understand it
        FeatureTypeInfo ft = getFeatureType(collectionId);
        GetFeatureRequest request =
                GetFeatureRequest.adapt(Wfs20Factory.eINSTANCE.createGetFeatureType());
        Query query = request.createQuery();
        query.setTypeNames(Arrays.asList(new QName(ft.getNamespace().getURI(), ft.getName())));
        List<Filter> filters = new ArrayList<>();
        if (bbox != null) {
            filters.add(APIBBoxParser.toFilter(bbox, bboxCRS));
        }
        if (datetime != null) {
            filters.add(buildTimeFilter(ft, datetime));
        }
        if (itemId != null) {
            filters.add(FF.id(FF.featureId(itemId)));
        }

        // The ids parameter is part of the draft proposal "Query by IDs". The syntax and semantic
        // of the parameter is subject to change in a future release. Its usage should be carefully
        // considered.
        if (ids != null && !ids.isEmpty()) {
            filters.add(buildIdsFilter(ids));
        }

        if (filter != null) {
            Filter parsedFilter = filterParser.parse(filter, filterLanguage, filterCRS);
            filters.add(parsedFilter);
        }
        query.setFilter(mergeFiltersAnd(filters));
        if (sortBy != null) {
            query.setSortBy(ImmutableList.copyOf(sortBy));
        }
        if (crs != null) {
            query.setSrsName(new URI(crs));
        } else {
            query.setSrsName(new URI("EPSG:4326"));
        }
        request.setStartIndex(startIndex);
        request.setMaxFeatures(limit);
        request.setBaseUrl(APIRequestInfo.get().getBaseURL());
        request.getAdaptedQueries().add(query.getAdaptee());

        // run it
        FeaturesGetFeature gf = new FeaturesGetFeature(getService(), getCatalog());
        gf.setFilterFactory(FF);
        gf.setStoredQueryProvider(getStoredQueryProvider());
        FeatureCollectionResponse response = gf.run(request);

        // store information about single vs multi request
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(ITEM_ID, itemId, RequestAttributes.SCOPE_REQUEST);
        }

        // build a response tracking both results and request to allow reusing the existing WFS
        // output formats
        return new FeaturesResponse(request.getAdaptee(), response);
    }

    @PostMapping(path = "collections/{collectionId}/search", name = "searchFeatures")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse search(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestBody APISearchQuery query)
            throws Exception {

        // WARNING:
        // This endpoint is part of the draft proposal "OGC API - Features - Part 5". The syntax and
        // semantic of the endpoint is subject to change in a future release. Its usage should be
        // carefully considered.
        return items(
                collectionId,
                query.getStartIndex(),
                query.getLimit(),
                query.getBbox(),
                query.getBboxCRS(),
                query.getDatetime(),
                query.getFilter(),
                query.getFilterLang(),
                query.getFilterCRS(),
                query.getSortBy(),
                query.getCrs(),
                query.getIds(),
                null);
    }

    /** TODO: use DimensionInfo instead? It's used to return the time range in the collection */
    private Filter buildTimeFilter(FeatureTypeInfo ft, String time)
            throws ParseException, IOException {
        Collection times = timeParser.parse(time);
        if (times.isEmpty() || times.size() > 1) {
            throw new ServiceException(
                    "Invalid time specification, must be a single time, or a time range",
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "time");
        }

        List<Filter> filters = new ArrayList<>();
        Object timeSpec = times.iterator().next();
        for (String timeProperty : getTimeProperties(ft)) {
            PropertyName property = FF.property(timeProperty);
            Filter filter;
            if (timeSpec instanceof Date) {
                filter = FF.equals(property, FF.literal(timeSpec));
            } else if (timeSpec instanceof DateRange) {
                DateRange dateRange = (DateRange) timeSpec;
                Literal before = FF.literal(dateRange.getMinValue());
                Literal after = FF.literal(dateRange.getMaxValue());
                filter = FF.between(property, before, after);
            } else {
                throw new IllegalArgumentException("Cannot build time filter out of " + timeSpec);
            }

            filters.add(filter);
        }

        return mergeFiltersOr(filters);
    }

    private List<String> getTimeProperties(FeatureTypeInfo ft) throws IOException {
        FeatureType schema = ft.getFeatureType();
        return schema.getDescriptors().stream()
                .filter(pd -> Date.class.isAssignableFrom(pd.getType().getBinding()))
                .map(pd -> pd.getName().getLocalPart())
                .collect(Collectors.toList());
    }

    private Id buildIdsFilter(List<String> ids) {
        FeatureId[] featureIds =
                ids.stream()
                        .map((id) -> FF.featureId(id))
                        .collect(Collectors.toList())
                        .toArray(new FeatureId[ids.size()]);

        return FF.id(featureIds);
    }

    private Filter mergeFiltersAnd(List<Filter> filters) {
        if (filters.isEmpty()) {
            return Filter.INCLUDE;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return FF.and(filters);
        }
    }

    private Filter mergeFiltersOr(List<Filter> filters) {
        if (filters.isEmpty()) {
            return Filter.EXCLUDE;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return FF.or(filters);
        }
    }

    private StoredQueryProvider getStoredQueryProvider() {
        return new StoredQueryProvider(getCatalog());
    }
}

/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import net.opengis.wfs20.Wfs20Factory;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.config.GeoServer;
import org.geoserver.crs.CapabilitiesCRSProvider;
import org.geoserver.ogcapi.APIBBoxParser;
import org.geoserver.ogcapi.APIConformance;
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
import org.geotools.api.feature.type.PropertyDescriptor;
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
import org.geotools.util.logging.Logging;
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
@APIService(service = "Features", version = "1.0.1", landingPage = "ogc/features/v1", serviceClass = WFSInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/features/v1")
public class FeatureService {

    private static final Logger LOGGER = Logging.getLogger(FeatureService.class);

    static final Pattern INTEGER = Pattern.compile("\\d+");

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
    public static List<String> getFeatureTypeCRS(FeatureTypeInfo featureType, List<String> defaultCRS) {
        // by default use the provided list, unless there is an override
        if (featureType.isOverridingServiceSRS()) {
            List<String> result = featureType.getResponseSRS().stream()
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

    @SuppressWarnings("unused")
    public WFSInfo getServiceInfo() {
        // required for DisabledServiceCheck class
        return getService();
    }

    /**
     * List conformance implementations implemented for FeatureService.
     *
     * @return List of implemented conformance available for use
     */
    public List<APIConformance> getConformances() {
        List<APIConformance> conformances = Arrays.asList(
                FeatureConformance.CORE,
                FeatureConformance.OAS30,
                FeatureConformance.HTML,
                FeatureConformance.GEOJSON,
                FeatureConformance.GMLSF2,
                FeatureConformance.CRS_BY_REFERENCE,
                FeatureConformance.FEATURES_FILTER,
                FeatureConformance.FILTER,
                FeatureConformance.QUERYABLES,
                FeatureConformance.IDS,
                FeatureConformance.SEARCH,
                FeatureConformance.SORTBY,
                ECQLConformance.ECQL_TEXT,
                CQL2Conformance.CQL2_TEXT,
                CQL2Conformance.CQL2_ADVANCED,
                CQL2Conformance.CQL2_ARITHMETIC,
                CQL2Conformance.CQL2_BASIC,
                CQL2Conformance.CQL2_BASIC_SPATIAL,
                CQL2Conformance.CQL2_FUNCTIONS,
                CQL2Conformance.CQL2_PROPERTY_PROPERTY,
                CQL2Conformance.CQL2_SPATIAL);
        // FeatureConformance.GMLSF0, // does not use the gmlsf namespace
        // CQL2Conformance.CQL2_JSON, // Very different from the binding we have
        // CQL2Conformance.CQL2_ARRAY, // excluded, no support for array operations now
        // CQL2Conformance.CQL2_TEMPORAL, // excluded for now, no support for all operators
        return conformances;
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
            produces = {OPEN_API_MEDIA_TYPE_VALUE, APPLICATION_YAML_VALUE, MediaType.TEXT_XML_VALUE})
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
        WFSInfo wfs = getServiceInfo();
        CQL2Conformance cql2 = CQL2Conformance.configuration(wfs);
        if (!cql2.functions(wfs)) {
            throw new APIException(
                    APIException.NOT_FOUND, "Functions not supported by the service.", HttpStatus.NOT_FOUND);
        }
        return new FunctionsDocument();
    }

    @GetMapping(path = "collections/{collectionId}", name = "describeCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionDocument collection(@PathVariable(name = "collectionId") String collectionId) throws IOException {
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
    public Queryables queryables(@PathVariable(name = "collectionId") String collectionId) throws IOException {
        FeatureTypeInfo ft = getFeatureType(collectionId);
        String id = ResponseUtils.buildURL(
                APIRequestInfo.get().getBaseURL(),
                "ogc/features/v1/collections/" + ResponseUtils.urlEncode(collectionId) + "/queryables",
                null,
                URLMangler.URLType.RESOURCE);
        Queryables queryables = new QueryablesBuilder(id).forType(ft).build();
        queryables.addSelfLinks("ogc/features/v1/collections/" + collectionId + "/queryables");
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
            throw new APIException(APIException.NOT_FOUND, "Unknown collection " + collectionId, HttpStatus.NOT_FOUND);
        }
        return featureType;
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        WFSInfo wfsInfo = getServiceInfo();

        List<APIConformance> conformances = new ArrayList<>();

        FeatureConformance featuresConformance = FeatureConformance.configuration(wfsInfo);
        if (featuresConformance.isEnabled(wfsInfo)) {
            conformances.addAll(featuresConformance.conformances(wfsInfo));

            ECQLConformance ecqlConformance = ECQLConformance.configuration(wfsInfo);
            conformances.addAll(ecqlConformance.conformances(wfsInfo));

            CQL2Conformance cql2Conformance = CQL2Conformance.configuration(wfsInfo);
            conformances.addAll(cql2Conformance.conformances(wfsInfo));
        }

        // only advertise what is actually implemented
        // conformances.retainAll(getConformances());

        List<String> classes = conformances.stream().map(APIConformance::getId).collect(Collectors.toList());

        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(path = "collections/{collectionId}/items/{itemId:.+}", name = "getFeature")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse item(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0") BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "bbox-crs", required = false) String bboxCRS,
            @RequestParam(name = "time", required = false) String time,
            @PathVariable(name = "itemId") String itemId,
            @RequestParam(name = "crs", required = false) String crs,
            @RequestParam(name = "properties", required = false) List<String> properties,
            @RequestParam(name = "exclude-properties", required = false) List<String> excludeProperties)
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
                properties,
                excludeProperties,
                itemId);
    }

    @GetMapping(path = "collections/{collectionId}/items", name = "getFeatures")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse items(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0") BigInteger startIndex,
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
            @RequestParam(name = "properties", required = false) List<String> properties,
            @RequestParam(name = "exclude-properties", required = false) List<String> excludeProperties,
            String itemId)
            throws Exception {

        WFSInfo wfs = getServiceInfo();
        FeatureConformance features = FeatureConformance.configuration(wfs);

        // build the request in a way core WFS machinery can understand it
        FeatureTypeInfo ft = getFeatureType(collectionId);
        GetFeatureRequest request = GetFeatureRequest.adapt(Wfs20Factory.eINSTANCE.createGetFeatureType());
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

        if (ids != null && !ids.isEmpty()) {
            // The ids parameter is part of the draft proposal "Query by IDs". The syntax and
            // semantic
            // of the parameter is subject to change in a future release. Its usage should be
            // carefully
            // considered.
            if (features.ids(wfs)) {
                filters.add(buildIdsFilter(ids));
            } else {
                LOGGER.warning(() -> "The ids parameter is not supported by the service, requires "
                        + FeatureConformance.IDS.getId()
                        + " conformance to be enabled.");
            }
        }
        if (filter != null) {
            CQL2Conformance cql2 = CQL2Conformance.configuration(wfs);
            ECQLConformance ecql = ECQLConformance.configuration(wfs);

            if (features.filter(wfs)) {
                if (APIFilterParser.ECQL_TEXT.equals(filterLanguage) && !ecql.text(wfs)) {
                    ignoreFilterLanguage(APIFilterParser.ECQL_TEXT, ECQLConformance.ECQL_TEXT);
                } else if (APIFilterParser.CQL2_TEXT.equals(filterLanguage) && !cql2.text(wfs)) {
                    ignoreFilterLanguage(APIFilterParser.CQL2_TEXT, CQL2Conformance.CQL2_TEXT);
                } else if (APIFilterParser.CQL2_JSON.equals(filterLanguage) && !cql2.json(wfs)) {
                    ignoreFilterLanguage(APIFilterParser.CQL2_TEXT, CQL2Conformance.CQL2_JSON);
                } else {
                    Filter parsedFilter = filterParser.parse(filter, filterLanguage, filterCRS);
                    filters.add(parsedFilter);
                }
            } else {
                LOGGER.warning(() -> "The filter parameter is not supported by the service, requires "
                        + FeatureConformance.FILTER.getId()
                        + " conformance to be enabled.");
            }
        }
        query.setFilter(mergeFiltersAnd(filters));
        if (sortBy != null) {
            if (features.sortBy(wfs)) {
                query.setSortBy(ImmutableList.copyOf(sortBy));
            } else {
                LOGGER.warning(() -> "The sortby parameter is not supported by the service, requires "
                        + FeatureConformance.SORTBY.getId()
                        + " conformance to be enabled.");
            }
        }

        if (properties != null || excludeProperties != null) {
            if (!features.propertySelection(wfs)) {
                LOGGER.warning(
                        () -> "The properties / exclude-properties parameter is not supported by the service, requires "
                                + FeatureConformance.PROPERTY_SELECTION.getId()
                                + " conformance to be enabled.");
            } else if (properties != null && excludeProperties != null) {
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "You cannot use both properties and exclude-properties in the same request",
                        HttpStatus.BAD_REQUEST);
            } else {
                if (properties != null && !properties.isEmpty()) {
                    query.setPropertyNames(properties);
                } else if (excludeProperties != null && !excludeProperties.isEmpty()) {
                    List<String> props = new ArrayList<>();
                    for (PropertyDescriptor pd : ft.getFeatureType().getDescriptors()) {
                        if (!excludeProperties.contains(pd.getName().getLocalPart())) {
                            props.add(pd.getName().getLocalPart());
                        }
                    }
                    query.setPropertyNames(props);
                }
            }
        }

        if (crs != null) {
            if (features.crsByReference(wfs)) {
                query.setSrsName(new URI(crs));
            } else {
                LOGGER.warning(() -> "The crs parameter is not supported by the service, requires "
                        + FeatureConformance.CRS_BY_REFERENCE.getId()
                        + " conformance to be enabled.");

                query.setSrsName(new URI("EPSG:4326"));
            }
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

    private static void ignoreFilterLanguage(String filterLanguage, APIConformance conformance) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning("The filter language '"
                    + filterLanguage
                    + "' is not supported by the service, requires "
                    + conformance.getId()
                    + " conformance to be enabled.");
        }
    }

    @PostMapping(path = "collections/{collectionId}/search", name = "searchFeatures")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse search(
            @PathVariable(name = "collectionId") String collectionId, @RequestBody APISearchQuery query)
            throws Exception {
        WFSInfo wfsInfo = getServiceInfo();
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        if (!featureServiceInfo.search(wfsInfo)) {
            throw new APIException(
                    APIException.NOT_FOUND, "Search is not supported by the service.", HttpStatus.NOT_FOUND);
        }
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
                null,
                null,
                null);
    }

    /** TODO: use DimensionInfo instead? It's used to return the time range in the collection */
    private Filter buildTimeFilter(FeatureTypeInfo ft, String time) throws ParseException, IOException {
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
            } else if (timeSpec instanceof DateRange dateRange) {
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
        FeatureId[] featureIds = ids.stream()
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

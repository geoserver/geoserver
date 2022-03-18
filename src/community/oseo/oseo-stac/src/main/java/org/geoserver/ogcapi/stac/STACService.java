/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

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
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_IDENTIFIER;
import static org.geoserver.opensearch.eo.store.OpenSearchQueries.getProductProperties;
import static org.geoserver.ows.URLMangler.URLType.RESOURCE;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.urlEncode;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionVisitor;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.DefaultContentType;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.JSONSchemaMessageConverter;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.ogcapi.PaginationLinksBuilder;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.Sortables;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.ServiceException;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Implementation of OGC Features API service */
@APIService(
        service = "STAC",
        version = "1.0",
        landingPage = "ogc/stac",
        serviceClass = OSEOInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/stac")
public class STACService {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    public static final String STAC_VERSION = "1.0.0";

    public static final String FEATURE_CORE =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core";
    public static final String FEATURE_HTML =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html";
    public static final String FEATURE_GEOJSON =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson";
    public static final String FEATURE_OAS30 =
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30";
    public static final String STAC_CORE = "https://api.stacspec.org/v1.0.0-beta.5/core";
    public static final String STAC_SEARCH = "https://api.stacspec.org/v1.0.0-beta.5/item-search";
    public static final String STAC_SEARCH_SORT =
            "https://api.stacspec.org/v1.0.0-beta.5/item-search#sort";
    public static final String STAC_SEARCH_FILTER =
            "https://api.stacspec.org/v1.0.0-beta.5/item-search#filter";
    public static final String STAC_FEATURES =
            "https://api.stacspec.org/spec/v1.0.0-beta.1/ogcapi-features";

    /** Container type: catalog */
    public static String TYPE_CATALOG = "Catalog";

    /** Container type: collection */
    public static String TYPE_COLLECTION = "Collection";

    private static final String DISPLAY_NAME = "SpatioTemporal Asset Catalog";

    private static final String FIELDS_PARAM = "fields";

    static final Logger LOGGER = Logging.getLogger(STACService.class);

    private final GeoServer geoServer;
    private final OpenSearchAccessProvider accessProvider;
    private final STACTemplates templates;
    private final SampleFeatures sampleFeatures;
    private TimeParser timeParser = new TimeParser();
    private final APIFilterParser filterParser;

    public STACService(
            GeoServer geoServer,
            OpenSearchAccessProvider accessProvider,
            STACTemplates templates,
            APIFilterParser filterParser,
            SampleFeatures sampleFeatures) {
        this.geoServer = geoServer;
        this.accessProvider = accessProvider;
        this.templates = templates;
        this.filterParser = filterParser;
        this.sampleFeatures = sampleFeatures;
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
        return new STACLandingPage(
                getService(), "ogc/stac", conformance().getConformsTo(), getCollectionIds());
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
                        STAC_CORE,
                        STAC_FEATURES,
                        STAC_SEARCH,
                        STAC_SEARCH_FILTER,
                        STAC_SEARCH_SORT,
                        FEATURES_FILTER,
                        FILTER,
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
                        CQL2_TEXT
                        /* CQL2_JSON very different from the binding we have */ );
        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(
            path = "api",
            name = "getApi",
            produces = {
                OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE,
                "application/x-yaml",
                MediaType.TEXT_XML_VALUE
            })
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() throws IOException {
        return new STACAPIBuilder(accessProvider).build(getService());
    }

    @GetMapping(path = "collections", name = "getCollections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsResponse collections() throws IOException {
        Query q = new Query();
        q.setFilter(getEnabledFilter());
        FeatureCollection<FeatureType, Feature> collections =
                accessProvider.getOpenSearchAccess().getCollectionSource().getFeatures(q);
        return new CollectionsResponse(collections);
    }

    @GetMapping(path = "collections/{collectionId}", name = "getCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionResponse collection(@PathVariable("collectionId") String collectionId)
            throws IOException {
        Feature collection = getCollection(collectionId);
        return new CollectionResponse(collection);
    }

    private Feature getCollection(String collectionId) throws IOException {
        return getCollection(collectionId, Query.ALL_PROPERTIES);
    }

    private Feature getCollection(String collectionId, List<PropertyName> selectedFields)
            throws IOException {
        Query q = new Query();
        q.setFilter(
                FF.and(
                        getEnabledFilter(),
                        FF.equals(FF.property(EO_IDENTIFIER), FF.literal(collectionId))));
        q.setProperties(selectedFields);
        FeatureCollection<FeatureType, Feature> collections =
                accessProvider.getOpenSearchAccess().getCollectionSource().getFeatures(q);
        Feature collection = DataUtilities.first(collections);
        if (collection == null)
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Collection not found: " + collectionId,
                    HttpStatus.NOT_FOUND);
        return collection;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getCollectionIds() throws IOException {
        Query q = new Query();
        q.setFilter(getEnabledFilter());
        FeatureCollection<FeatureType, Feature> collections =
                accessProvider.getOpenSearchAccess().getCollectionSource().getFeatures(q);
        FeatureType schema = collections.getSchema();
        // for unique visitor to work against complex features, full namespace has to be provided
        Name name = new NameImpl(schema.getName().getNamespaceURI(), "name");
        UniqueVisitor unique = new UniqueVisitor(FF.property(schema.getDescriptor(name).getName()));
        collections.accepts(unique, null);
        Set<Attribute> values = unique.getUnique();
        return values.stream().map(a -> (String) a.getValue()).collect(Collectors.toSet());
    }

    @GetMapping(path = "collections/{collectionId}/items/{itemId:.+}", name = "getItem")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public ItemResponse item(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "itemId") String itemId,
            @RequestParam(name = FIELDS_PARAM, required = false) String[] fields,
            HttpServletRequest request)
            throws Exception {
        // run just to check the collection exists, and throw an appropriate exception otherwise
        getCollection(collectionId);

        Query q = new Query();
        q.setFilter(
                FF.and(
                        getEnabledFilter(),
                        FF.equals(FF.property("identifier"), FF.literal(itemId))));

        FeatureSource<FeatureType, Feature> products =
                accessProvider.getOpenSearchAccess().getProductSource();
        boolean hasField = request.getParameterMap().containsKey(FIELDS_PARAM);
        RootBuilder rootBuilder = null;
        if (supportsFieldsSelection(request) && hasField) {
            rootBuilder = templates.getItemTemplate(collectionId);
            PropertySelectionVisitor propertySelectionVisitor =
                    new PropertySelectionVisitor(
                            new STACPropertySelection(fields), products.getSchema());
            rootBuilder = (RootBuilder) rootBuilder.accept(propertySelectionVisitor, null);
            q.setPropertyNames(new ArrayList<>(propertySelectionVisitor.getQueryProperties()));
        } else {
            q.setProperties(getProductProperties(accessProvider.getOpenSearchAccess()));
        }

        FeatureCollection<FeatureType, Feature> items = products.getFeatures(q);
        Feature item = DataUtilities.first(items);
        if (item == null) {
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Could not locate item " + itemId,
                    HttpStatus.NOT_FOUND);
        }
        ItemResponse response = new ItemResponse(collectionId, item);
        response.setTemplate(rootBuilder);
        return response;
    }

    @GetMapping(path = "collections/{collectionId}/items", name = "getItems")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public ItemsResponse items(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0") int startIndex,
            @RequestParam(name = "limit", required = false) Integer requestedLimit,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "datetime", required = false) String datetime,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "filter-lang", required = false) String filterLanguage,
            @RequestParam(name = "sortby", required = false) SortBy[] sortBy,
            @RequestParam(name = FIELDS_PARAM, required = false) String[] fields,
            HttpServletRequest request)
            throws Exception {

        // check the collection is enabled
        Feature collection = getCollection(collectionId);
        if (Boolean.FALSE.equals(collection.getProperty("enabled").getValue())) {
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Collection " + collectionId + " is not avialable",
                    HttpStatus.NOT_FOUND);
        }
        boolean hasFieldParam = request.getParameterMap().containsKey(FIELDS_PARAM);
        QueryResultBuilder resultBuilder =
                new QueryResultBuilder(templates, accessProvider, filterParser, sampleFeatures);
        resultBuilder
                .collectionIds(Arrays.asList(collectionId))
                .startIndex(startIndex)
                .requestedLimit(requestedLimit)
                .bbox(bbox)
                .datetime(datetime)
                .filter(filter)
                .filterLanguage(filterLanguage)
                .sortby(sortBy)
                .excludeDisabledCollection(false)
                .hasFieldParam(hasFieldParam)
                .fields(fields)
                .supportsFieldsSelection(supportsFieldsSelection(request));

        // query the items based on the request parameters
        QueryResult qr = resultBuilder.build();

        // build the links
        ItemsResponse response =
                new ItemsResponse(
                        collectionId, qr.getItems(), qr.getNumberMatched(), qr.getReturned());
        String path = "ogc/stac/collections/" + urlEncode(collectionId) + "/items";
        PaginationLinksBuilder linksBuilder =
                new PaginationLinksBuilder(
                        path,
                        startIndex,
                        qr.getQuery().getMaxFeatures(),
                        qr.getReturned(),
                        qr.getNumberMatched().longValue());
        response.setPrevious(linksBuilder.getPrevious());
        response.setNext(linksBuilder.getNext());
        response.setSelf(linksBuilder.getSelf());
        response.setTemplateMap(qr.getTemplateMap());
        return response;
    }

    @GetMapping(path = "search", name = "searchGet")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public SearchResponse searchGet(
            @RequestParam(name = "collections", required = false) List<String> collectionIds,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0") int startIndex,
            @RequestParam(name = "limit", required = false) Integer requestedLimit,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "intersects", required = false) String intersects,
            @RequestParam(name = "datetime", required = false) String datetime,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "filter-lang", required = false) String filterLanguage,
            @RequestParam(name = "sortby", required = false) SortBy[] sortBy,
            @RequestParam(name = FIELDS_PARAM, required = false) String[] fields,
            HttpServletRequest request)
            throws Exception {
        boolean hasFieldParam = request.getParameterMap().containsKey(FIELDS_PARAM);
        QueryResultBuilder resultBuilder =
                new QueryResultBuilder(templates, accessProvider, filterParser, sampleFeatures);
        resultBuilder
                .collectionIds(collectionIds)
                .startIndex(startIndex)
                .requestedLimit(requestedLimit)
                .bbox(bbox)
                .intersects(intersects)
                .datetime(datetime)
                .filter(filter)
                .filterLanguage(filterLanguage)
                .sortby(sortBy)
                .excludeDisabledCollection(true)
                .hasFieldParam(hasFieldParam)
                .fields(fields)
                .supportsFieldsSelection(supportsFieldsSelection(request));

        // query the items based on the request parameters
        QueryResult qr = resultBuilder.build();
        // query the items based on the request parameters

        // build the links
        SearchResponse response =
                new SearchResponse(qr.getItems(), qr.getNumberMatched(), qr.getReturned());
        response.setTemplateMap(qr.getTemplateMap());
        String path = "ogc/stac/search";
        PaginationLinksBuilder linksBuilder =
                new PaginationLinksBuilder(
                        path,
                        startIndex,
                        qr.getQuery().getMaxFeatures(),
                        qr.getReturned(),
                        qr.getNumberMatched().longValue());
        response.setPrevious(linksBuilder.getPrevious());
        response.setNext(linksBuilder.getNext());
        response.setSelf(linksBuilder.getSelf());
        response.setTemplateMap(qr.getTemplateMap());
        return response;
    }

    @PostMapping(path = "search", name = "searchPost")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public SearchResponse searchPost(@RequestBody SearchQuery sq) throws Exception {

        QueryResultBuilder resultBuilder =
                new QueryResultBuilder(templates, accessProvider, filterParser, sampleFeatures);
        resultBuilder
                .collectionIds(sq.getCollections())
                .intersects(sq.getIntersection())
                .startIndex(sq.getStartIndex())
                .requestedLimit(sq.getLimit())
                .bbox(sq.getBbox())
                .datetime(sq.getDatetime())
                .filter(sq.getFilter())
                .filterLanguage(sq.getFilterLang())
                .excludeDisabledCollection(true);

        // query the items based on the request parameters
        QueryResult qr = resultBuilder.build();

        // build the links
        SearchResponse response =
                new SearchResponse(qr.getItems(), qr.getNumberMatched(), qr.getReturned());
        String path = "ogc/stac/search";
        PaginationLinksBuilder linksBuilder =
                new PaginationLinksBuilder(
                        path,
                        Optional.ofNullable(sq.getStartIndex()).orElse(0),
                        qr.getQuery().getMaxFeatures(),
                        qr.getReturned(),
                        qr.getNumberMatched().longValue());
        response.setSelf(linksBuilder.getSelf());
        response.setPost(true);
        response.setPreviousBody(linksBuilder.getPreviousMap(false));
        response.setNextBody(linksBuilder.getNextMap(false));

        return response;
    }

    public PropertyIsEqualTo getEnabledFilter() {
        return FF.equals(FF.property(OpenSearchAccess.ENABLED), FF.literal(true));
    }

    static Filter getCollectionsFilter(List<String> collectionIds) {
        FilterMerger filters = new FilterMerger();
        collectionIds.stream()
                .map(id -> FF.equals(FF.property("parentIdentifier"), FF.literal(id)))
                .forEach(f -> filters.add(f));
        return filters.or();
    }

    @GetMapping(
            path = "collections/{collectionId}/queryables",
            name = "getCollectionQueryables",
            produces = JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE)
    @ResponseBody
    @HTMLResponseBody(templateName = "queryables-collection.ftl", fileName = "queryables.html")
    public Queryables collectionQueryables(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        // check the collection is there
        getCollection(collectionId);
        String id =
                buildURL(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/stac/collections/" + urlEncode(collectionId) + "/queryables",
                        null,
                        RESOURCE);
        Queryables queryables =
                new STACQueryablesBuilder(
                                id,
                                templates.getItemTemplate(collectionId),
                                sampleFeatures.getSchema(),
                                sampleFeatures.getSample(collectionId))
                        .getQueryables();
        queryables.setCollectionId(collectionId);
        return queryables;
    }

    @GetMapping(
            path = "collections/{collectionId}/sortables",
            name = "getCollectionSortables",
            produces = JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE)
    @ResponseBody
    @HTMLResponseBody(templateName = "sortables-collection.ftl", fileName = "sortables.html")
    public Sortables collectionSortables(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        // check the collection is there
        getCollection(collectionId);
        String id =
                buildURL(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/stac/collections/" + urlEncode(collectionId) + "/sortables",
                        null,
                        RESOURCE);
        FeatureType itemsSchema =
                accessProvider.getOpenSearchAccess().getProductSource().getSchema();
        RootBuilder template = this.templates.getItemTemplate(collectionId);
        Sortables sortables = new STACSortablesMapper(template, itemsSchema, id).getSortables();
        sortables.setCollectionId(collectionId);
        return sortables;
    }

    @GetMapping(
            path = "queryables",
            name = "getSearchQueryables",
            produces = JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE)
    @ResponseBody
    @HTMLResponseBody(templateName = "queryables-global.ftl", fileName = "queryables.html")
    public Queryables searchQueryables() throws IOException {
        String baseURL = APIRequestInfo.get().getBaseURL();
        String id = buildURL(baseURL, "ogc/stac/queryables", null, RESOURCE);
        LOGGER.severe(
                "Should consider the various collection specific templates here, and decide what to do for queriables that are in one collection but not in others (replace with null and simplify filter?)");
        Queryables queryables =
                new STACQueryablesBuilder(
                                id,
                                templates.getItemTemplate(null),
                                sampleFeatures.getSchema(),
                                sampleFeatures.getSample(null))
                        .getQueryables();
        return queryables;
    }

    @GetMapping(
            path = "sortables",
            name = "getSearchSortables",
            produces = JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE)
    @ResponseBody
    @HTMLResponseBody(templateName = "sortables-global.ftl", fileName = "sortables.html")
    public Sortables searchSortables() throws IOException {
        String baseURL = APIRequestInfo.get().getBaseURL();
        String id = buildURL(baseURL, "ogc/stac/sortables", null, RESOURCE);
        LOGGER.severe(
                "Should consider the various collection specific templates here, and decide what to do for sortables that are in one collection but not in others (replace with null and simplify filter?)");
        FeatureType itemsSchema =
                accessProvider.getOpenSearchAccess().getProductSource().getSchema();
        RootBuilder template = this.templates.getItemTemplate(null);
        Sortables sortables = new STACSortablesMapper(template, itemsSchema, id).getSortables();
        return sortables;
    }

    private boolean supportsFieldsSelection(HttpServletRequest request) {
        return getMediaTypeOrDefault(request).equals(OGCAPIMediaTypes.GEOJSON_VALUE);
    }

    private String getMediaTypeOrDefault(HttpServletRequest request) {
        String mediaType = request.getParameter("f");
        if (mediaType == null) mediaType = request.getHeader(HttpHeaders.ACCEPT);
        if (mediaType == null) mediaType = OGCAPIMediaTypes.GEOJSON_VALUE;
        return mediaType;
    }
}

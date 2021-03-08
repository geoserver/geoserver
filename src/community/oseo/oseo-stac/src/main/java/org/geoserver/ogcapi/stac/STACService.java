/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIBBoxParser;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.DefaultContentType;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.ServiceException;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

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
    private TimeParser timeParser = new TimeParser();
    private final APIFilterParser filterParser;

    public STACService(
            GeoServer geoServer,
            OpenSearchAccessProvider accessProvider,
            APIFilterParser filterParser) {
        this.geoServer = geoServer;
        this.accessProvider = accessProvider;
        this.filterParser = filterParser;
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

    @GetMapping(path = "collections", name = "getCollections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsResponse collections() throws IOException {
        FeatureCollection<FeatureType, Feature> collections =
                accessProvider.getOpenSearchAccess().getCollectionSource().getFeatures();
        return new CollectionsResponse(collections);
    }

    @GetMapping(path = "collections/{collectionId}", name = "getCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionResponse collection(@PathVariable("collectionId") String collectionId)
            throws IOException {
        Query q = new Query();
        q.setFilter(FF.equals(FF.property("name"), FF.literal(collectionId)));
        FeatureCollection<FeatureType, Feature> collections =
                accessProvider.getOpenSearchAccess().getCollectionSource().getFeatures(q);
        Feature collection = DataUtilities.first(collections);
        return new CollectionResponse(collection);
    }

    @GetMapping(path = "collections/{collectionId}/items/{itemId:.+}", name = "getItem")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public ItemResponse item(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "itemId") String itemId)
            throws Exception {
        Query q = new Query();
        q.setFilter(FF.equals(FF.property("identifier"), FF.literal(itemId)));

        FeatureSource<FeatureType, Feature> products =
                accessProvider.getOpenSearchAccess().getProductSource();
        FeatureCollection<FeatureType, Feature> items = products.getFeatures(q);
        Feature item = DataUtilities.first(items);
        if (item == null) {
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Could not locate item " + itemId,
                    HttpStatus.NOT_FOUND);
        }
        return new ItemResponse(collectionId, item);
    }

    @GetMapping(path = "collections/{collectionId}/items", name = "getItems")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public ItemsResponse items(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "datetime", required = false) String datetime,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "filter-lang", required = false) String filterLanguage)
            throws Exception {
        List<Filter> filters = new ArrayList<>();
        filters.add(FF.equals(FF.property("parentIdentifier"), FF.literal(collectionId)));
        if (bbox != null) {
            filters.add(APIBBoxParser.toFilter(bbox, DefaultGeographicCRS.WGS84));
        }
        if (datetime != null) {
            filters.add(buildTimeFilter(datetime));
        }
        if (filter != null) {
            Filter parsedFilter = filterParser.parse(filter, filterLanguage);
            filters.add(parsedFilter);
        }
        Query q = new Query();
        if (startIndex != null) q.setStartIndex(startIndex.intValue());
        if (limit != null) q.setMaxFeatures(limit.intValue());
        q.setFilter(mergeFiltersAnd(filters));

        FeatureSource<FeatureType, Feature> source =
                accessProvider.getOpenSearchAccess().getProductSource();
        FeatureCollection<FeatureType, Feature> items = source.getFeatures(q);
        return new ItemsResponse(collectionId, items);
    }

    private Filter buildTimeFilter(String time) throws ParseException, IOException {
        Collection times = timeParser.parse(time);
        if (times.isEmpty() || times.size() > 1) {
            throw new ServiceException(
                    "Invalid time specification, must be a single time, or a time range",
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "time");
        }

        Object timeSpec = times.iterator().next();

        if (timeSpec instanceof Date) {
            // range containment
            return FF.between(
                    FF.literal(timeSpec), FF.property("timeStart"), FF.property("timeEnd"));
        } else if (timeSpec instanceof DateRange) {
            // range overlap filter
            DateRange dateRange = (DateRange) timeSpec;
            Literal before = FF.literal(dateRange.getMinValue());
            Literal after = FF.literal(dateRange.getMaxValue());
            Filter lower = FF.lessOrEqual(FF.property("timeStart"), after);
            Filter upper = FF.greaterOrEqual(FF.property("timeEnd"), before);
            return FF.and(lower, upper);
        } else {
            throw new IllegalArgumentException("Cannot build time filter out of " + timeSpec);
        }
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
}

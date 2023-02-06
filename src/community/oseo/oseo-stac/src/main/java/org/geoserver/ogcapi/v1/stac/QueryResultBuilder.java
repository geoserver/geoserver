/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_IDENTIFIER;
import static org.geoserver.opensearch.eo.store.OpenSearchQueries.getProductProperties;

import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionVisitor;
import org.geoserver.ogcapi.APIBBoxParser;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.DateRange;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.FactoryException;

/**
 * A builder for a QueryResult object. It provides methods to set the various fields that can be
 * used to query a source and return the appropriate {@link QueryResult};
 */
class QueryResultBuilder {

    private final OpenSearchAccessProvider accessProvider;
    private final STACTemplates templates;
    private List<String> collectionIds;
    private Integer startIndex;
    private Integer requestedLimit;
    private Filter bboxFilter;
    private Geometry intersects;
    private String datetime;
    private String filter;
    private String filterLanguage;
    private SortBy[] sortby;
    private boolean excludeDisabledCollection;
    private boolean hasFieldParam;
    private String[] fields;
    private boolean supportsFieldsSelection;
    private APIFilterParser filterParser;

    private static final String EO_COLLECTIION = "eo:collection";

    private TimeParser timeParser = new TimeParser();

    private SampleFeatures sampleFeatures;

    private CollectionsCache collectionsCache;

    private GeoServer geoServer;

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    static final String DEF_TEMPLATE = "DEFAULT";

    QueryResultBuilder(
            STACTemplates templates,
            OpenSearchAccessProvider provider,
            APIFilterParser filterParser,
            SampleFeatures sampleFeatures,
            CollectionsCache collectionsCache) {
        this.templates = templates;
        this.accessProvider = provider;
        this.filterParser = filterParser;
        this.sampleFeatures = sampleFeatures;
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
        this.collectionsCache = collectionsCache;
    }

    /**
     * Set the collection ids to the builder.
     *
     * @param collectionIds
     * @return the builder.
     */
    QueryResultBuilder collectionIds(List<String> collectionIds) {
        this.collectionIds = collectionIds;
        return this;
    }

    /**
     * Set the index value to the builder.
     *
     * @param startIndex the index value.
     * @return the builder.
     */
    QueryResultBuilder startIndex(Integer startIndex) {
        this.startIndex = startIndex;
        return this;
    }

    /**
     * Set the limit value to the builder.
     *
     * @param requestedLimit the limit value.
     * @return the builder.
     */
    QueryResultBuilder requestedLimit(Integer requestedLimit) {
        this.requestedLimit = requestedLimit;
        return this;
    }

    /**
     * Set the bbox to use to filter data.
     *
     * @param bbox the bbox as a string.
     * @return the builder.
     * @throws FactoryException
     */
    QueryResultBuilder bbox(String bbox) throws FactoryException {
        if (bbox != null)
            this.bboxFilter = APIBBoxParser.toFilter(bbox, DefaultGeographicCRS.WGS84);
        return this;
    }

    /**
     * Set the bbox to use to filter data.
     *
     * @param bbox the bbox as an array of double.
     * @return the builder.
     * @throws FactoryException
     */
    QueryResultBuilder bbox(double[] bbox) throws FactoryException {
        if (bbox != null)
            this.bboxFilter = APIBBoxParser.toFilter(bbox, DefaultGeographicCRS.WGS84);
        return this;
    }

    /**
     * Set the geometry to use for an intersects filter.
     *
     * @param intersects the geometry to use for the intersects filter as a string.
     * @return the builder.
     */
    QueryResultBuilder intersects(String intersects) {
        if (intersects != null) this.intersects = GeoJSONReader.parseGeometry(intersects);
        return this;
    }

    /**
     * Set the geometry to use for an intersects filter.
     *
     * @param intersection the geometry to use for the intersects filter.
     * @return the builder.
     */
    QueryResultBuilder intersects(Geometry intersection) {
        this.intersects = intersection;
        return this;
    }

    /**
     * Set the datetime to the builder.
     *
     * @param datetime the datetime to filter data.
     * @return the builder.
     */
    QueryResultBuilder datetime(String datetime) {
        this.datetime = datetime;
        return this;
    }

    /**
     * Set a filter to the builder.
     *
     * @param filter the filter.
     * @return the builder.
     */
    QueryResultBuilder filter(String filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Set the filter language to the builder.
     *
     * @param filterLanguage
     * @return the builder.
     */
    QueryResultBuilder filterLanguage(String filterLanguage) {
        this.filterLanguage = filterLanguage;
        return this;
    }

    /**
     * Set the SortBy fields to the builder.
     *
     * @param sortby the sortBy fields.
     * @return the builder.
     */
    QueryResultBuilder sortby(SortBy[] sortby) {
        this.sortby = sortby;
        return this;
    }

    /**
     * Set a flag to tell the builder to exclude a collection from the result when disabled.
     *
     * @param excludeDisabledCollection true to disable excluded collections, false otherwise.
     * @return the builder.
     */
    QueryResultBuilder excludeDisabledCollection(boolean excludeDisabledCollection) {
        this.excludeDisabledCollection = excludeDisabledCollection;
        return this;
    }

    /**
     * Set a flag to tell the builder if the field param was specified or not. This is necessary
     * since the behaviour changes according to the emptyness of the parameter or its absence.
     *
     * @param hasFieldParam true if has, false otherwise.
     * @return the builder.
     */
    QueryResultBuilder hasFieldParam(boolean hasFieldParam) {
        this.hasFieldParam = hasFieldParam;
        return this;
    }

    /**
     * Set the included/excluded fields.
     *
     * @param fields An array of strings holding the value of the fields request param.
     * @return the builder.
     */
    QueryResultBuilder fields(String[] fields) {
        this.fields = fields;
        return this;
    }

    /**
     * Set the supportsFieldsSelection flag. Currently only GET methods search and items and
     * endpoints for the geo+json are supporting it.
     *
     * @param supportsFieldsSelection true if supports, false otherwise.
     * @return the builder.
     */
    QueryResultBuilder supportsFieldsSelection(boolean supportsFieldsSelection) {
        this.supportsFieldsSelection = supportsFieldsSelection;
        return this;
    }

    /**
     * Produce a QueryResult using the various query fields set.
     *
     * @return {@link QueryResult}.
     * @throws IOException
     * @throws ParseException
     * @throws FactoryException
     */
    QueryResult build() throws IOException, ParseException {
        // request parsing
        FilterMerger filters = new FilterMerger();

        addCollectionsFilter(filters, collectionIds, excludeDisabledCollection);
        if (bboxFilter != null) {
            filters.add(bboxFilter);
        }
        if (intersects != null) {
            filters.add(FF.intersects(FF.property(""), FF.literal(intersects)));
        }
        if (datetime != null) {
            filters.add(buildTimeFilter(datetime));
        }
        if (filter != null) {
            Filter mapped = parseFilter(collectionIds, filter, filterLanguage);
            filters.add(mapped);
        }
        // keep only enabled products
        filters.add(getEnabledFilter());

        Query q = new Query();
        q.setStartIndex(startIndex);
        int limit = getLimit(requestedLimit);
        q.setMaxFeatures(limit);
        q.setFilter(filters.and());
        q.setSortBy(mapSortProperties(collectionIds, sortby));
        FeatureSource<FeatureType, Feature> source =
                accessProvider.getOpenSearchAccess().getProductSource();
        Map<String, RootBuilder> builderMap = new HashMap<>();
        if (supportsFieldsSelection && hasFieldParam) {
            Set<String> properties = new HashSet<>();
            populateTemplateMapAndProperties(builderMap, properties, source);
            List<String> props = new ArrayList<>();
            for (String p : properties) {
                if (p.startsWith(EO_COLLECTIION)) props.addAll(mapComplexPathToProperties(p));
                else props.add(p);
            }
            q.setPropertyNames(props);
        } else {
            List<PropertyName> queryProps =
                    getProductProperties(accessProvider.getOpenSearchAccess());
            q.setProperties(queryProps);
        }
        QueryResult result = queryItems(source, q);
        if (supportsFieldsSelection && !builderMap.isEmpty()) {
            result.setTemplateMap(builderMap);
        }
        return result;
    }

    private void populateTemplateMapAndProperties(
            Map<String, RootBuilder> builderMap,
            Set<String> properties,
            FeatureSource<FeatureType, Feature> source)
            throws IOException {
        RootBuilder rootBuilder;
        String key = DEF_TEMPLATE;
        if (collectionIds == null || collectionIds.isEmpty() || collectionIds.size() > 1) {
            rootBuilder = templates.getItemTemplate(null);
        } else {
            key = collectionIds.get(0);
            rootBuilder = templates.getItemTemplate(key);
        }
        if (rootBuilder != null) {
            if (hasFieldParam) {
                PropertySelectionVisitor selectionVisitor =
                        new PropertySelectionVisitor(
                                new STACPropertySelection(fields), source.getSchema());
                rootBuilder = (RootBuilder) rootBuilder.accept(selectionVisitor, null);
                Set<String> props = selectionVisitor.getQueryProperties();
                props.add("parentIdentifier");
                builderMap.put(key, rootBuilder);
                properties.addAll(props);
            }
        }
    }
    /**
     * Returns an actual limit based on the
     *
     * @param requestedLimit
     * @return the builder.
     */
    private int getLimit(Integer requestedLimit) {
        OSEOInfo oseo = getService();
        int serviceMax = oseo.getMaximumRecordsPerPage();
        if (requestedLimit == null) return oseo.getRecordsPerPage();
        return Math.min(serviceMax, requestedLimit);
    }

    /**
     * Get the OSEOInfo service instance.
     *
     * @return the OSEOInfo service instance.
     */
    OSEOInfo getService() {
        return geoServer.getService(OSEOInfo.class);
    }

    private QueryResult queryItems(FeatureSource<FeatureType, Feature> source, Query q)
            throws IOException {
        // get the items
        FeatureCollection<FeatureType, Feature> items = source.getFeatures(q);

        // the counts
        Query matchedQuery = new Query(q);
        matchedQuery.setMaxFeatures(-1);
        matchedQuery.setStartIndex(0);
        int matched = source.getCount(matchedQuery);
        int returned = items.size();

        return new QueryResult(q, items, BigInteger.valueOf(matched), returned);
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

    private SortBy[] mapSortProperties(List<String> collectionIds, SortBy[] sortby)
            throws IOException {
        // nothing to map, easy way out
        if (sortby == null) return null;

        // do we map for a specific collection, or have to deal with multiple ones?
        FeatureType itemsSchema =
                accessProvider.getOpenSearchAccess().getProductSource().getSchema();
        TemplateBuilder builder;
        STACSortablesMapper mapper = null;
        STACQueryablesBuilder stacQueryablesBuilder = null;
        String collectionId = null;
        if (collectionIds != null && !collectionIds.isEmpty()) {
            // right now assuming multiple collections means using search, where the
            // sortables are generic
            collectionId = collectionIds.get(0);
        }
        mapper =
                STACSortablesMapper.getSortablesMapper(
                        collectionId,
                        templates,
                        sampleFeatures,
                        collectionsCache,
                        itemsSchema,
                        geoServer);

        return mapper.map(sortby);
    }

    private void addCollectionsFilter(
            FilterMerger filters, List<String> collectionIds, boolean excludeDisabledCollection)
            throws IOException {
        List<String> disabledIds =
                excludeDisabledCollection
                        ? getDisabledCollections(collectionIds)
                        : Collections.emptyList();

        if (collectionIds != null && !collectionIds.isEmpty()) {
            collectionIds.removeAll(disabledIds);
            filters.add(STACService.getCollectionsFilter(collectionIds));
        } else if (!disabledIds.isEmpty()) {
            // exclude disabled collections
            filters.add(FF.not(STACService.getCollectionsFilter(disabledIds)));
        }
    }

    private List<String> getDisabledCollections(List<String> collectionIds) throws IOException {
        Query q = new Query();
        Filter filter = FF.equals(FF.property(OpenSearchAccess.ENABLED), FF.literal(false));
        if (collectionIds != null && !collectionIds.isEmpty()) {
            List<Filter> filters = new ArrayList<>();
            filters.add(filter);

            filters.addAll(
                    collectionIds.stream()
                            .map(cid -> FF.equals(FF.property(EO_IDENTIFIER), FF.literal(cid)))
                            .collect(Collectors.toList()));
            filter = FF.and(filters);
        }
        q.setFilter(filter);
        q.setProperties(Arrays.asList(FF.property(EO_IDENTIFIER)));
        FeatureCollection<FeatureType, Feature> collections =
                accessProvider.getOpenSearchAccess().getCollectionSource().getFeatures(q);
        return DataUtilities.list(collections).stream()
                .map(f -> (String) f.getProperty(EO_IDENTIFIER).getValue())
                .collect(Collectors.toList());
    }

    PropertyIsEqualTo getEnabledFilter() {
        return FF.equals(FF.property(OpenSearchAccess.ENABLED), FF.literal(true));
    }

    Filter parseFilter(List<String> collectionIds, String filter, String filterLang)
            throws IOException {
        Filter parsed = filterParser.parse(filter, filterLang);
        return new TemplatePropertyMapper(
                        templates,
                        sampleFeatures,
                        collectionsCache,
                        geoServer.getService(OSEOInfo.class))
                .mapProperties(collectionIds, parsed);
    }

    /**
     * Map a complexPath to a List of separate property names suitable to be consumend by the
     * OpenSearch Join mechanism.
     *
     * @param complexPath the complex path.
     * @return the List of property names suitable to be added to the Query properties.
     * @throws IOException
     */
    private List<String> mapComplexPathToProperties(String complexPath) throws IOException {
        List<String> props = new ArrayList<>();
        String[] parts;
        if (complexPath.indexOf("/") != -1) parts = complexPath.split("/");
        else parts = complexPath.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[0];
            String[] partSplitted = part.split(":");
            String localName;
            if (partSplitted.length > 1) localName = partSplitted[1];
            else localName = partSplitted[0];
            if (i != (parts.length - 1)) {
                sb.append(localName).append(".");
                props.add(localName);
            }
        }
        props.add(sb.toString());
        return props;
    }
}

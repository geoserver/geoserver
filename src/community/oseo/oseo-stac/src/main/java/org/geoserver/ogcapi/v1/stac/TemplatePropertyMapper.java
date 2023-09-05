/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.util.logging.Logging;

class TemplatePropertyMapper {

    static final Logger LOGGER = Logging.getLogger(TemplatePropertyMapper.class);
    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    private final STACTemplates templates;
    private final SampleFeatures sampleFeatures;
    private final CollectionsCache collectionsCache;
    private final OSEOInfo oseoInfo;

    public TemplatePropertyMapper(
            STACTemplates templates,
            SampleFeatures sampleFeatures,
            CollectionsCache collectionsCache,
            OSEOInfo oseoInfo) {
        this.templates = templates;
        this.sampleFeatures = sampleFeatures;
        this.collectionsCache = collectionsCache;
        this.oseoInfo = oseoInfo;
    }

    /**
     * Back maps STAC properties used in the filter to the underlying data model, using expressions
     * found in the JSON template.
     */
    public Filter mapProperties(List<String> collectionIds, Filter filter) throws IOException {
        Filter result = mapPropertiesInternal(collectionIds, filter);
        // result might contain duplications and null properties, improve it
        return SimplifyingFilterVisitor.simplify(result);
    }

    private Filter mapPropertiesInternal(List<String> collectionIds, Filter filter)
            throws IOException {
        // do we have custom templates, and if so, for which collections?
        Set<String> customTemplateCollections = templates.getCustomItemTemplates();
        if (customTemplateCollections.isEmpty()) {
            return mapFilter(filter, templates.getItemTemplate(null), null);
        }

        // if we have to go blind, we'll use all collections with a custom template, plus
        // one for the default template, mapped to null
        if (collectionIds == null || collectionIds.isEmpty()) {
            collectionIds = new ArrayList<>(customTemplateCollections);
            collectionIds.add(null); // default template
        }

        Map<Filter, List<String>> collectionFilters = mapFilters(collectionIds, filter);

        // if only one filter came out, it can be used as is, otherwise merge them
        if (collectionFilters.size() == 1) {
            return collectionFilters.keySet().iterator().next();
        }
        List<Filter> filters = mergeFilters(collectionFilters);

        if (filters.size() == 1) return filters.get(0);
        else return FF.or(filters);
    }

    /**
     * Handles different filters for different collections, plus at least one case that contains the
     * default collection, which can be handled as a default
     */
    private List<Filter> mergeFilters(Map<Filter, List<String>> collectionFilters) {
        Filter defaultFilter = null;
        List<String> customFilterCollections = new ArrayList<>();
        List<Filter> filters = new ArrayList<>();
        for (Map.Entry<Filter, List<String>> entry : collectionFilters.entrySet()) {
            Filter filter = entry.getKey();
            List<String> collections = entry.getValue();
            if (collections.contains((String) null)) {
                // these use the default filter, will have to be encoded at the end
                defaultFilter = filter;
            } else {
                Filter collectionFilter = getCollectionsFilter(collections);
                customFilterCollections.addAll(collections);
                filters.add(FF.and(collectionFilter, filter));
            }
        }

        if (defaultFilter != null) {
            Filter collectionFilter = getCollectionsFilter(customFilterCollections);
            filters.add(FF.and(FF.not(collectionFilter), defaultFilter));
        }
        return filters;
    }

    /**
     * Grab the builder for each collection (might be different) and back-map collectionFilters on
     * it
     */
    private Map<Filter, List<String>> mapFilters(List<String> collectionIds, Filter result)
            throws IOException {
        Map<RootBuilder, Filter> filtersCache = new LinkedHashMap<>();
        Map<Filter, List<String>> collectionFilters = new LinkedHashMap<>();
        for (String collectionId : collectionIds) {
            RootBuilder template = templates.getItemTemplate(collectionId);
            Filter filter =
                    filtersCache.computeIfAbsent(template, t -> mapFilter(result, t, collectionId));
            List<String> filterCollections =
                    collectionFilters.computeIfAbsent(filter, c -> new ArrayList<>());
            filterCollections.add(collectionId);
        }
        return collectionFilters;
    }

    private Filter mapFilter(Filter source, RootBuilder t, String collectionId) {
        try {
            STACQueryablesBuilder builder =
                    new STACQueryablesBuilder(
                            null,
                            t,
                            sampleFeatures.getSchema(),
                            sampleFeatures.getSample(collectionId),
                            collectionsCache.getCollection(collectionId),
                            oseoInfo);
            Map<String, Expression> expressions = builder.getExpressionMap();
            Set<String> queryables = builder.getQueryables().getProperties().keySet();
            Set<String> notIncluded = new HashSet<>();
            STACPathVisitor visitor = new STACPathVisitor(expressions, queryables, notIncluded);
            Filter out = (Filter) source.accept(visitor, null);
            if (!notIncluded.isEmpty()) {
                LOGGER.fine(
                        "Filter includes attribute not found in queryables configuration: "
                                + notIncluded);
            }
            return out;
        } catch (IOException e) {
            throw new RuntimeException("Failed to map filter back to source data", e);
        }
    }

    private Filter getCollectionsFilter(List<String> collectionIds) {
        FilterMerger filters = new FilterMerger();
        collectionIds.stream()
                .map(id -> FF.equals(FF.property("parentIdentifier"), FF.literal(id)))
                .forEach(f -> filters.add(f));
        return filters.or();
    }
}

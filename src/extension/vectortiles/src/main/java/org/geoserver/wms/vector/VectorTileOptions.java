/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static org.geotools.api.style.FeatureTypeStyle.VT_ATTRIBUTES;
import static org.geotools.api.style.FeatureTypeStyle.VT_COALESCE;
import static org.geotools.api.style.FeatureTypeStyle.VT_LABELS;
import static org.geotools.api.style.FeatureTypeStyle.VT_LABEL_ATTRIBUTES;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.LiteFeatureTypeStyle;
import org.geoserver.wms.map.StyleQueryUtil;
import org.geotools.api.data.Query;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.api.style.Rule;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.Filters;
import org.geotools.filter.SortByImpl;
import org.geotools.map.Layer;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/** Collects from the style the vector tiles vendor options, helps apply them */
class VectorTileOptions {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private final GeometryDescriptor geometryDescriptor;
    private final FeatureType schema;
    List<String> attributes;
    boolean labelLayer;
    List<String> labelAttributes;
    private boolean coalesceEnabled;

    /**
     * Builds a new {@link VectorTileOptions} object, based on layer and map contents
     *
     * @param layer
     * @param mapContent
     */
    VectorTileOptions(Layer layer, WMSMapContent mapContent) {
        List<LiteFeatureTypeStyle> liteFeatureStyles = StyleQueryUtil.getLiteFeatureStyles(layer, mapContent);
        for (LiteFeatureTypeStyle lft : liteFeatureStyles) {
            if (lft.options == null) continue;
            collectVectorTileOptions(lft.options);
            for (Rule rule : lft.ruleList) {
                collectVectorTileOptions(rule.getOptions());
            }
        }
        this.schema = layer.getFeatureSource().getSchema();
        this.geometryDescriptor = schema.getGeometryDescriptor();
    }

    /** Collects vector tile options from the options map */
    private void collectVectorTileOptions(Map<String, String> options) {
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (VT_ATTRIBUTES.equals(key)) attributes = toStringList(value);
            if (VT_LABELS.equals(key)) labelLayer = Boolean.valueOf(value);
            if (VT_LABEL_ATTRIBUTES.equals(key)) labelAttributes = toStringList(value);
            if (VT_COALESCE.equals(key)) coalesceEnabled = Boolean.valueOf(value);
        }
    }

    /**
     * Will return true for polygons and multi-polygons, to indicate that the label point must be generated (the
     * original geometry won't do)
     *
     * @return
     */
    boolean isPolygonLabelEnabled() {
        Class<?> binding = geometryDescriptor.getType().getBinding();
        return Polygon.class.isAssignableFrom(binding) || MultiPolygon.class.isAssignableFrom(binding);
    }

    /** Parses the value as a comma separated list of values and returns them as a mutable list of {@link String} */
    private List<String> toStringList(String value) {
        return new ArrayList<>(Arrays.asList(value.split("\\s*,\\s*")));
    }

    /** The list of attributes included in the vector tile */
    List<String> getAttributes() {
        return attributes;
    }

    /**
     * Whether to generate a separate label layer, or not. Mandatory for polygons, potentially useful for labels and
     * points if a separate list of attributes is chosen and a (small) subset of the features needs to be labelled
     */
    boolean generateLabelLayer() {
        return labelLayer;
    }

    /** Set of attributes for the label layer */
    List<String> getLabelAttributes() {
        return labelAttributes;
    }

    /** Customizes the main features query based on the vector tile options. */
    void customizeQuery(Query query) {
        customizeQueryAttributes(query, attributes);
        // sort by attributes to find out the ones sharing the same value
        if (coalesceEnabled) {
            SortBy[] sortBy = query.getSortBy();
            SortBy[] merged = getCoalesceSortBy(attributes, sortBy);
            query.setSortBy(merged);
        }
    }

    private SortBy[] getCoalesceSortBy(List<String> attributes, SortBy[] sortBy) {
        Set<PropertyName> sortAttributes;
        if (attributes == null) {
            sortAttributes = schema.getDescriptors().stream()
                    .filter(pd -> !(pd instanceof GeometryDescriptor))
                    .map(pd -> FF.property(pd.getName()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            sortAttributes = attributes.stream()
                    .filter(att -> !(schema.getDescriptor(att) instanceof GeometryDescriptor))
                    .map(FF::property)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        // remove the sorts we already have
        if (sortBy != null) {
            for (SortBy sort : sortBy) {
                sortAttributes.remove(sort.getPropertyName());
            }
        }

        Stream<SortBy> attributesSort = sortAttributes.stream().map(p -> new SortByImpl(p, SortOrder.ASCENDING));

        if (sortBy == null) {
            return attributesSort.toArray(n -> new SortBy[n]);
        } else {
            return Streams.concat(Arrays.stream(sortBy), attributesSort).toArray(n -> new SortBy[n]);
        }
    }

    /** Customizes the label features query based on the vector tile options. */
    Query customizeLabelQuery(Query query) {
        if (labelAttributes != null) {
            customizeQueryAttributes(query, labelAttributes);
            Filter nonNullValues = getNonNullFilter(labelAttributes);
            query.setFilter(Filters.and(FF, query.getFilter(), nonNullValues));
        }

        return query;
    }

    private Filter getNonNullFilter(List<String> attributes) {
        if (attributes.size() == 1) return FF.not(FF.isNull(FF.property(attributes.get(0))));

        List<Filter> filters = attributes.stream()
                .map(att -> FF.not(FF.isNull(FF.property(att))))
                .collect(Collectors.toList());
        return FF.and(filters);
    }

    private void customizeQueryAttributes(Query query, List<String> propertyNames) {
        if (propertyNames != null) {
            String defaultGeometry = geometryDescriptor.getLocalName();
            if (!propertyNames.contains(defaultGeometry)) propertyNames.add(defaultGeometry);
            query.setPropertyNames(propertyNames);
        }
    }

    /** Returns true if features with the same attributes should be coalesced, false otherwise */
    boolean isCoalesceEnabled() {
        return coalesceEnabled;
    }
}

/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.clickhouse;

import static org.geotools.dggs.clickhouse.ClickHouseDGGSDataStore.GEOMETRY;
import static org.geotools.dggs.gstore.DGGSStore.ZONE_ID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.geotools.api.feature.FeatureVisitor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.FeatureAttributeVisitor;
import org.geotools.feature.visitor.GroupByVisitor;
import org.geotools.feature.visitor.GroupByVisitor.GroupByRawResult;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class DGGSFeatureCollection implements SimpleFeatureCollection {
    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private final SimpleFeatureType schema;
    private final DGGSInstance dggs;
    private final SimpleFeatureCollection delegate;

    public DGGSFeatureCollection(SimpleFeatureCollection delegate, SimpleFeatureType schema, DGGSInstance dggs) {
        this.delegate = delegate;
        this.schema = schema;
        this.dggs = dggs;
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public String getID() {
        return delegate.getID();
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        // if the visitor is geometryless, we can delegate to the underlying collection
        if (isGeometryless(visitor, getSchema())) {
            delegate.accepts(visitor, progress);
        }

        // special case for aggregate visitors grouping on geometry (need to map the geometry to zone ids)
        if (visitor instanceof GroupByVisitor) {
            GroupByVisitor groupByVisitor = (GroupByVisitor) visitor;
            if (delegateGroupBy(progress, groupByVisitor)) return;
        }

        DataUtilities.visit(this, visitor, progress);
    }

    /**
     * Checks conditions for delegating the group by operation to the underlying feature collection, and if so, performs
     * the delegation and the mapping between geometries and zone ids.
     *
     * @return True if the delegation was delegate, false otherwise
     */
    private boolean delegateGroupBy(ProgressListener progress, GroupByVisitor groupByVisitor) throws IOException {
        Expression expression = groupByVisitor.getExpression();
        List<Expression> groupByAttributes = groupByVisitor.getGroupByAttributes();
        // in order to delegate we need to aggregate and group on property names, and the aggregation must not be on
        // the geometry (must be a grouping attribute instead). The case where the geometry is not used anywhere is
        // already handled by the caller of this method, we can assume a geometry is present somewhere
        if (!(expression instanceof PropertyName)
                || GEOMETRY.equals(((PropertyName) expression).getPropertyName())
                || !allPropertyNames(groupByAttributes)) {
            return false;
        }

        // map the geometry attributes to zone ids and execute the visit on them
        int geometryIdx = getGeometryIndex(groupByAttributes);
        List<Expression> newGroupAttributes = new ArrayList<>(groupByAttributes);
        newGroupAttributes.set(geometryIdx, FF.property(ZONE_ID));
        GroupByVisitor mappedVisitor =
                new GroupByVisitor(groupByVisitor.getAggregate(), expression, newGroupAttributes, null);
        delegate.accepts(mappedVisitor, progress);

        // the result now needs to be mapped back to the geometry
        @SuppressWarnings("unchecked")
        Map<List<Object>, Object> unmappedResults =
                (Map<List<Object>, Object>) mappedVisitor.getResult().toMap();
        List<GroupByRawResult> mappedResults = unmappedResults.entrySet().stream()
                .map(e -> {
                    // the key is a list of grouping attribute values, we need to map the zone id back to the geometry
                    List<Object> key = e.getKey();
                    Zone zone = dggs.getZone((String) key.get(geometryIdx));
                    key.set(geometryIdx, zone.getBoundary());
                    return new GroupByRawResult(key, e.getValue());
                })
                .collect(Collectors.toList());
        groupByVisitor.setValue(mappedResults);

        return true;
    }

    private int getGeometryIndex(List<Expression> expressions) {
        for (int i = 0; i < expressions.size(); i++) {
            Expression e = expressions.get(i);
            if (e instanceof PropertyName && GEOMETRY.equals(((PropertyName) e).getPropertyName())) {
                return i;
            }
        }

        throw new IllegalArgumentException(
                "Group by attributes must contain a geometry attribute, but none found in: " + expressions);
    }

    private static boolean allPropertyNames(List<Expression> groupByAttributes) {
        return groupByAttributes.stream().allMatch(e -> e instanceof PropertyName);
    }

    /**
     * Returns true if the visitor is geometryless, that is, it's not accessing a geometry field in the target schema
     */
    public static boolean isGeometryless(FeatureVisitor visitor, SimpleFeatureType schema) {
        if (visitor instanceof FeatureAttributeVisitor) {
            // pass through unless one of the expressions requires the geometry attribute
            FilterAttributeExtractor extractor = new FilterAttributeExtractor(schema);
            for (Expression e : ((FeatureAttributeVisitor) visitor).getExpressions()) {
                e.accept(extractor, null);
            }

            for (PropertyName pname : extractor.getPropertyNameSet()) {
                AttributeDescriptor att = (AttributeDescriptor) pname.evaluate(schema);
                if (att instanceof GeometryDescriptor) {
                    return false;
                }
            }
            return true;
        } else if (visitor instanceof CountVisitor) {
            return true;
        }
        return false;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(this);
    }

    @Override
    public boolean contains(Object o) {
        try (SimpleFeatureIterator fi = features()) {
            while (fi.hasNext()) {
                SimpleFeature next = fi.next();
                if (Objects.equals(o, next)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        Set<?> candidates = new HashSet<>(o);
        try (SimpleFeatureIterator fi = features()) {
            while (fi.hasNext()) {
                SimpleFeature next = fi.next();
                candidates.remove(next);
                if (candidates.isEmpty()) break;
            }
        }
        return candidates.isEmpty();
    }

    @Override
    public boolean isEmpty() {
        try (SimpleFeatureIterator fi = features()) {
            return !fi.hasNext();
        }
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @Override
    @SuppressWarnings("unchecked") // some blind cast necessary
    public <O> O[] toArray(O[] array) {
        int size = size();
        if (array.length < size) {
            @SuppressWarnings("unchecked")
            O[] grown =
                    (O[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), size);
            array = grown;
        }
        try (FeatureIterator<SimpleFeature> it = features()) {
            int i = 0;
            for (; it.hasNext() && i < size; i++) {
                array[i] = (O) it.next();
            }
            for (; i < size; i++) {
                array[i] = null;
            }
            return array;
        }
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        // TODO: optimize it so that the delegate feature collection is rebuilt instead
        return new FilteringSimpleFeatureCollection(delegate, filter);
    }

    @Override
    public SimpleFeatureCollection sort(SortBy order) {
        // TODO: optimize it so that the delegate feature collection is rebuilt instead
        return new SortedSimpleFeatureCollection(delegate, new SortBy[] {order});
    }

    @Override
    public SimpleFeatureIterator features() {
        @SuppressWarnings("PMD.CloseResource") // wrapped and returned
        SimpleFeatureIterator it = delegate.features();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(getSchema());
        return new SimpleFeatureIterator() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public SimpleFeature next() throws NoSuchElementException {
                return wrapFeature(it.next(), fb);
            }

            @Override
            public void close() {
                it.close();
            }
        };
    }

    private SimpleFeature wrapFeature(SimpleFeature next, SimpleFeatureBuilder fb) {
        for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
            String name = ad.getLocalName();
            if (GEOMETRY.equals(name)) {
                Zone zone = dggs.getZone((String) next.getAttribute("zoneId"));
                fb.add(zone.getBoundary());
            } else {
                fb.add(next.getAttribute(name));
            }
        }
        return fb.buildFeature(next.getID());
    }
}

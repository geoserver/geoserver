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
package org.geootols.dggs.clickhouse;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.FeatureAttributeVisitor;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

public class DGGSFeatureCollection implements SimpleFeatureCollection {
    private final SimpleFeatureType schema;
    private final DGGSInstance dggs;
    private final SimpleFeatureCollection delegate;

    public DGGSFeatureCollection(
            SimpleFeatureCollection delegate, SimpleFeatureType schema, DGGSInstance dggs) {
        this.delegate = delegate;
        this.schema = schema;
        this.dggs = dggs;
    }

    public String[] getSchemaProperties(SimpleFeatureType schema) {
        return schema.getAttributeDescriptors()
                .stream()
                .map(ad -> ad.getLocalName())
                .toArray(n -> new String[n]);
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
        if (isGeometryless(visitor, getSchema())) {
            delegate.accepts(visitor, progress);
        } else {
            DataUtilities.visit(this, visitor, progress);
        }
    }

    /**
     * Returns true if the visitor is geometryless, that is, it's not accessing a geometry field in
     * the target schema
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
    public <O> O[] toArray(O[] array) {
        int size = size();
        if (array.length < size) {
            @SuppressWarnings("unchecked")
            O[] grown =
                    (O[])
                            java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(), size);
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
            if (ClickHouseDGGSDataStore.GEOMETRY.equals(name)) {
                Zone zone = dggs.getZone((String) next.getAttribute("zoneId"));
                fb.add(zone.getBoundary());
            } else {
                fb.add(next.getAttribute(name));
            }
        }
        return fb.buildFeature(next.getID());
    }
}

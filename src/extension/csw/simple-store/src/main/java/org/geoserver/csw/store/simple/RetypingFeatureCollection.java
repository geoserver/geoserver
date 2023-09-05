/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import java.util.Iterator;
import java.util.List;
import org.geoserver.csw.feature.AbstractFeatureCollection;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.feature.FeatureCollection;

/**
 * Very basic retyper, can only shave off root attributes and does not really reduce the feature
 * type, but only the attributes in the returned features.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RetypingFeatureCollection<T extends FeatureType, F extends Feature>
        extends AbstractFeatureCollection<T, F> {

    FeatureCollection<T, F> delegate;
    List<PropertyName> properties;

    public RetypingFeatureCollection(
            FeatureCollection<T, F> delegate, List<PropertyName> properties) {
        super(delegate.getSchema());
        this.delegate = delegate;
        this.properties = properties;
    }

    @Override
    public FeatureCollection<T, F> subCollection(Filter filter) {
        FeatureCollection<T, F> subCollection = delegate.subCollection(filter);
        return new RetypingFeatureCollection<>(subCollection, properties);
    }

    @Override
    public FeatureCollection<T, F> sort(SortBy order) {
        FeatureCollection<T, F> sorted = delegate.sort(order);
        return new RetypingFeatureCollection<>(sorted, properties);
    }

    @Override
    protected Iterator<F> openIterator() {
        return new RetypingIterator<>(delegate.features(), schema, properties);
    }

    @Override
    protected void closeIterator(Iterator<F> close) {
        if (close instanceof RetypingIterator) {
            ((RetypingIterator) close).close();
        }
    }
}

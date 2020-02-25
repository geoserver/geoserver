/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.geoserver.security.SecurityUtils.*;

import java.util.logging.Logger;
import org.geoserver.security.WrapperPolicy;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Secures a feature collection according to the given policy. The implementation assumes all of the
 * attributes that should not be read have been shaved off already, and similarly, that the read
 * filters have been applied already in the delegate, and adds control over writes
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 * @param <F>
 */
public class SecuredFeatureCollection<T extends FeatureType, F extends Feature>
        extends DecoratingFeatureCollection<T, F> {
    static final Logger LOGGER = Logging.getLogger(SecuredFeatureCollection.class);

    WrapperPolicy policy;

    SecuredFeatureCollection(FeatureCollection<T, F> delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public org.geotools.feature.FeatureIterator<F> features() {
        return (FeatureIterator) SecuredObjects.secure(delegate.features(), policy);
    }

    public FeatureCollection<T, F> sort(SortBy order) {
        // attributes should have been shaved already
        final FeatureCollection<T, F> fc = delegate.sort(order);
        if (fc == null) return null;
        else return (FeatureCollection) SecuredObjects.secure(fc, policy);
    }

    public FeatureCollection<T, F> subCollection(Filter filter) {
        final FeatureCollection<T, F> fc = delegate.subCollection(filter);
        if (fc == null) return null;
        else return (FeatureCollection) SecuredObjects.secure(fc, policy);
    }
}

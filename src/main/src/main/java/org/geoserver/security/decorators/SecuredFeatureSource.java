/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataAccess;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

/**
 * Given a {@link FeatureSource} makes sure only the operations allowed by the WrapperPolicy
 * can be performed through it or using a object that can be accessed thru it. Depending on the
 * challenge policy, the object and the related ones will simply hide feature source abilities,
 * or will throw Spring security exceptions
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 * @param <T>
 * @param <F>
 */
public class SecuredFeatureSource<T extends FeatureType, F extends Feature>
        extends DecoratingFeatureSource<T, F> {
    
    static final Logger LOGGER = Logging.getLogger(SecuredFeatureSource.class);

    WrapperPolicy policy;

    protected SecuredFeatureSource(FeatureSource<T, F> delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    public DataAccess<T, F> getDataStore() {
        final DataAccess<T, F> store = delegate.getDataStore();
        if (store == null)
            return null;
        else 
            return (DataAccess) SecuredObjects.secure(store, policy);
    }

    public FeatureCollection<T, F> getFeatures() throws IOException {
        final FeatureCollection<T, F> fc = delegate.getFeatures(getReadQuery());
        if (fc == null)
            return null;
        else
            return (FeatureCollection) SecuredObjects.secure(fc, policy);
    }

    public FeatureCollection<T, F> getFeatures(Filter filter)
            throws IOException {
        return getFeatures(new Query(null, filter));
    }

    public FeatureCollection<T, F> getFeatures(Query query) throws IOException {
        // mix the external query with the access limits one
        final Query readQuery = getReadQuery();
        final Query mixed = DataUtilities.mixQueries(query, readQuery, query.getHandle());
        final FeatureCollection<T, F> fc = delegate.getFeatures(mixed);
        if (fc == null)
            return null;
        else
            return (FeatureCollection) SecuredObjects.secure(fc, policy);
    }
    
    protected Query getReadQuery() {
        if(policy.getAccessLevel() == AccessLevel.HIDDEN || policy.getAccessLevel() == AccessLevel.METADATA) {
            return new Query(null, Filter.EXCLUDE);
        } else if(policy.getLimits() == null) {
            return Query.ALL;
        } else if(policy.getLimits() instanceof VectorAccessLimits) {
            VectorAccessLimits val = (VectorAccessLimits) policy.getLimits();
            
            // Ugly hack: during WFS transactions the reads we do are used to count the number of features
            // we are deleting/updating: use the write filter instead of the read filter
            Request request = Dispatcher.REQUEST.get();
            if(request != null && request.getService().equalsIgnoreCase("WFS") && request.getRequest().equalsIgnoreCase("Transaction")) {
                return val.getWriteQuery();
            } else {
                return val.getReadQuery();
            }
            
        } else {
            throw new IllegalArgumentException("SecureFeatureSources has been fed " +
            		"with unexpected AccessLimits class " + policy.getLimits().getClass());
        }
    }
}

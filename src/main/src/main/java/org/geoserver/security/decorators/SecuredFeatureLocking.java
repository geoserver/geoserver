/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.geoserver.security.SecurityUtils.getWriteQuery;

import java.io.IOException;
import org.geoserver.security.WrapperPolicy;
import org.geotools.api.data.FeatureLock;
import org.geotools.api.data.FeatureLocking;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;

/**
 * See {@link SecuredFeatureStore} for an explanation of why this class exists
 *
 * @author Andrea Aime GeoSolutions
 * @param <T>
 * @param <F>
 */
public class SecuredFeatureLocking<T extends FeatureType, F extends Feature>
        extends SecuredFeatureStore<T, F> implements FeatureLocking<T, F> {

    FeatureLocking<T, F> lockDelegate;

    protected SecuredFeatureLocking(FeatureLocking<T, F> delegate, WrapperPolicy policy) {
        super(delegate, policy);
        this.lockDelegate = delegate;
    }

    @Override
    public int lockFeatures() throws IOException {
        return lockFeatures(Filter.INCLUDE);
    }

    @Override
    public int lockFeatures(Query query) throws IOException {
        Query writeQuery = getWriteQuery(policy);
        Query mixed = mixQueries(query, writeQuery);
        final Filter writeFilter = writeQuery.getFilter();

        if (writeFilter == Filter.EXCLUDE) {
            throw unsupportedOperation();
        } else if (writeFilter == Filter.INCLUDE) {
            return lockDelegate.lockFeatures(query);
        } else {
            return lockDelegate.lockFeatures(mixed);
        }
    }

    @Override
    public int lockFeatures(Filter filter) throws IOException {
        return lockDelegate.lockFeatures(new Query(null, filter));
    }

    @Override
    public void setFeatureLock(FeatureLock lock) {
        Query writeQuery = getWriteQuery(policy);
        if (writeQuery.getFilter() == Filter.EXCLUDE) {
            throw unsupportedOperation();
        } else {
            lockDelegate.setFeatureLock(lock);
        }
    }

    @Override
    public void unLockFeatures() throws IOException {
        unLockFeatures(Query.ALL);
    }

    @Override
    public void unLockFeatures(Filter filter) throws IOException {
        unLockFeatures(new Query(null, filter));
    }

    @Override
    public void unLockFeatures(Query query) throws IOException {
        Query writeQuery = getWriteQuery(policy);
        Query mixed = mixQueries(query, writeQuery);
        final Filter writeFilter = writeQuery.getFilter();

        if (writeFilter == Filter.EXCLUDE) {
            throw unsupportedOperation();
        } else if (writeFilter == Filter.INCLUDE) {
            lockDelegate.unLockFeatures(query);
        } else {
            lockDelegate.unLockFeatures(mixed);
        }
    }
}

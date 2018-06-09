/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.geoserver.security.SecurityUtils.*;

import java.io.IOException;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureLocking;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

/**
 * See {@link SecuredFeatureStore} for an explanation of why this class exists
 *
 * @author Andrea Aime GeoSolutions
 * @param <T>
 * @param <F>
 */
public class SecuredFeatureLocking<T extends FeatureType, F extends Feature>
        extends SecuredFeatureStore<T, F> implements FeatureLocking<T, F> {

    FeatureLocking lockDelegate;

    protected SecuredFeatureLocking(FeatureLocking delegate, WrapperPolicy policy) {
        super(delegate, policy);
        this.lockDelegate = delegate;
    }

    public int lockFeatures() throws IOException {
        return lockFeatures(Filter.INCLUDE);
    }

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

    public int lockFeatures(Filter filter) throws IOException {
        return lockDelegate.lockFeatures(new Query(null, filter));
    }

    public void setFeatureLock(FeatureLock lock) {
        Query writeQuery = getWriteQuery(policy);
        if (writeQuery.getFilter() == Filter.EXCLUDE) {
            throw unsupportedOperation();
        } else {
            lockDelegate.setFeatureLock(lock);
        }
    }

    public void unLockFeatures() throws IOException {
        unLockFeatures(Query.ALL);
    }

    public void unLockFeatures(Filter filter) throws IOException {
        unLockFeatures(new Query(null, filter));
    }

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

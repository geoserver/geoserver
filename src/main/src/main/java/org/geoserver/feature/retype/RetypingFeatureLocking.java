/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import java.io.IOException;
import org.geotools.api.data.FeatureLock;
import org.geotools.api.data.FeatureLocking;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureLocking;
import org.geotools.api.filter.Filter;

/** Renaming wrapper for a {@link FeatureLocking} instance, to be used along with {@link RetypingDataStore} */
class RetypingFeatureLocking extends RetypingFeatureStore implements SimpleFeatureLocking {

    RetypingFeatureLocking(RetypingDataStore ds, SimpleFeatureLocking wrapped, FeatureTypeMap typeMap) {
        super(ds, wrapped, typeMap);
    }

    RetypingFeatureLocking(SimpleFeatureLocking wrapped, FeatureTypeMap typeMap) throws IOException {
        super(wrapped, typeMap);
    }

    SimpleFeatureLocking featureLocking() {
        return (SimpleFeatureLocking) wrapped;
    }

    @Override
    public int lockFeatures() throws IOException {
        return featureLocking().lockFeatures();
    }

    @Override
    public int lockFeatures(Query query) throws IOException {
        return featureLocking().lockFeatures(store.retypeQuery(query, typeMap));
    }

    @Override
    public int lockFeatures(Filter filter) throws IOException {
        return featureLocking().lockFeatures(store.retypeFilter(filter, typeMap));
    }

    @Override
    public void setFeatureLock(FeatureLock lock) {
        featureLocking().setFeatureLock(lock);
    }

    @Override
    public void unLockFeatures() throws IOException {
        featureLocking().unLockFeatures();
    }

    @Override
    public void unLockFeatures(Filter filter) throws IOException {
        featureLocking().unLockFeatures(store.retypeFilter(filter, typeMap));
    }

    @Override
    public void unLockFeatures(Query query) throws IOException {
        featureLocking().unLockFeatures(store.retypeQuery(query, typeMap));
    }
}

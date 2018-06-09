/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import java.io.IOException;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureLocking;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.opengis.filter.Filter;

/**
 * Renaming wrapper for a {@link FeatureLocking} instance, to be used along with {@link
 * RetypingDataStore}
 */
class RetypingFeatureLocking extends RetypingFeatureStore implements SimpleFeatureLocking {

    RetypingFeatureLocking(
            RetypingDataStore ds, SimpleFeatureLocking wrapped, FeatureTypeMap typeMap) {
        super(ds, wrapped, typeMap);
    }

    RetypingFeatureLocking(SimpleFeatureLocking wrapped, FeatureTypeMap typeMap)
            throws IOException {
        super(wrapped, typeMap);
    }

    SimpleFeatureLocking featureLocking() {
        return (SimpleFeatureLocking) wrapped;
    }

    public int lockFeatures() throws IOException {
        return featureLocking().lockFeatures();
    }

    public int lockFeatures(Query query) throws IOException {
        return featureLocking().lockFeatures(store.retypeQuery(query, typeMap));
    }

    public int lockFeatures(Filter filter) throws IOException {
        return featureLocking().lockFeatures(store.retypeFilter(filter, typeMap));
    }

    public void setFeatureLock(FeatureLock lock) {
        featureLocking().setFeatureLock(lock);
    }

    public void unLockFeatures() throws IOException {
        featureLocking().unLockFeatures();
    }

    public void unLockFeatures(Filter filter) throws IOException {
        featureLocking().unLockFeatures(store.retypeFilter(filter, typeMap));
    }

    public void unLockFeatures(Query query) throws IOException {
        featureLocking().unLockFeatures(store.retypeQuery(query, typeMap));
    }
}

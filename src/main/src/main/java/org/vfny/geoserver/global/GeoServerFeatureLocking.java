/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.io.IOException;
import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureLocking;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * GeoServer wrapper for backend Geotools2 DataStore.
 *
 * <p>Support FeatureSource decorator for FeatureTypeInfo that takes care of mapping the
 * FeatureTypeInfo's FeatureSource with the schema and definition query configured for it.
 *
 * <p>Because GeoServer requires that attributes always be returned in the same order we need a way
 * to smoothly inforce this. Could we use this class to do so? It would need to support writing and
 * locking though.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GeoServerFeatureLocking extends GeoServerFeatureStore implements SimpleFeatureLocking {
    /**
     * Creates a new DEFQueryFeatureLocking object.
     *
     * @param locking GeoTools2 FeatureSource
     * @param settings Settings for this store
     */
    GeoServerFeatureLocking(
            FeatureLocking<SimpleFeatureType, SimpleFeature> locking, Settings settings) {
        super(locking, settings);
    }

    FeatureLocking<SimpleFeatureType, SimpleFeature> locking() {
        return (FeatureLocking<SimpleFeatureType, SimpleFeature>) source;
    }

    /**
     * Description ...
     *
     * @see
     *     org.vfny.geoserver.global.GeoServerFeatureStore#setFeatureLock(org.geotools.data.FeatureLock)
     */
    public void setFeatureLock(FeatureLock lock) {
        if (source instanceof FeatureLocking) {
            ((FeatureLocking<SimpleFeatureType, SimpleFeature>) source).setFeatureLock(lock);
        } else {
            throw new UnsupportedOperationException("FeatureTypeConfig does not supports locking");
        }
    }

    /** */
    public int lockFeatures(Query query) throws IOException {
        if (source instanceof FeatureLocking) {
            return ((FeatureLocking<SimpleFeatureType, SimpleFeature>) source).lockFeatures(query);
        } else {
            throw new DataSourceException("FeatureTypeConfig does not supports locking");
        }
    }

    //    /**
    //     * A custom hack for PostgisFeatureLocking?
    //     *

    //     *

    //     *

    //     */
    //    public int lockFeature(Feature feature) throws IOException {
    //        if (source instanceof PostgisFeatureLocking) {
    //            return ((PostgisFeatureLocking) source).lockFeature(feature);
    //        }
    //
    //        throw new IOException("FeatureTypeConfig does not support single FeatureLock");
    //    }

    /** */
    public int lockFeatures(Filter filter) throws IOException {
        filter = makeDefinitionFilter(filter);

        return locking().lockFeatures(filter);
    }

    /** */
    public int lockFeatures() throws IOException {
        return locking().lockFeatures();
    }

    /** */
    public void unLockFeatures() throws IOException {
        locking().lockFeatures();
    }

    /** */
    public void unLockFeatures(Filter filter) throws IOException {
        filter = makeDefinitionFilter(filter);

        locking().unLockFeatures(filter);
    }

    public void unLockFeatures(Query query) throws IOException {
        query = makeDefinitionQuery(query, schema);

        locking().lockFeatures(query);
    }
}

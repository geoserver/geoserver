/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureLocking;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * GeoServer wrapper for backend Geotools2 DataStore.
 *
 * <p>
 * Support FeatureSource decorator for FeatureTypeInfo that takes care of
 * mapping the FeatureTypeInfo's FeatureSource with the schema and definition
 * query configured for it.
 * </p>
 *
 * <p>
 * Because GeoServer requires that attributes always be returned in the same
 * order we need a way to smoothly inforce this. Could we use this class to do
 * so? It would need to support writing and locking though.
 * </p>
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GeoServerFeatureLocking extends GeoServerFeatureStore implements SimpleFeatureLocking {
    /**
     * Creates a new DEFQueryFeatureLocking object.
     *
     * @param locking GeoTools2 FeatureSource
     * @param schema DOCUMENT ME!
     * @param definitionQuery DOCUMENT ME!
     * @param declaredCRS 
     * @param srsHandling see {@link FeatureTypeInfo#FORCE} & co.
     * @param metadata metadata associated with the feature
     */
    GeoServerFeatureLocking(FeatureLocking<SimpleFeatureType, SimpleFeature> locking,
            SimpleFeatureType schema, Filter definitionQuery,
            CoordinateReferenceSystem declaredCRS, int srsHandling,
            Map<String, Serializable> metadata) {
        super(locking, schema, definitionQuery, declaredCRS, srsHandling, metadata);
    }

    FeatureLocking<SimpleFeatureType, SimpleFeature> locking() {
        return (FeatureLocking<SimpleFeatureType, SimpleFeature>) source;
    }

    /**
     * <p>
     * Description ...
     * </p>
     *
     * @param lock
     *
     * @throws UnsupportedOperationException DOCUMENT ME!
     *
     * @see org.vfny.geoserver.global.GeoServerFeatureStore#setFeatureLock(org.geotools.data.FeatureLock)
     */
    public void setFeatureLock(FeatureLock lock) {
        if (source instanceof FeatureLocking) {
            ((FeatureLocking<SimpleFeatureType, SimpleFeature>) source).setFeatureLock(lock);
        } else {
            throw new UnsupportedOperationException("FeatureTypeConfig does not supports locking");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param query DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     * @throws DataSourceException DOCUMENT ME!
     */
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
    //     * @param feature DOCUMENT ME!
    //     *
    //     * @return DOCUMENT ME!
    //     *
    //     * @throws IOException DOCUMENT ME!
    //     */
    //    public int lockFeature(Feature feature) throws IOException {
    //        if (source instanceof PostgisFeatureLocking) {
    //            return ((PostgisFeatureLocking) source).lockFeature(feature);
    //        }
    //
    //        throw new IOException("FeatureTypeConfig does not support single FeatureLock");
    //    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public int lockFeatures(Filter filter) throws IOException {
        filter = makeDefinitionFilter(filter);

        return locking().lockFeatures(filter);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public int lockFeatures() throws IOException {
        return locking().lockFeatures();
    }

    /**
     * DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public void unLockFeatures() throws IOException {
        locking().lockFeatures();
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public void unLockFeatures(Filter filter) throws IOException {
        filter = makeDefinitionFilter(filter);

        locking().unLockFeatures(filter);
    }

    public void unLockFeatures(Query query) throws IOException {
        query = makeDefinitionQuery(query, schema);

        locking().lockFeatures(query);
    }
}

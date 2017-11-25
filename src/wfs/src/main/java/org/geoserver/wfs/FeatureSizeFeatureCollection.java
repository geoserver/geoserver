/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;

import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;


/**
 * A feature collection which caches a configurable (and small) amount of features.
 * 
 * @author Alvaro Huarte
 */
class FeatureSizeFeatureCollection extends DecoratingSimpleFeatureCollection {
    
    /**
     * The original feature source.
     */
    protected FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
    
    /**
     * The feature cache to manage.
     */
    protected List<SimpleFeature> featureCache;
    
    /**
     * The original query.
     */
    protected Query query;
    
    /**
     * Defines the maximum number of cacheable features to avoid successive readings 
     * of the data source.
     * <p>
     * With a minimum overload of memory, it takes advantage of a previous reading 
     * of features when the feature source does not support direct count of the 
     * collection managed (e.g. shapefile stores).
     * </p>
     * <p>
     * It is very useful when the feature store needs to execute costly queries 
     * and the filter returns empty or low-count feature results. 
     * In some contexts, WFS-GetFeature requests need to precalculate the size 
     * of the results, and (e.g. shapefile stores) the query is executed at 
     * least twice causing two readings of the data source.
     * </p>
     */
    static int FEATURE_CACHE_LIMIT = 
        Integer.valueOf(System.getProperty("org.geoserver.wfs.getfeature.cachelimit", "16"));
    
    /**
     * Allows to programmatically set the maximum number of cacheable features.
     */
    public static void setFeatureCacheLimit(int featureCacheLimit) {
        FEATURE_CACHE_LIMIT = featureCacheLimit;
    }

    public FeatureSizeFeatureCollection(SimpleFeatureCollection delegate, FeatureSource<? extends FeatureType, ? extends Feature> source, Query query) {
        super(delegate);
        this.featureSource = source;
        this.query = query;
    }

    class CachedWrappingFeatureIterator implements SimpleFeatureIterator {
        
        private List<SimpleFeature> featureCache;
        private int featureIndex = 0;
        
        public CachedWrappingFeatureIterator(List<SimpleFeature> featureCache) {
            this.featureCache = featureCache;
        }
        
        @Override
        public boolean hasNext() {
            return featureIndex < featureCache.size();
        }
        
        @Override
        public SimpleFeature next() {
            return featureCache.get(featureIndex++);
        }
        
        @Override
        public void close() {
            featureIndex = 0;
        }
    }
    
    @Override
    public SimpleFeatureIterator features() {
        if (featureCache != null) {
            return new CachedWrappingFeatureIterator( featureCache );
        }
        return super.features();
    }
    
    @Override
    public int size() {
        if (featureCache != null) {
            return featureCache.size();
        }
        if (FEATURE_CACHE_LIMIT > 0) {
            FeatureIterator<? extends Feature> it = null;
            
            try {
                int count = featureSource.getCount(query);
                
                if (count == 0) {
                    featureCache = new ArrayList<SimpleFeature>();
                    return count;
                }
                if (count > 0) {
                    return count;
                }
                
                // we have to iterate, save to cache to avoid later successive readings of data.
                List<SimpleFeature> tempFeatureCache = new ArrayList<SimpleFeature>();
                
                // bean counting...
                it = featureSource.getFeatures(query).features();
                count = 0;
                while (it.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) it.next();
                    if (tempFeatureCache.size() < FEATURE_CACHE_LIMIT) tempFeatureCache.add(feature);
                    count++;
                }
                if (count <= FEATURE_CACHE_LIMIT) {
                    featureCache = tempFeatureCache;
                } else {
                    tempFeatureCache.clear();
                }
                return count;
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (it != null) {
                    it.close();
                }
            }
        }
        return super.size();
    }
}

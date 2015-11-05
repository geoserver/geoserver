package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.AbstractFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;

import org.opengis.feature.simple.SimpleFeature;

/**
 * A feature collection wrapping a base collection to cache the features managed.
 * It avoids read from data source each an iterator is called.
 * 
 * @author Alvaro Huarte
 */
class CachedFeatureCollection extends AbstractFeatureCollection {
    private List<SimpleFeature> cache = new ArrayList<SimpleFeature>();
    
    public CachedFeatureCollection(final SimpleFeatureCollection featureCollection) {
        super(featureCollection.getSchema());
        
        SimpleFeatureIterator featureIterator = featureCollection.features();
        if (featureIterator!=null) {
            try {
                while (featureIterator.hasNext()) cache.add(featureIterator.next());
            }
            finally {
                featureIterator.close();
            }
        }
        id = featureCollection.getID();
    }
    
    @Override
    public String getID() {
        return id;
    }
    
    @Override
    protected Iterator<SimpleFeature> openIterator() {
        return cache.iterator();
    }

    @Override
    public int size() {
        return cache.size();
    }
    
    @Override
    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(this);
    }
}

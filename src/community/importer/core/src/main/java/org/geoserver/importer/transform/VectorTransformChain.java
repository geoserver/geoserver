/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.util.logging.Logging;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Transform chain for vectors.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class VectorTransformChain extends TransformChain<VectorTransform> {

    static Logger LOGGER = Logging.getLogger(VectorTransformChain.class);

    public VectorTransformChain(List<VectorTransform> transforms) {
        super(transforms);
    }

    public VectorTransformChain(VectorTransform... transforms) {
        super(transforms);
    }
    
    public void pre(ImportTask item, ImportData data) throws Exception {
        for (PreVectorTransform tx : filter(transforms, PreVectorTransform.class)) {
            try {
                tx.apply(item, data);
            } catch (Exception e) {
                error(tx, e);
            }
        }
    }

    public SimpleFeatureType inline(ImportTask task, DataStore dataStore, SimpleFeatureType featureType) 
        throws Exception {
        
        for (InlineVectorTransform tx : filter(transforms, InlineVectorTransform.class)) {
            try {
                tx.init();
                featureType = tx.apply(task, dataStore, featureType);
            } catch (Exception e) {
                error(tx, e);
            }
        }
        
        return featureType;
    }

    public SimpleFeature inline(ImportTask task, DataStore dataStore, SimpleFeature oldFeature, 
        SimpleFeature feature) throws Exception {
        
        for (InlineVectorTransform tx : filter(transforms, InlineVectorTransform.class)) {
            try {
                feature = tx.apply(task, dataStore, oldFeature, feature);
                if (feature == null) {
                    break;
                }
            } catch (Exception e) {
                error(tx, e);
            }
        }
        
        return feature;
    }

    public void post(ImportTask task, ImportData data) throws Exception {
        for (PostVectorTransform tx : filter(transforms, PostVectorTransform.class)) {
            try {
                tx.apply(task, data);
            } catch (Exception e) {
                error(tx, e);
            }
        }
    }

    <T> List<T> filter(List<VectorTransform> transforms, Class<T> type) {
        List<T> filtered = new ArrayList<T>();
        for (VectorTransform tx : transforms) {
            if (type.isInstance(tx)) {
                filtered.add((T) tx);
            }
        }
        return filtered;
    }

    void error(VectorTransform tx, Exception e) throws Exception {
        if (tx.stopOnError(e)) {
            throw e;
        }
        else {
            //log and continue
            LOGGER.log(Level.WARNING, "Transform " + tx + " failed", e);
        }
    }
}

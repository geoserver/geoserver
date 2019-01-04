/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.util.List;
import java.util.logging.Logger;
import org.geoserver.importer.ImportTask;
import org.geotools.data.DataStore;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Transform chain for vectors.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class VectorTransformChain extends TransformChain<VectorTransform> {
    private static final long serialVersionUID = 7406010540766743012L;

    static Logger LOGGER = Logging.getLogger(VectorTransformChain.class);

    public VectorTransformChain(List<VectorTransform> transforms) {
        super(transforms);
    }

    public VectorTransformChain(VectorTransform... transforms) {
        super(transforms);
    }

    public SimpleFeatureType inline(
            ImportTask task, DataStore dataStore, SimpleFeatureType featureType) throws Exception {

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

    public SimpleFeature inline(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {

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
}

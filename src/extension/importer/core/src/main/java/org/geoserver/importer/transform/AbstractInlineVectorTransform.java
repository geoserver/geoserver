/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import org.geoserver.importer.ImportTask;
import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/** Convenience base class to make creating inline vector transforms easier */
public abstract class AbstractInlineVectorTransform extends AbstractTransform
        implements InlineVectorTransform {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public SimpleFeatureType apply(
            ImportTask task, DataStore dataStore, SimpleFeatureType featureType) throws Exception {
        return featureType;
    }

    @Override
    public SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        return feature;
    }
}

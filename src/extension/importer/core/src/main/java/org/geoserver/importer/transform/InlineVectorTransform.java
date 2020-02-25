/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import org.geoserver.importer.ImportTask;
import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Vector transform that is performed inline features are read from the source and written to the
 * destination.
 *
 * <p>This type of transform can only be applied to an indirect import.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface InlineVectorTransform extends VectorTransform {

    SimpleFeatureType apply(ImportTask task, DataStore dataStore, SimpleFeatureType featureType)
            throws Exception;

    /** @return null to discontinue processing */
    SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception;
}

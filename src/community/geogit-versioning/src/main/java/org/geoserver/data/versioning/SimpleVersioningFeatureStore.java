package org.geoserver.data.versioning;

import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public interface SimpleVersioningFeatureStore extends
        VersioningFeatureStore<SimpleFeatureType, SimpleFeature>, SimpleFeatureStore {

}

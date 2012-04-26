package org.geoserver.data.versioning;

import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public interface VersioningDataStore extends
        VersioningDataAccess<SimpleFeatureType, SimpleFeature>, DataStore {

}

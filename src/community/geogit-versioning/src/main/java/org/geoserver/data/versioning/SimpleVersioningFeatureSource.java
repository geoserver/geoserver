package org.geoserver.data.versioning;

import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public interface SimpleVersioningFeatureSource extends
        VersioningFeatureSource<SimpleFeatureType, SimpleFeature>, SimpleFeatureSource {

}

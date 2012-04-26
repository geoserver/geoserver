package org.geoserver.data.versioning;

import org.geotools.data.simple.SimpleFeatureLocking;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public interface SimpleVersioningFeatureLocking extends
        VersioningFeatureLocking<SimpleFeatureType, SimpleFeature>, SimpleFeatureLocking {

}

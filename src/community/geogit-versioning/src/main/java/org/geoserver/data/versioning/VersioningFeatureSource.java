package org.geoserver.data.versioning;

import org.geotools.data.FeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public interface VersioningFeatureSource<F extends FeatureType, T extends Feature> extends
        FeatureSource<F, T> {

}

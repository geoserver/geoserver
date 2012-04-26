package org.geoserver.data.versioning;

import org.geotools.data.FeatureStore;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public interface VersioningFeatureStore<F extends FeatureType, T extends Feature> extends
        FeatureStore<F, T> {

}

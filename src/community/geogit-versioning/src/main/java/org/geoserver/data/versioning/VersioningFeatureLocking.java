package org.geoserver.data.versioning;

import org.geotools.data.FeatureLocking;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public interface VersioningFeatureLocking<F extends FeatureType, T extends Feature> extends
        FeatureLocking<F, T> {

}

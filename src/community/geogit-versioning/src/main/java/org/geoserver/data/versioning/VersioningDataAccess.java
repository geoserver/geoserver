package org.geoserver.data.versioning;

import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public interface VersioningDataAccess<F extends FeatureType, T extends Feature> extends
        DataAccess<F, T> {

}

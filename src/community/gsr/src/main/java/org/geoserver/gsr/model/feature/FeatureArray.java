/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.feature;

import java.util.List;
import org.geoserver.gsr.model.GSRModel;

/** Model object for the list of features for an Add/Update/Delete Feature request */
public class FeatureArray implements GSRModel {
    public final List<Feature> features;

    public FeatureArray(List<Feature> features) {
        this.features = features;
    }
}

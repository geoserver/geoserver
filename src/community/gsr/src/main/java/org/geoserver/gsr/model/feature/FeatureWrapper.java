/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.feature;

import org.geoserver.gsr.model.AbstractGSRModel;
import org.geoserver.gsr.model.GSRModel;

public class FeatureWrapper extends AbstractGSRModel implements GSRModel {
    public Feature feature;

    public FeatureWrapper(Feature feature) {
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }
}

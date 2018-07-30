/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.feature;

import com.boundlessgeo.gsr.model.GSRModel;

public class FeatureWrapper implements GSRModel {
    public Feature feature;

    public FeatureWrapper(Feature feature) {
        this.feature = feature;
    }
}

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.feature;

import java.util.Collections;
import java.util.List;
import com.boundlessgeo.gsr.model.AbstractGSRModel;
import com.boundlessgeo.gsr.model.GSRModel;

public class FeatureWrapper extends AbstractGSRModel implements GSRModel {
    public Feature feature;

    public FeatureWrapper(Feature feature) {
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }
    
    
}

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.map;

import com.boundlessgeo.gsr.core.GSRModel;

import java.util.List;

/**
 * List of {@link Legends}
 */
public class Legends implements GSRModel {

    List<LayerLegend> legends;

    public Legends(List<LayerLegend> legends) {
        this.legends = legends;
    }
}

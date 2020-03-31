/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.feature;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.geoserver.gsr.translate.geometry.QuantizedGeometryEncoder;

/**
 * Geometry Tranformation representation, for use in {@link FeatureList} responses that use {@link
 * QuantizedGeometryEncoder quantized geometry}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transform {
    public final QuantizedGeometryEncoder.OriginPosition originPosition;
    public final double[] scale;
    public final double[] translate;

    public Transform(
            QuantizedGeometryEncoder.OriginPosition originPosition,
            double[] scale,
            double[] translate) {
        this.originPosition = originPosition;
        this.scale = scale;
        this.translate = translate;
    }
}

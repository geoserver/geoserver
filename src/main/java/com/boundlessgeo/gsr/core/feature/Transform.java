package com.boundlessgeo.gsr.core.feature;

import com.boundlessgeo.gsr.core.geometry.QuantizedGeometryEncoder;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Geometry Tranformation representation, for use in {@link FeatureList} responses that use
 * {@link com.boundlessgeo.gsr.core.geometry.QuantizedGeometryEncoder quantized geometry}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transform {
    public final QuantizedGeometryEncoder.OriginPosition originPosition;
    public final double[] scale;
    public final double[] translate;

    public Transform(QuantizedGeometryEncoder.OriginPosition originPosition, double[] scale, double[] translate) {
        this.originPosition = originPosition;
        this.scale = scale;
        this.translate = translate;
    }
}

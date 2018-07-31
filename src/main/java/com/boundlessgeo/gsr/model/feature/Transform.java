package com.boundlessgeo.gsr.model.feature;

import com.boundlessgeo.gsr.translate.geometry.QuantizedGeometryEncoder;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Geometry Tranformation representation, for use in {@link FeatureList} responses that use
 * {@link QuantizedGeometryEncoder quantized geometry}
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

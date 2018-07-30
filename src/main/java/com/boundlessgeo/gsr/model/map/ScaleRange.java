/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.map;

import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;

public class ScaleRange {
    public final Double minScale;
    public final Double maxScale;

    public ScaleRange(Double minScale, Double maxScale) {
        this.minScale = minScale;
        this.maxScale = maxScale;
    }

    public static ScaleRange extract(Style style) {
        Double minScale = null, maxScale = null;
        for (FeatureTypeStyle ft : style.featureTypeStyles()) {
            for (Rule r : ft.rules()) {
                double minS = r.getMinScaleDenominator();
                double maxS = r.getMaxScaleDenominator();
                if (minScale == null || minS > minScale) {
                    minScale = minS;
                }
                if (maxScale == null || maxS < maxScale) {
                    maxScale = maxS;
                }
            }
        }
        minScale = Double.isInfinite(minScale) ? null : minScale;
        maxScale = Double.isInfinite(maxScale) ? null : maxScale;
        return new ScaleRange(minScale, maxScale);
    }
}

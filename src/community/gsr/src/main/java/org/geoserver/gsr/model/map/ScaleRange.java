/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.map;

import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;

public class ScaleRange {
    public final Double minScale;
    public final Double maxScale;

    public ScaleRange(Double minScale, Double maxScale) {
        this.minScale = minScale;
        this.maxScale = maxScale;
    }

    public static ScaleRange extract(Style style) {
        Double minScale = null, maxScale = null;
        // find at which scales there is something visible, so we get the
        // max of max scales and min of min scales
        for (FeatureTypeStyle ft : style.featureTypeStyles()) {
            for (Rule r : ft.rules()) {
                double minS = r.getMinScaleDenominator();
                double maxS = r.getMaxScaleDenominator();
                if (minScale == null || minS < minScale) {
                    minScale = minS;
                }
                if (maxScale == null || maxS > maxScale) {
                    maxScale = maxS;
                }
            }
        }
        minScale = Double.isInfinite(minScale) ? 0 : minScale;
        maxScale = Double.isInfinite(maxScale) ? 0 : maxScale;
        return new ScaleRange(minScale, maxScale);
    }
}

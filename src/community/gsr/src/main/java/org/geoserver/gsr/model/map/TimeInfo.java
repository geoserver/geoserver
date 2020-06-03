/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.map;

import java.math.BigDecimal;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;

/** TimeInfo field, used by {@link LayerOrTable} */
public class TimeInfo {

    public final String startTimeField;
    public final String endTimeField;
    public final Object trackIdField = new Object();

    public final BigDecimal timeInterval;
    public final String timeIntervalUnits;
    public final TimeReference timeReference;

    public TimeInfo(DimensionInfo time) {
        startTimeField = time.getAttribute();
        if (time.getEndAttribute() != null) {
            endTimeField = time.getEndAttribute();
        } else {
            endTimeField = time.getAttribute();
        }

        if (time.getPresentation() == DimensionPresentation.DISCRETE_INTERVAL) {

            BigDecimal resolution = time.getResolution();
            timeInterval = resolution;
            timeIntervalUnits = resolution == null ? null : "ms";

            timeReference = new TimeReference();
        } else {
            timeInterval = null;
            timeIntervalUnits = null;
            timeReference = null;
        }
    }

    public static class TimeReference {
        public final String timeZone = "UTC";
        public final Boolean respectDaylightSaving = true;
    }
}

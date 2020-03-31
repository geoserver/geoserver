package com.boundlessgeo.gsr.model.map;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;

import java.math.BigDecimal;

/**
 * TimeInfo field, used by {@link LayerOrTable}
 */
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

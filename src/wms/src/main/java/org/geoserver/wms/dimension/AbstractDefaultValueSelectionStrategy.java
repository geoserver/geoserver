/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.util.Date;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.feature.type.DateUtil;

/**
 * Abstract parent class for DefaultValueSelectionStrategy implementations.
 *
 * @author Ilkka Rinne / Spatineo Inc. for Finnish Meteorological Institute
 */
public abstract class AbstractDefaultValueSelectionStrategy
        implements DimensionDefaultValueSelectionStrategy {

    @Override
    /**
     * Formats the dimension default value for the capabilities file as ISO 8601 DateTime for TIME
     * and as a number for ELEVATION. Assumes that getDefaultValue returns a single value, classes
     * handling ranges have to override this method
     */
    public String getCapabilitiesRepresentation(
            ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        String retval = null;
        if (dimensionName.equals(ResourceInfo.TIME)) {
            Date dateValue =
                    (Date) getDefaultValue(resource, dimensionName, dimensionInfo, Date.class);
            if (dateValue == null) {
                return DimensionDefaultValueSetting.TIME_CURRENT;
            }
            retval = DateUtil.serializeDateTime(dateValue.getTime(), true);
        } else if (dimensionName.equals(ResourceInfo.ELEVATION)) {
            Number numberValue =
                    (Number) getDefaultValue(resource, dimensionName, dimensionInfo, Number.class);
            if (numberValue == null) {
                return "0";
            }
            retval = numberValue.toString();
        } else {
            Object value = getDefaultValue(resource, dimensionName, dimensionInfo, Object.class);
            retval = stringRepresentation(value);
        }
        return retval;
    }

    private String stringRepresentation(Object value) {
        if (value == null) return "";
        if (value instanceof Date) {
            Date dateValue = (Date) value;
            return DateUtil.serializeDateTime(dateValue.getTime(), true);
        } else {
            return value.toString();
        }
    }
}

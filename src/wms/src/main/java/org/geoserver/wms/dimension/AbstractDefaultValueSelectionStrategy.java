/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.util.Date;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.feature.type.DateUtil;

/**
 *
 * Abstract parent class for DefaultValueSelectionStrategy implementations.
 * 
 * @author Ilkka Rinne / Spatineo Inc. for Finnish Meteorological Institute
 *
 */
public abstract class AbstractDefaultValueSelectionStrategy implements DimensionDefaultValueSelectionStrategy {  
    
    @Override    
    /**
     * Formats the dimension default value for the capabilities file
     * as ISO 8601 DateTime for TIME and as a number for ELEVATION.
     */
    public String getCapabilitiesRepresentation(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        String retval = null;
        if (dimensionName.equals(ResourceInfo.TIME)){
            Date dateValue = getDefaultValue(resource, dimensionName, dimensionInfo, Date.class);
            retval = DateUtil.serializeDateTime(dateValue.getTime(), true);
        }
        else if (dimensionName.equals(ResourceInfo.ELEVATION)){
            Number numberValue = getDefaultValue(resource, dimensionName, dimensionInfo, Number.class);
            retval = numberValue.toString();
        }
        else {
            Object value = getDefaultValue(resource, dimensionName, dimensionInfo, Object.class);
            retval = value.toString();
        }
        return retval;
    }
}

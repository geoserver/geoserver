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
    
   /**
    * Returns the default value using casting rules to try to convert the
    * value to the desired type if the types don't match exactly.
    */
    @SuppressWarnings("unchecked")
    @Override
    public final <T> T getDefaultValue(ResourceInfo resource, String dimensionName,
            DimensionInfo dimension, Class<T> clz) {
        T retval = null;
        Object value = doGetDefaultValue(resource, dimensionName, dimension);
        if (value != null){
            if (clz.isAssignableFrom(value.getClass())){
                retval = (T)value;
            }
            else {
                if (value.getClass().equals(Long.class) ||value.getClass().equals(Integer.class)){
                    if (clz.equals(Double.class)){
                        retval = (T) Double.valueOf(value.toString());
                    }
                    else if (clz.equals(Float.class)){
                        retval = (T) Float.valueOf(value.toString());
                    }
                }
                else if (clz.equals(String.class)){
                    retval = (T)value.toString();
                }
                else {
                    throw new IllegalArgumentException("The default value for dimension of type "+value.getClass().getCanonicalName()+" cannot be assigned to "+clz.getCanonicalName());            
                }
            }
        }
        return retval;
    }
    
    protected abstract Object doGetDefaultValue(ResourceInfo resource, String dimensionName,
            DimensionInfo dimension);
    
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

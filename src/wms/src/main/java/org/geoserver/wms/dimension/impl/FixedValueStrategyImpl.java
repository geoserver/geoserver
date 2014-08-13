/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension.impl;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.dimension.AbstractDefaultValueSelectionStrategy;
import org.geotools.util.Converters;

/**
 * A default value strategy which always return the same fixed value.
 *  
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class FixedValueStrategyImpl extends AbstractDefaultValueSelectionStrategy {

    private Object value;
    
    /**
     * Constructs a 
     * @param value
     */
    public FixedValueStrategyImpl(Object value){
        this.value = value;
    }

    @Override
    public <T> T getDefaultValue(ResourceInfo resource, String dimensionName,
            DimensionInfo dimension, Class<T> clz) {
        return Converters.convert(this.value, clz);
    }
        
}
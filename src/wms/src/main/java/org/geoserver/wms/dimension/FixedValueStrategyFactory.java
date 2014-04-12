/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

/**
 * Implementations return instances of the DimensionDefaultValueStrategy
 * selected using a fixed value.
 * 
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 *
 */
public interface FixedValueStrategyFactory {
    
    /**
     * Returns a fixed default value strategy.
     * 
     * @param value
     * @return
     */
    public DimensionDefaultValueSelectionStrategy createFixedValueStrategy(Object value);
        
}

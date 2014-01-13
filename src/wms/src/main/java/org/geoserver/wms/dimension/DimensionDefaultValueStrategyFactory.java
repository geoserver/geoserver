/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;

public interface DimensionDefaultValueStrategyFactory {

    /**
     * Returns a dimension and resource specific strategy for selecting default values
     * for the given resource and dimension in GetMap requests.
     *  
     */
    public DimensionDefaultValueStrategy getStrategy(ResourceInfo resource, String dimensionName, DimensionInfo dimension);
      
}

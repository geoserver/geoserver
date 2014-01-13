/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.io.Serializable;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;

public interface DimensionDefaultValueStrategy extends Serializable {
    
    /**
     * Gets the actual value given the resource, the dimension, and the selected values for the already processed dimensions
     */
    
    public <T> T getDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dimension, Class<T> clz);
    /**
     * Returns the capabilities representation of the default value. For example, it could be "current"
     */
    public String getCapabilitiesRepresentation(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo);
    
}

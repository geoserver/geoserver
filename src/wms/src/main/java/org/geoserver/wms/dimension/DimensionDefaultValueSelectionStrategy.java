/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;

/**
 * Interface defining the API for different dimension default value providers. 
 * 
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 *
 */
public interface DimensionDefaultValueSelectionStrategy {
    
    /**
     * Gets the actual value given the resource, the dimension, and the selected values for the already processed dimensions
     */    
    public <T> T getDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dimension, Class<T> clz);

    /**
     * Returns the capabilities representation of the default value. For example, it could be "current"
     */
    public String getCapabilitiesRepresentation(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo);
    
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;

/**
 * A defines a factory for creating a Strategy for selecting the default values the given resource
 * and dimension combination.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public interface DimensionDefaultValueSelectionStrategyFactory {

    /**
     * Returns a dimension and resource specific strategy for selecting default values for the given
     * resource and dimension in GetMap requests.
     */
    public DimensionDefaultValueSelectionStrategy getStrategy(
            ResourceInfo resource, String dimensionName, DimensionInfo dimension);
}

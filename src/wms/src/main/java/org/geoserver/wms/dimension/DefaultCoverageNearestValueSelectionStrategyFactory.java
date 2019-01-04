/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import org.geoserver.wms.dimension.impl.CoverageNearestValueSelectionStrategyImpl;
import org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl;

/**
 * Default implementation of NearestValueStrategyFactory for coverage (raster) resources.
 *
 * <p>Extend / replace this class in the WMS application context binding of
 * coverageNearestValueStrategyFactory property of {@link
 * DimensionDefaultValueSelectionStrategyFactoryImpl} to change the implementations used for
 * selecting the dimension default values for coverage resources using the NEAREST strategy.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class DefaultCoverageNearestValueSelectionStrategyFactory
        implements NearestValueStrategyFactory {

    @Override
    public DimensionDefaultValueSelectionStrategy createNearestValueStrategy(Object toMatch) {
        return new CoverageNearestValueSelectionStrategyImpl(toMatch);
    }

    @Override
    public DimensionDefaultValueSelectionStrategy createNearestValueStrategy(
            Object toMatch, String fixedCapabilitiesValue) {
        return new CoverageNearestValueSelectionStrategyImpl(toMatch, fixedCapabilitiesValue);
    }
}

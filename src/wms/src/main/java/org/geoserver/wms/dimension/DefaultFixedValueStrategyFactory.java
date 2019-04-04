/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl;
import org.geoserver.wms.dimension.impl.FixedValueStrategyImpl;

/**
 * Default implementation of FixedValueStrategyFactory.
 *
 * <p>Extend / replace this class in the WMS application context binding of
 * fixedValueStrategyFactory property of {@link DimensionDefaultValueSelectionStrategyFactoryImpl}
 * to change the implementations used for selecting the dimension default values FIXED strategy.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class DefaultFixedValueStrategyFactory implements FixedValueStrategyFactory {

    @Override
    public DimensionDefaultValueSelectionStrategy createFixedValueStrategy(Object value) {
        return new FixedValueStrategyImpl(value);
    }

    @Override
    public DimensionDefaultValueSelectionStrategy createFixedValueStrategy(
            Object value, String fixedCapabilitiesValue) {
        return new FixedValueStrategyImpl(value, fixedCapabilitiesValue);
    }
}

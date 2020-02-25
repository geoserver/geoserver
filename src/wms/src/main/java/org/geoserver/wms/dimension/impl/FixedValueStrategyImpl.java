/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension.impl;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.dimension.AbstractDefaultValueSelectionStrategy;
import org.geotools.util.Converters;
import org.geotools.util.Range;

/**
 * A default value strategy which always return the same fixed value.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class FixedValueStrategyImpl extends AbstractDefaultValueSelectionStrategy {

    private Object value;
    private String fixedCapabilitiesValue;

    /** Constructs a */
    public FixedValueStrategyImpl(Object value) {
        this.value = value;
    }

    public FixedValueStrategyImpl(Object value, String fixedCapabilitiesValue) {
        this.value = value;
        this.fixedCapabilitiesValue = fixedCapabilitiesValue;
    }

    @Override
    public Object getDefaultValue(
            ResourceInfo resource, String dimensionName, DimensionInfo dimension, Class clz) {
        if (value instanceof Range) {
            Range r = (Range) value;
            if (clz.isAssignableFrom(r.getElementClass())) {
                return r;
            } else {
                Comparable min = (Comparable) Converters.convert(r.getMinValue(), clz);
                Comparable max = (Comparable) Converters.convert(r.getMaxValue(), clz);
                return new Range(clz, min, max);
            }
        } else {
            return Converters.convert(this.value, clz);
        }
    }

    @Override
    public String getCapabilitiesRepresentation(
            ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        if (fixedCapabilitiesValue != null) {
            return this.fixedCapabilitiesValue;
        } else {
            return super.getCapabilitiesRepresentation(resource, dimensionName, dimensionInfo);
        }
    }
}

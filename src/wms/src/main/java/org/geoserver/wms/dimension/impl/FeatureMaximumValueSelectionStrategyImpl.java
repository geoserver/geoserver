/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension.impl;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.dimension.AbstractFeatureAttributeVisitorSelectionStrategy;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.util.Converters;

/**
 * Default implementation for selecting the default values for dimensions of feature (vector)
 * resources using the maximum domain value strategy.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class FeatureMaximumValueSelectionStrategyImpl
        extends AbstractFeatureAttributeVisitorSelectionStrategy {

    /** Default constructor. */
    public FeatureMaximumValueSelectionStrategyImpl() {}

    @Override
    public Object getDefaultValue(
            ResourceInfo resource, String dimensionName, DimensionInfo dimension, Class clz) {
        final MaxVisitor max = new MaxVisitor(dimension.getAttribute());
        CalcResult res = getCalculatedResult((FeatureTypeInfo) resource, dimension, max);
        if (res.equals(CalcResult.NULL_RESULT)) {
            return null;
        } else {
            return Converters.convert(max.getMax(), clz);
        }
    }
}

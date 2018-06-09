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
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.util.Converters;

/**
 * Default implementation for selecting the default values for dimensions of feature (vector)
 * resources using the minimum domain value strategy.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class FeatureMinimumValueSelectionStrategyImpl
        extends AbstractFeatureAttributeVisitorSelectionStrategy {

    /** Default constructor. */
    public FeatureMinimumValueSelectionStrategyImpl() {}

    @Override
    public Object getDefaultValue(
            ResourceInfo resource, String dimensionName, DimensionInfo dimension, Class clz) {
        final MinVisitor min = new MinVisitor(dimension.getAttribute());
        CalcResult res = getCalculatedResult((FeatureTypeInfo) resource, dimension, min);
        if (res.equals(CalcResult.NULL_RESULT)) {
            return null;
        } else {
            return Converters.convert(min.getMin(), clz);
        }
    }
}

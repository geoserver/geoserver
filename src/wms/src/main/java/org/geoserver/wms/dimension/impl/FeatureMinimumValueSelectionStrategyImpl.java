/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
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

/**
 * Default implementation for selecting the default values for dimensions of 
 * feature (vector) resources using the minimum domain value strategy.
 *  
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 *
 */
public class FeatureMinimumValueSelectionStrategyImpl extends
        AbstractFeatureAttributeVisitorSelectionStrategy {

    /**
     * Default constructor.
     */
    public FeatureMinimumValueSelectionStrategyImpl() {
    }

    @Override
    protected Object doGetDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        final MinVisitor max = new MinVisitor(dimensionInfo.getAttribute());
        CalcResult res = getCalculatedResult((FeatureTypeInfo) resource, dimensionInfo, max);
        if (res.equals(CalcResult.NULL_RESULT)) {
            return null;
        } else {
            return max.getMin();
        }
    }              
}
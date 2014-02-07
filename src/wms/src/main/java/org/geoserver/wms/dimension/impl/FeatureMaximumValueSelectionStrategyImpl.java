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
import org.geotools.feature.visitor.MaxVisitor;

/**
 * Default implementation for selecting the default values for dimensions of 
 * feature (vector) resources using the maximum domain value strategy.
 *  
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 *
 */
public class FeatureMaximumValueSelectionStrategyImpl extends
        AbstractFeatureAttributeVisitorSelectionStrategy {

    /**
     * Default constructor.
     */
    public FeatureMaximumValueSelectionStrategyImpl() {
    }

    @Override
    protected Object doGetDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dim) {
        final MaxVisitor max = new MaxVisitor(dim.getAttribute());
        CalcResult res = getCalculatedResult((FeatureTypeInfo) resource, dim, max);
        if (res.equals(CalcResult.NULL_RESULT)) {
            return null;
        } else {
            return max.getMax();
        }
    }    
}
/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension.impl;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.dimension.AbstractFeatureAttributeVisitorSelectionStrategy;
import org.geoserver.wms.dimension.NearestVisitor;
import org.geotools.feature.visitor.CalcResult;

/**
 * Default implementation for selecting the default values for dimensions of 
 * feature (vector) resources using the nearest-domain-value-to-the-reference-value
 * strategy.
 *  
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 *
 */

public class FeatureNearestValueSelectionStrategyImpl extends
        AbstractFeatureAttributeVisitorSelectionStrategy {

    private Object toMatch;
    private String fixedCapabilitiesValue;
    
    /**
     * Default constructor.
     */
    public FeatureNearestValueSelectionStrategyImpl(Object toMatch){
        this(toMatch,null);
    }
        
    public FeatureNearestValueSelectionStrategyImpl(Object toMatch, String capabilitiesValue) {
        this.toMatch = toMatch;
        this.fixedCapabilitiesValue = capabilitiesValue;
    }

    @Override
    protected Object doGetDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        final NearestVisitor<Object> nearest = new NearestVisitor<Object>(dimensionInfo.getAttribute(),
                this.toMatch);

        CalcResult res = getCalculatedResult((FeatureTypeInfo) resource, dimensionInfo, nearest);
        if (res.equals(CalcResult.NULL_RESULT)) {
            return null;
        } else {
            return res.getValue();
        }
    }
    
    @Override
    public String getCapabilitiesRepresentation(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        if (fixedCapabilitiesValue != null){
            return this.fixedCapabilitiesValue;
        }
        else {
            return super.getCapabilitiesRepresentation(resource, dimensionName, dimensionInfo);
        }
    }
}
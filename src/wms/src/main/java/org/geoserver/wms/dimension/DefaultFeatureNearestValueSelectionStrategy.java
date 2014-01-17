/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.feature.visitor.CalcResult;

class DefaultFeatureNearestValueSelectionStrategy extends
        AbstractFeatureAttributeVisitorSelectionStrategy {

    /** serialVersionUID */
    private static final long serialVersionUID = 7974448014127508563L;
    private Object toMatch;
    private String fixedCapabilitiesValue;

    
    /**
     * Default constructor.
     */
    public DefaultFeatureNearestValueSelectionStrategy(Object toMatch){
        this(toMatch,null);
    }
    
    public DefaultFeatureNearestValueSelectionStrategy(Object toMatch, String capabilitiesValue) {
        this.toMatch = toMatch;
        this.fixedCapabilitiesValue = capabilitiesValue;
    }

    @Override
    protected Object doGetDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        final NearestVisitor<Object> nearest = new NearestVisitor<Object>(dimensionInfo.getAttribute(),
                this.toMatch);

        CalcResult res = getSelectedValue((FeatureTypeInfo) resource, dimensionInfo, nearest);
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
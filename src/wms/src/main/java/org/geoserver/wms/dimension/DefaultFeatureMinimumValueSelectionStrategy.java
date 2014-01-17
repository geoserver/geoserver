/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MinVisitor;

class DefaultFeatureMinimumValueSelectionStrategy extends
        AbstractFeatureAttributeVisitorSelectionStrategy {

    /** serialVersionUID */
    private static final long serialVersionUID = -1435277659164580396L;

    /**
     * Default constructor.
     */
    public DefaultFeatureMinimumValueSelectionStrategy() {
    }

    @Override
    protected Object doGetDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        final MinVisitor max = new MinVisitor(dimensionInfo.getAttribute());
        CalcResult res = getSelectedValue((FeatureTypeInfo) resource, dimensionInfo, max);
        if (res.equals(CalcResult.NULL_RESULT)) {
            return null;
        } else {
            return max.getMin();
        }
    }              
}
/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MaxVisitor;

class DefaultFeatureMaximumValueSelectionStrategy extends
        AbstractFeatureAttributeVisitorSelectionStrategy {

    /** serialVersionUID */
    private static final long serialVersionUID = -4662167723177252198L;

    /**
     * Default constructor.
     */
    public DefaultFeatureMaximumValueSelectionStrategy() {
    }

    @Override
    protected Object doGetDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dim) {
        final MaxVisitor max = new MaxVisitor(dim.getAttribute());
        CalcResult res = getSelectedValue((FeatureTypeInfo) resource, dim, max);
        if (res.equals(CalcResult.NULL_RESULT)) {
            return null;
        } else {
            return max.getMax();
        }
    }    
}
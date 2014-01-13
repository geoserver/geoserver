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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dim, Class<T> clz) {
        final MaxVisitor max = new MaxVisitor(dim.getAttribute());
        CalcResult res = getSelectedValue((FeatureTypeInfo) resource, dim, max);
        if (res.equals(CalcResult.NULL_RESULT)) {
            return null;
        } else {
            Comparable<?> value = max.getMax();
            if (clz.isAssignableFrom(value.getClass())){
                return (T)value;
            }
            else {
                throw new IllegalArgumentException("The default value for dimension of type "+value.getClass().getCanonicalName()+" cannot be assigned to "+clz.getCanonicalName());
            }
        }
    }    
}
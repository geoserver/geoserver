/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;

class FixedValueStrategy extends AbstractCapabilitiesDefaultValueSelectionStrategy {
    /** serialVersionUID */
    private static final long serialVersionUID = 3698979702097275605L;
    private Object value;
    
    public FixedValueStrategy(Object value){
        this.value = value;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getDefaultValue(ResourceInfo resource, String dimensionName,
            DimensionInfo dimension, Class<T> clz) {
        if (this.value != null){
            if (clz.isAssignableFrom(this.value.getClass())){
                return (T)value;
            }
            else {
                throw new IllegalArgumentException("The default value for dimension of type "+this.value.getClass().getCanonicalName()+" cannot be assigned to "+clz.getCanonicalName());
            }                
        }
        else {
            return null;
        }
    }        
}
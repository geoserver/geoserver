package org.geoserver.wms.dimension;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;

public abstract class AbstractDefaultValueStrategy implements DimensionDefaultValueStrategy {

    /** serialVersionUID */
    private static final long serialVersionUID = 5184468692888222473L;

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T getDefaultValue(ResourceInfo resource, String dimensionName,
            DimensionInfo dimension, Class<T> clz) {
        T retval = null;
        Object value = doGetDefaultValue(resource, dimensionName, dimension);
        if (value != null){
            if (clz.isAssignableFrom(value.getClass())){
                retval = (T)value;
            }
            else {
                if (value.getClass().equals(Long.class) ||value.getClass().equals(Integer.class)){
                    if (clz.equals(Double.class)){
                        retval = (T) Double.valueOf(value.toString());
                    }
                    else if (clz.equals(Float.class)){
                        retval = (T) Float.valueOf(value.toString());
                    }
                }
                else if (clz.equals(String.class)){
                    retval = (T)value.toString();
                }
                else {
                    throw new IllegalArgumentException("The default value for dimension of type "+value.getClass().getCanonicalName()+" cannot be assigned to "+clz.getCanonicalName());            
                }
            }
        }
        return retval;
    }
    
    protected abstract Object doGetDefaultValue(ResourceInfo resource, String dimensionName,
            DimensionInfo dimension);
    
}

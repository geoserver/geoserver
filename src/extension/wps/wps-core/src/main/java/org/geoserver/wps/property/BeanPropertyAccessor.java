/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.property;

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.beanutils.BeanUtils;
import org.geoserver.wps.WPSException;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.util.Converters;

/** Extracts a property from any JavaBean using Spring bean utilities */
public class BeanPropertyAccessor implements PropertyAccessor {

    @Override
    public boolean canHandle(Object object, String xpath, Class<?> target) {
        return true;
    }

    @Override
    public <T> T get(Object object, String xpath, Class<T> target) throws IllegalArgumentException {
        Object value;
        try {
            value = BeanUtils.getProperty(object, xpath);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new WPSException("Failed to retrieve property " + xpath + " in " + object, e);
        }
        if (target != null) {
            return Converters.convert(value, target);
        } else {
            return (T) value;
        }
    }

    @Override
    public <T> void set(Object object, String xpath, T value, Class<T> target)
            throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }
}

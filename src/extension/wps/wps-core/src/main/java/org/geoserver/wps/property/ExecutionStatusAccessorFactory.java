/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.property;

import org.geoserver.wps.executor.ExecutionStatus;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.geotools.util.factory.Hints;

/** Property accessor for GeoServer ExecutionStatus beans */
public class ExecutionStatusAccessorFactory implements PropertyAccessorFactory {

    private static final BeanPropertyAccessor INSTANCE = new BeanPropertyAccessor();

    @Override
    public PropertyAccessor createPropertyAccessor(
            Class<?> type, String xpath, Class<?> target, Hints hints) {
        if (ExecutionStatus.class.isAssignableFrom(type)) {
            return INSTANCE;
        }
        return null;
    }
}

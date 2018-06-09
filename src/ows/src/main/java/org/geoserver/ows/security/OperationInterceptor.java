/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.security;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.geoserver.platform.Operation;

public interface OperationInterceptor {
    public Object invoke(
            Operation opDescriptor, Method operation, Object serviceBean, Object[] parameters)
            throws InvocationTargetException, IllegalArgumentException, IllegalAccessException;
}

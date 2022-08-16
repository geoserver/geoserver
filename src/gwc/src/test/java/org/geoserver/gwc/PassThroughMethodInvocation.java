/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInvocation;

public class PassThroughMethodInvocation implements MethodInvocation {
    private Method method;
    private Object[] arguments;
    private Object targetObject;

    public PassThroughMethodInvocation(Object targetObject, Method method, Object... arguments) {
        this.targetObject = targetObject;
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public Object proceed() throws Throwable {
        return method.invoke(targetObject, arguments);
    }

    @Override
    public Object getThis() {
        return targetObject;
    }

    @Override
    public AccessibleObject getStaticPart() {
        return null;
    }
}

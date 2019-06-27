/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import java.lang.reflect.Method;
import org.springframework.core.MethodParameter;

/** A simple carrier for the controller method return value, used in the API dispatcher */
class ReturnValueMethodParameter extends MethodParameter {

    private final Object returnValue;

    public ReturnValueMethodParameter(Method method, Object returnValue) {
        super(method, -1);
        this.returnValue = returnValue;
    }

    protected ReturnValueMethodParameter(ReturnValueMethodParameter original) {
        super(original);
        this.returnValue = original.returnValue;
    }

    @Override
    public Class<?> getParameterType() {
        return (this.returnValue != null ? this.returnValue.getClass() : super.getParameterType());
    }

    @Override
    public ReturnValueMethodParameter clone() {
        return new ReturnValueMethodParameter(this);
    }
}

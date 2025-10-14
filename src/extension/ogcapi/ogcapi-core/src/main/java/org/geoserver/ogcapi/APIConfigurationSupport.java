/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.rest.RestConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

/**
 * Subclass of {@link WebMvcConfigurationSupport} adding support for dispatching
 * {@link DispatcherCallback#operationDispatched} events to callbacks
 */
// Not @Configuration on purpose, or it will interfere with the REST API.
// @Component is good enough to allow auto-wiring.
@Component
public class APIConfigurationSupport extends RestConfiguration {

    static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.ogcapi");

    List<DispatcherCallback> callbacks;

    class APIRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {
        @Override
        protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
            return new APIInvocableHandlerMethod(handlerMethod);
        }
    }

    class APIInvocableHandlerMethod extends ServletInvocableHandlerMethod {

        public APIInvocableHandlerMethod(HandlerMethod handlerMethod) {
            super(handlerMethod);
        }

        @Override
        protected Object doInvoke(Object... args) throws Exception {
            Request request = Dispatcher.REQUEST.get();
            if (request == null) return super.doInvoke(args);
            Operation operation =
                    new Operation(request.getRequest(), request.getServiceDescriptor(), getBridgedMethod(), args);
            operation = fireOperationDispatchedCallback(request, operation);
            request.setOperation(operation);
            try {
                return operation.getMethod().invoke(operation.getService().getService(), operation.getParameters());
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getTargetException();
                if (targetException instanceof RuntimeException exception1) {
                    throw exception1;
                } else if (targetException instanceof Error error) {
                    throw error;
                } else if (targetException instanceof Exception exception) {
                    throw exception;
                } else {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    @Override
    protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
        return new APIRequestMappingHandlerAdapter();
    }

    public List<DispatcherCallback> getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(List<DispatcherCallback> callbacks) {
        this.callbacks = callbacks;
    }

    // SHARE (or move to a callback handler/list class of sort?)
    Operation fireOperationDispatchedCallback(Request req, Operation op) {
        for (DispatcherCallback cb : callbacks) {
            Operation o = cb.operationDispatched(req, op);
            op = o != null ? o : op;
        }
        return op;
    }
}

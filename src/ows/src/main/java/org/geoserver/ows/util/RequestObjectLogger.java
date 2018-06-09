/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Logs the properties of a request object passed into an operation of an ows service.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class RequestObjectLogger implements MethodInterceptor {
    /** Logging instance */
    Logger logger;

    public RequestObjectLogger(String logPackage) {
        logger = org.geotools.util.logging.Logging.getLogger(logPackage);
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!logger.isLoggable(Level.INFO)) {
            return invocation.proceed();
        }
        StringBuffer log = new StringBuffer();
        log.append("\n" + "Request: " + invocation.getMethod().getName());

        if (invocation.getArguments().length > 0) {
            Object requestBean = null;

            for (int i = 0; i < invocation.getArguments().length; i++) {
                Object argument = (Object) invocation.getArguments()[i];

                if (isRequestObject(argument)) {
                    requestBean = (Object) argument;
                    break;
                }
            }

            if (requestBean != null) {
                log(requestBean, 1, log);
            }
        }

        Object result = invocation.proceed();
        logger.info(log.toString());

        return result;
    }

    /**
     * Determines if an object is the request object.
     *
     * <p>Subclasses should override this to do explict checks for the request object.
     */
    protected boolean isRequestObject(Object obj) {
        return true;
    }

    protected void log(Object object, int level, StringBuffer log) {
        ClassProperties props = OwsUtils.getClassProperties(object.getClass());

        for (String prop : props.properties()) {
            if ("class".equalsIgnoreCase(prop)) continue;
            Object value = OwsUtils.get(object, prop);
            log.append("\n");

            for (int i = 0; i < level; i++) log.append("\t");

            log.append(prop).append(" = ").append(value);
        }
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import java.util.Map;

import org.slf4j.MDC;

/**
 * Transfers the Request Trace ID (in the logging {@link MDC}) to another thread
 * 
 */
public class RequestTraceThreadLocalTransfer implements ThreadLocalTransfer {

    public static final String KEY = RequestTraceThreadLocalTransfer.class.getName() + "#threadLocal";

    @Override
    public void collect(Map<String, Object> storage) {
        Object state = MDC.get("requestTraceID");
        storage.put(KEY, state);
    }

    @Override
    public void apply(Map<String, Object> storage) {
        Object state = storage.get(KEY);
        MDC.put("requestTraceID", (String)state);;
    }

    @Override
    public void cleanup() {
        MDC.remove("requestTraceID");
    }

}

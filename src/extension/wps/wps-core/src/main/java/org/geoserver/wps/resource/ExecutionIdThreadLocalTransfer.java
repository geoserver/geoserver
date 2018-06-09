/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.util.Map;
import org.geoserver.threadlocals.ThreadLocalTransfer;

/**
 * Transfers the {@link WPSResourceManager} executionId thread local between threads
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ExecutionIdThreadLocalTransfer implements ThreadLocalTransfer {

    static final String KEY = ExecutionIdThreadLocalTransfer.class.getCanonicalName() + ".key";

    WPSResourceManager manager;

    public ExecutionIdThreadLocalTransfer(WPSResourceManager manager) {
        this.manager = manager;
    }

    @Override
    public void collect(Map<String, Object> storage) {
        String executionId = manager.getCurrentExecutionId();
        if (executionId != null) {
            storage.put(KEY, executionId);
        }
    }

    @Override
    public void apply(Map<String, Object> storage) {
        String executionId = (String) storage.get(KEY);
        if (executionId != null) {
            manager.setCurrentExecutionId(executionId);
        }
    }

    @Override
    public void cleanup() {
        manager.clearExecutionId();
    }
}

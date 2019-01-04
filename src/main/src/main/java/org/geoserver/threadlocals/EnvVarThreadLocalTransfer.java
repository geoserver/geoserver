/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import java.util.HashMap;
import java.util.Map;
import org.geotools.filter.function.EnvFunction;

/** Transfers the environment variables across thread pools */
public class EnvVarThreadLocalTransfer implements ThreadLocalTransfer {

    Map<String, Object> copy;

    @Override
    public void collect(Map<String, Object> storage) {
        copy = new HashMap<>(EnvFunction.getLocalValues());
    }

    @Override
    public void apply(Map<String, Object> storage) {
        EnvFunction.setLocalValues(copy);
    }

    @Override
    public void cleanup() {
        EnvFunction.clearLocalValues();
    }
}

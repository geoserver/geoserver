/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import java.util.Map;
import org.geoserver.security.AdminRequest;

/**
 * Transfers the {@link AdminRequest} management to another thread
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AdminRequestThreadLocalTransfer implements ThreadLocalTransfer {

    public static final String KEY = AdminRequest.class.getName() + "#threadLocal";

    @Override
    public void collect(Map<String, Object> storage) {
        Object state = AdminRequest.get();
        storage.put(KEY, state);
    }

    @Override
    public void apply(Map<String, Object> storage) {
        Object state = storage.get(KEY);
        AdminRequest.start(state);
    }

    @Override
    public void cleanup() {
        AdminRequest.finish();
    }
}

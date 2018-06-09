/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import java.util.Map;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;

/**
 * Transfers the LocalWorkspace management to another thread
 *
 * @author Andrea Aime - GeoSolutions
 */
public class LocalWorkspaceThreadLocalTransfer implements ThreadLocalTransfer {

    public static final String KEY = LocalWorkspace.class.getName() + "#threadLocal";

    @Override
    public void collect(Map<String, Object> storage) {
        WorkspaceInfo wi = LocalWorkspace.get();
        storage.put(KEY, wi);
    }

    @Override
    public void apply(Map<String, Object> storage) {
        WorkspaceInfo wi = (WorkspaceInfo) storage.get(KEY);
        LocalWorkspace.set(wi);
    }

    @Override
    public void cleanup() {
        LocalWorkspace.remove();
    }
}

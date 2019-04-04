/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geoserver.catalog.WorkspaceInfo;

/**
 * A thread local variable for a {@link WorkspaceInfo} that was specified as part of an ows request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LocalWorkspace {

    /** the workspace thread local */
    static ThreadLocal<WorkspaceInfo> workspace = new ThreadLocal<WorkspaceInfo>();

    public static void set(WorkspaceInfo ws) {
        workspace.set(ws);
    }

    public static WorkspaceInfo get() {
        return workspace.get();
    }

    public static void remove() {
        workspace.remove();
    }
}

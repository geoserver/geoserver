/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Collects all {@link ThreadLocalTransfer} found in the application context and applies them on
 * request. This object is not thread safe, it's meant to be instantiated when a transfer is
 * required To use it:
 *
 * <ul>
 *   <li>Create a {@link ThreadLocalsTransfer} in the request thread
 *   <li>Pass the object as state to the {@link Runnable}/{@link Callable} being run in a thread
 *       pool
 *   <li>As the {@link Runnable}/{@link Callable} starts its activity, invoke {@link #apply()}
 *   <li>In a finally block wrapping the whole activity of the {@link Runnable}/{@link Callable}
 *       call {@link #cleanup()} to clean the current Thread {@link ThreadLocal} variables
 * </ul>
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ThreadLocalsTransfer {

    Map<String, Object> storage = new HashMap<String, Object>();

    private List<ThreadLocalTransfer> transfers;

    /**
     * Starts and runs all registered {@link ThreadLocalsTransfer} objects and collects the
     * associated thread locals in the storage object
     */
    public ThreadLocalsTransfer() {
        transfers = GeoServerExtensions.extensions(ThreadLocalTransfer.class);
        for (ThreadLocalTransfer transfer : transfers) {
            transfer.collect(storage);
        }
    }

    /** Set the thread local values in the current thread */
    public void apply() {
        for (ThreadLocalTransfer transfer : transfers) {
            transfer.apply(storage);
        }
    }

    /** Clean up the thread locals in the current thread */
    public void cleanup() {
        for (ThreadLocalTransfer transfer : transfers) {
            transfer.cleanup();
        }
    }
}

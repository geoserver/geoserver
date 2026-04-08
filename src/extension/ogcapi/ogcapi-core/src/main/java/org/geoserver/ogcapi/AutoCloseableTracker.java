/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.google.common.annotations.VisibleForTesting;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Tracks {@link AutoCloseable} resources used in FreeMarker templates and ensures cleanup.
 *
 * <p>This class maintains a thread-local registry of {@link AutoCloseable} resources (such as closeable iterators or
 * streams) that are accessed during template rendering. After template processing completes, all tracked resources are
 * automatically closed to prevent resource leaks.
 *
 * <p>This is a generic tracking mechanism that works with any {@link AutoCloseable} resource. It only handles tracking
 * and cleanup; the actual wrapping of resources for template use is delegated to FreeMarker's standard wrappers via
 * {@link AutoCloseableTrackingWrapper}'s outer identity pattern.
 *
 * <p>Usage: Call {@link #track(AutoCloseable)} to register a resource, and {@link #purge()} after template processing
 * to close all tracked resources.
 *
 * @see AutoCloseableTrackingWrapper#wrap(Object)
 * @see AbstractHTMLMessageConverter#purgeIterators()
 */
class AutoCloseableTracker {

    static final Logger LOGGER = Logging.getLogger(AutoCloseableTracker.class);

    /** Thread local to track open AutoCloseable resources */
    private static final ThreadLocal<List<AutoCloseable>> CLOSEABLES = ThreadLocal.withInitial(LinkedList::new);

    @VisibleForTesting
    static final AtomicLong closed = new AtomicLong();

    /**
     * Registers an AutoCloseable resource for tracking and cleanup.
     *
     * @param closeable the resource to track
     */
    public static void track(AutoCloseable closeable) {
        CLOSEABLES.get().add(closeable);
    }

    /**
     * Closes all AutoCloseable resources that were tracked during template processing. Should be called after template
     * rendering completes.
     */
    @SuppressWarnings("PMD.CloseResource") // closing with direct call to close() instead of try-with-resources
    public static void purge() {
        List<AutoCloseable> closeables = CLOSEABLES.get();
        for (AutoCloseable c : closeables) {
            try {
                closed.incrementAndGet();
                c.close();
            } catch (Exception e) {
                LOGGER.log(
                        Level.WARNING, "Error closing resource: " + c.getClass().getSimpleName(), e);
            }
        }
        CLOSEABLES.remove();
    }
}

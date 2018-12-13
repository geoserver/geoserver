/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static java.lang.String.format;
import static org.geoserver.cluster.hazelcast.HazelcastUtil.localAddress;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.geoserver.cluster.Event;
import org.geoserver.config.GeoServer;

/**
 * Synchronizer that does a full geoserver reload on any event.
 *
 * <p>This synchronizer assumes a shared data directory among nodes in the cluster.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ReloadHzSynchronizer extends HzSynchronizer {

    /** lock during reload */
    protected AtomicBoolean eventLock = new AtomicBoolean();

    final ThreadPoolExecutor reloadService;

    public ReloadHzSynchronizer(HzCluster cluster, GeoServer gs) {
        super(cluster, gs);

        ThreadFactory threadFactory =
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("Hz-GeoServer-Reload-%d")
                        .build();
        // a thread pool executor operating out of a blocking queue with maximum of 1 element, which
        // discards execute requests if the queue is full
        reloadService =
                new ThreadPoolExecutor(
                        1, 1, 0L, TimeUnit.MILLISECONDS, getWorkQueue(), threadFactory);
    }

    BlockingQueue<Runnable> getWorkQueue() {
        return new LinkedBlockingQueue<>(1);
    }

    @Override
    protected Future<?> processEvent(Event event) {
        // submit task and return immediately. The task will be ignored if another one is already
        // scheduled
        try {
            return reloadService.submit(
                    () -> {
                        // lock during event processing
                        eventLock.set(true);
                        try {
                            gs.reload();
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Reload failed", e);
                        } finally {
                            eventLock.set(false);
                        }
                    });
        } catch (RejectedExecutionException e) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest(
                        format("%s - Reload in progress. Ignoring event %s", nodeId(), event));
            }
            return null;
        }
    }

    @Override
    protected void dispatch(Event e) {
        // check lock, if locked it means event in response to configuration reload, don't propagate
        if (eventLock.get()) {
            return;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(format("%s - Publishing event %s", nodeId(), e));
        }
        e.setSource(localAddress(cluster.getHz()));
        topic.publish(e);

        incCounter(getClass(), "dispatched");
    }
}

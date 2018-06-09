/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static java.lang.String.format;
import static org.geoserver.cluster.hazelcast.HazelcastUtil.localAddress;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.yammer.metrics.Metrics;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
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

    final ExecutorService reloadService;

    public ReloadHzSynchronizer(HzCluster cluster, GeoServer gs) {
        super(cluster, gs);

        ThreadFactory threadFactory =
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("Hz-GeoServer-Reload-%d")
                        .build();
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.DiscardPolicy();
        // a thread pool executor operating out of a blocking queue with maximum of 1 element, which
        // discards execute requests if the queue is full
        reloadService =
                new ThreadPoolExecutor(
                        1,
                        1,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>(1),
                        threadFactory,
                        rejectionHandler);
    }

    @Override
    protected void processEvent(Event event) throws Exception {
        // submit task and return immediately. The task will be ignored if another one is already
        // scheduled
        reloadService.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        // lock during event processing
                        eventLock.set(true);
                        try {
                            gs.reload();
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Reload failed", e);
                        } finally {
                            eventLock.set(false);
                        }
                    }
                });
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

        Metrics.newCounter(getClass(), "dispatched").inc();
    }
}

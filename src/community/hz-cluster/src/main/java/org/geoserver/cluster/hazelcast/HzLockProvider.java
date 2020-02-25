/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static java.lang.String.format;
import static org.geoserver.cluster.hazelcast.HazelcastUtil.nodeId;

import com.google.common.base.Preconditions;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.LockProviderInitializer;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource.Lock;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.InitializingBean;

/**
 * A {@link LockProvider} implementation on top of Hazelcast's {@link ILock} distributed locks.
 *
 * <p>A Spring bean of this type shall be configured in the project's {@code applicationContext.xml}
 * spring configuration file in order for {@link LockProviderInitializer} to find it, and for the
 * global configuration page (web UI) to find it and present it as a locking provider option.
 */
public class HzLockProvider implements LockProvider, InitializingBean {

    private static final Logger LOGGER = Logging.getLogger(HzLockProvider.class);

    private HzCluster cluster;

    /** {@code cluster} property to be set in {@code applicationContext.xml} */
    public void setCluster(HzCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Preconditions.checkNotNull(cluster, "HzCluster is not set");
    }

    /**
     * Returns a {@link java.util.concurrent.locks.Lock} decorator for a Hazelcast {@link ILock
     * distributed lock}
     *
     * <p>{@code path} is used as the key to acquire the distributed lock from {@link
     * HazelcastInstance#getLock(String)}.
     *
     * @see org.geoserver.platform.resource.LockProvider#acquire(java.lang.String)
     */
    @Override
    public Lock acquire(final String path) {
        Preconditions.checkState(
                cluster.isEnabled(),
                "Hazelcast cluster is not enabled. Either enable it or chose a different lock provider.");
        Preconditions.checkState(cluster.isRunning(), "Hazelcast cluster is not running");

        HazelcastInstance clusterInstance = cluster.getHz();

        // if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.info(
                format(
                        "%s - Acquiring distributed lock '%s' (Thread %s)",
                        nodeId(cluster), path, Thread.currentThread().getName()));
        // }
        ILock lock = clusterInstance.getLock(path);
        lock.lock();

        // if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.info(
                format(
                        "%s - Successfully acquired distributed lock '%s' (Thread %s)",
                        nodeId(cluster), path, Thread.currentThread().getName()));
        // }
        Preconditions.checkState(lock.isLockedByCurrentThread());
        return new LockAdapter(lock, path, cluster);
    }

    private static class LockAdapter implements Lock {

        private ILock lock;

        private String path;

        private HzCluster cluster;

        public LockAdapter(ILock lock, String path, HzCluster cluster) {
            this.lock = lock;
            this.path = path;
            this.cluster = cluster;
        }

        @Override
        public void release() {
            if (null == lock || !lock.isLocked()) {
                LOGGER.info(
                        format(
                                "%s - Distributed lock is already unlocked '%s' (Thread %s)",
                                nodeId(cluster), path, Thread.currentThread().getName()));
                return;
            }
            try {
                LOGGER.info(
                        format(
                                "%s - Releasing distributed lock '%s' (Thread %s)",
                                nodeId(cluster), path, Thread.currentThread().getName()));
                this.lock.unlock();
                this.lock = null;
                LOGGER.info(
                        format(
                                "%s - Successfully released distributed lock '%s' (Thread %s)",
                                nodeId(cluster), path, Thread.currentThread().getName()));
            } catch (RuntimeException e) {
                LOGGER.log(
                        Level.SEVERE,
                        format(
                                "%s - Error releasing distributed lock '%s' (Thread %s): %s",
                                nodeId(cluster),
                                path,
                                Thread.currentThread().getName(),
                                e.getMessage()),
                        e);
                throw e;
            }
        }
    }
}

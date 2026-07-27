/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.seed;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.threadlocals.ThreadLocalsTransfer;
import org.geowebcache.seed.SeederThreadPoolExecutor;

/**
 * Combination of {@link org.geoserver.wms.ThreadLocalTransferExecutor} and {@link SeederThreadPoolExecutor} so that
 * Integrated GWC can preserve the current authentication when seeding.
 *
 * <p>Will perform thread locals transfer using Spring registered {@link org.geoserver.threadlocals.ThreadLocalTransfer}
 * when starting a new task, and will clean up thread locals after completing that task. Otherwise, behaves the same as
 * {@link SeederThreadPoolExecutor}.
 *
 * <p>Only intended for use as a thread pool for Integrated GWC seeding tasks.
 */
public class SeederThreadLocalTransferExecutor extends SeederThreadPoolExecutor {

    /**
     * Creates the seeder thread pool using configuration from the given {@link GWCConfigPersister}. Pool sizes are read
     * from gwc-gs.xml and can be overridden by system properties / environment variables.
     *
     * @param configPersister provides the GeoServer GWC configuration (gwc-gs.xml)
     */
    public SeederThreadLocalTransferExecutor(GWCConfigPersister configPersister) {
        super(
                configPersister.getConfig().getSeederCorePoolSize() != null
                        ? configPersister.getConfig().getSeederCorePoolSize()
                        : DEFAULT_CORE_POOL_SIZE,
                configPersister.getConfig().getSeederMaxPoolSize() != null
                        ? configPersister.getConfig().getSeederMaxPoolSize()
                        : DEFAULT_MAX_POOL_SIZE);
    }

    /** Copied from {@link org.geoserver.wms.ThreadLocalTransferExecutor} */
    @Override
    public Future<?> submit(Runnable task) {
        ThreadLocalsTransfer threadLocalTransfer = new ThreadLocalsTransfer();
        return super.submit(() -> {
            threadLocalTransfer.apply();
            try {
                task.run();
            } finally {
                threadLocalTransfer.cleanup();
            }
        });
    }

    /** Copied from {@link org.geoserver.wms.ThreadLocalTransferExecutor} */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        ThreadLocalsTransfer threadLocalTransfer = new ThreadLocalsTransfer();
        return super.submit(() -> {
            threadLocalTransfer.apply();
            try {
                return task.call();
            } finally {
                threadLocalTransfer.cleanup();
            }
        });
    }

    /** Copied from {@link org.geoserver.wms.ThreadLocalTransferExecutor} */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        ThreadLocalsTransfer threadLocalTransfer = new ThreadLocalsTransfer();
        return super.submit(() -> {
            threadLocalTransfer.apply();
            try {
                task.run();
                return result;
            } finally {
                threadLocalTransfer.cleanup();
            }
        });
    }
}

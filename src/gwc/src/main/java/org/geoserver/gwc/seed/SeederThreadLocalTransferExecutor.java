/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.seed;

import java.util.concurrent.*;
import org.geoserver.threadlocals.ThreadLocalsTransfer;
import org.geowebcache.seed.SeederThreadPoolExecutor;

/**
 * Combination of {@link org.geoserver.wms.ThreadLocalTransferExecutor} and {@link
 * SeederThreadPoolExecutor} so that Integrated GWC can preserve the current authentication when
 * seeding.
 *
 * <p>Will perform thread locals transfer using Spring registered {@link
 * org.geoserver.threadlocals.ThreadLocalTransfer} when starting a new task, and will clean up
 * thread locals after completing that task. Otherwise, behaves the same as {@link
 * SeederThreadPoolExecutor}.
 *
 * <p>Only intended for use as a thread pool for Integrated GWC seeding tasks.
 */
public class SeederThreadLocalTransferExecutor extends SeederThreadPoolExecutor {

    public SeederThreadLocalTransferExecutor(int corePoolSize, int maxPoolSize) {
        super(corePoolSize, maxPoolSize);
    }

    /** Copied from {@link org.geoserver.wms.ThreadLocalTransferExecutor} */
    @Override
    public Future<?> submit(Runnable task) {
        ThreadLocalsTransfer threadLocalTransfer = new ThreadLocalsTransfer();
        return super.submit(
                () -> {
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
        return super.submit(
                () -> {
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
        return super.submit(
                () -> {
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

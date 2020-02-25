/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.geoserver.threadlocals.ThreadLocalsTransfer;

/**
 * An equivalent to {@link Executors#newCachedThreadPool()} that will also perform thread locals
 * transfer using Spring registered {@link org.geoserver.threadlocals.ThreadLocalTransfer} when
 * starting a new task
 */
class ThreadLocalTransferExecutor extends ThreadPoolExecutor {

    public ThreadLocalTransferExecutor() {
        super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

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

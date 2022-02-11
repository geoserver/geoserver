/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * A thread holding both a latch to start a task and a latch to wait for the thread to complete.
 * Accepts a BiFunction where one of the parameter is the ResourceLoader and execute it.
 *
 * @param <P> the Parameter type.
 * @param <R> the Result type.
 */
public class ResourcePoolLatchedThread<P, R> extends Thread {

    private CountDownLatch taskLatch;
    private CountDownLatch doneLatch;
    private ResourcePool resourcePool;
    private PoolBiFunction<P, R> function;
    private R result;
    private P funParam;
    private List<Exception> errors = new ArrayList<>();

    public ResourcePoolLatchedThread(
            CountDownLatch taskLatch,
            CountDownLatch doneLatch,
            ResourcePool pool,
            P funParam,
            PoolBiFunction<P, R> function) {
        this.taskLatch = taskLatch;
        this.doneLatch = doneLatch;
        this.resourcePool = pool;
        this.funParam = funParam;
        this.function = function;
    }

    @Override
    public void run() {
        try {
            taskLatch.await();
            result = function.apply(resourcePool, funParam);
            doneLatch.countDown();
        } catch (Exception e) {
            errors.add(e);
        }
    }

    /**
     * Get the result of the task.
     *
     * @return
     */
    public R getResult() {
        return this.result;
    }

    /**
     * Get the list of exception occurred while performing the task.
     *
     * @return
     */
    public List<Exception> getErrors() {
        return errors;
    }

    /**
     * A simple functional interface where one of the two parameters is a ResourcePool instance.
     *
     * @param <P> the type of the Parameter.
     * @param <R> the type of the Result.
     */
    @FunctionalInterface
    public interface PoolBiFunction<P, R> {

        /**
         * Execute the function.
         *
         * @param pool the resource pool.
         * @param p the parameter.
         * @return the result of the function.
         * @throws IOException
         */
        R apply(ResourcePool pool, P p) throws IOException;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * A task queue that groups tasks by key and ensures that tasks with same key execute serially.
 *
 * @author Justin Deoliveira, OpenGeo
 * @param <K> The key type.
 */
public class PipeliningTaskQueue<K> implements Runnable {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    volatile ConcurrentHashMap<K, Queue<Pipelineable<K>>> pipelines;
    ScheduledExecutorService executor;
    ExecutorService tasks;

    public PipeliningTaskQueue() {
        pipelines = new ConcurrentHashMap();
        tasks = Executors.newCachedThreadPool();
    }

    public void start() {
        executor = Executors.newScheduledThreadPool(4);
        executor.scheduleAtFixedRate(this, 0, 10, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this, 0, 10, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this, 0, 10, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this, 0, 10, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        executor.shutdown();
        executor = null;

        tasks.shutdown();
        tasks = null;
    }

    public void execute(K key, Runnable task) {
        execute(key, task, "");
    }

    public void execute(K key, Runnable task, String desc) {
        Queue<Pipelineable<K>> pipeline = pipelines.get(key);
        if (pipeline == null) {
            synchronized (this) {
                pipeline = pipelines.get(key);
                if (pipeline == null) {
                    pipeline = new ConcurrentLinkedQueue();
                    Queue<Pipelineable<K>> other = pipelines.putIfAbsent(key, pipeline);
                    if (other != null) pipeline = other;
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Queuing task into pipeline " + key);
        }
        pipeline.add(new Pipelineable<K>(key, task));
    }

    public void clear(K key) {
        pipelines.remove(key);
    }

    public void shutdown() {
        executor.shutdown();
        tasks.shutdown();
    }

    public void run() {

        for (Queue<Pipelineable<K>> pipeline : pipelines.values()) {
            Pipelineable<K> job = pipeline.peek();
            if (job != null) {
                if (!job.lock.tryLock()) continue; // another thread already handling this job

                if (job.future != null) {
                    // job has been submitted, if it is done remove it from
                    // the queue
                    if (job.future.isDone()) {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Removing task from queue " + job.key);
                        }
                        pipeline.remove();
                    }
                } else {
                    // start the job
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Executing task in queue " + job.key);
                    }
                    job.future = tasks.submit(job.task);
                }

                job.lock.unlock();
            }
        }
    }

    public class Pipelineable<K> {

        K key;
        Runnable task;
        Future<?> future;
        Lock lock;
        String desc;

        public Pipelineable(K key, Runnable task) {
            this.key = key;
            this.task = task;
            this.lock = new ReentrantLock();
        }
    }
}

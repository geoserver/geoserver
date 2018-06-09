/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JobQueue {

    /** job id counter */
    AtomicLong counter = new AtomicLong();

    /** recent jobs */
    ConcurrentHashMap<Long, Task<?>> jobs = new ConcurrentHashMap<Long, Task<?>>();

    /** job runner */
    // ExecutorService pool = Executors.newCachedThreadPool();
    ExecutorService pool =
            new ThreadPoolExecutor(
                    0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()) {
                protected <T extends Object> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                    if (callable instanceof Job) {
                        return new Task((Job) callable);
                    }
                    return super.newTaskFor(callable);
                };

                protected void beforeExecute(Thread t, Runnable r) {
                    if (t != null && r instanceof Task) {
                        ((Task) r).started();
                    }
                };

                protected void afterExecute(Runnable r, Throwable t) {
                    if (t != null && r instanceof Task) {
                        ((Task) r).setError(t);
                    }
                };
            };

    /** job cleaner */
    ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    {
        cleaner.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        List<Long> toremove = new ArrayList<Long>();
                        for (Map.Entry<Long, Task<?>> e : jobs.entrySet()) {
                            if (e.getValue().isCancelled()
                                    || (e.getValue().isDone() && e.getValue().isRecieved())) {
                                toremove.add(e.getKey());
                            }
                        }
                        for (Long l : toremove) {
                            jobs.remove(l);
                        }
                    }
                },
                60,
                60,
                TimeUnit.SECONDS);
    }

    public Long submit(Job<?> task) {
        Long jobid = counter.getAndIncrement();
        Task t = (Task) pool.submit(task);
        t.setId(jobid);

        jobs.put(jobid, t);
        return jobid;
    }

    public Task<?> getTask(Long jobid) {
        Task<?> t = jobs.get(jobid);
        t.recieve();
        return t;
    }

    public List<Task<?>> getTasks() {
        return new ArrayList<Task<?>>(jobs.values());
    }

    public void shutdown() {
        cleaner.shutdownNow();
        pool.shutdownNow();
    }
}

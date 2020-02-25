/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.job;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.Importer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

public class JobQueue {

    static Logger LOGGER = Logging.getLogger(JobQueue.class);

    /** job id counter */
    AtomicLong counter = new AtomicLong();

    /** recent jobs */
    ConcurrentHashMap<Long, Task<?>> jobs = new ConcurrentHashMap<Long, Task<?>>();

    /** job runner */
    ThreadPoolExecutor pool =
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
                                    || (e.getValue()
                                            .isDone() /* AF: This condition is never verified ?!? && e.getValue().isRecieved() */)) {
                                try {
                                    ImportContext context = (ImportContext) e.getValue().get();

                                    if (context.getState() == ImportContext.State.COMPLETE
                                            && context.isEmpty()) {
                                        context.unlockUploadFolder(context.getUploadDirectory());
                                        toremove.add(e.getKey());
                                    }
                                } catch (Exception ex) {
                                    LOGGER.log(Level.INFO, ex.getMessage(), ex);
                                }
                            }
                        }
                        for (Long l : toremove) {
                            jobs.remove(l);
                        }

                        final Importer importer = GeoServerExtensions.bean(Importer.class);
                        File[] files = importer.getUploadRoot().listFiles();
                        if (files != null) {
                            for (File f : files) {
                                if (f.isDirectory() && new File(f, ".clean-me").exists()) {
                                    try {
                                        IOUtils.delete(f);
                                    } catch (IOException e) {
                                        LOGGER.log(
                                                Level.WARNING,
                                                "It was not possible to cleanup Importer temporary folder "
                                                        + f,
                                                e);
                                    }
                                }
                            }
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

    public void setMaximumPoolSize(int maximumPoolSize) {
        pool.setMaximumPoolSize(maximumPoolSize);
    }

    public int getMaximumPoolSize() {
        return pool.getMaximumPoolSize();
    }
}

package org.geoserver.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.geoserver.platform.GeoServerExtensions;
import org.opengis.util.ProgressListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

public class LongTaskMonitor implements InitializingBean, DisposableBean {

    private static CustomizableThreadFactory longTaskTF = new CustomizableThreadFactory(
            "GeoServer LongTask Executor Thread-");
    static {
        longTaskTF.setDaemon(true);
    }

    private AtomicLong seq = new AtomicLong();

    private ThreadPoolExecutor longTasksExecutor;

    private List<ProgressListener> listeners;

    private Map<LongTask<?>, Long> currentTasks;

    private int maxThreads;

    public LongTaskMonitor() {
        maxThreads = Runtime.getRuntime().availableProcessors();
        currentTasks = new ConcurrentHashMap<LongTask<?>, Long>();
    }

    public static LongTaskMonitor get() {
        LongTaskMonitor instance = GeoServerExtensions.bean(LongTaskMonitor.class);
        if (null == instance) {
            throw new IllegalStateException("No LongTaskMonitor found in GeoServerExtensions");
        }
        return instance;
    }

    /**
     * @return the maxThreads
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * @param maxThreads
     *            the maxThreads to set
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        if (longTasksExecutor != null) {
            longTasksExecutor.setCorePoolSize(maxThreads);
            longTasksExecutor.setMaximumPoolSize(maxThreads);
        }
    }

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        longTasksExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads,
                longTaskTF);

    }

    /**
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() throws Exception {
        longTasksExecutor.shutdownNow();
        longTasksExecutor.awaitTermination(60, TimeUnit.SECONDS);
    }

    public <V> Future<V> dispatch(final LongTask<V> task) {

        if (listeners != null) {
            for (ProgressListener listener : listeners) {
                task.addProgressListener(listener);
            }
        }
        Long id = Long.valueOf(seq.incrementAndGet());
        this.currentTasks.put(task, id);
        Future<V> future = longTasksExecutor.submit(new Callable<V>() {

            /**
             * @see java.util.concurrent.Callable#call()
             */
            public V call() throws Exception {
                try {
                    return task.call();
                } finally {
                    // LongTaskMonitor.this.currentTasks.remove(task);
                }
            }
        });

        return future;
    }

    public Long getId(LongTask<?> task) {
        return currentTasks.get(task);
    }

    public List<LongTask<?>> getAllTasks() {
        ArrayList<LongTask<?>> list = new ArrayList<LongTask<?>>(currentTasks.keySet());
        return list;
    }

    public LongTask<?> getTask(Long id) {
        Map<LongTask<?>, Long> copy = new HashMap<LongTask<?>, Long>(currentTasks);
        for (Map.Entry<LongTask<?>, Long> e : copy.entrySet()) {
            if (e.getValue().equals(id)) {
                return e.getKey();
            }
        }
        return null;
    }

    public int getTaskCount() {
        return currentTasks.size();
    }

    public void removeTerminated(LongTask<?> task) {
        Assert.isTrue(task.isDone(), "Task is not terminated");
        currentTasks.remove(task);
    }

}

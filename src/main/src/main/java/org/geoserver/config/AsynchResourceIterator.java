/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.Filter;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;

/**
 * An iterator listing and mapping resources to a target object via a {@link ResourceMapper}. The
 * mapping is performed in a background thread pool, allowing to decouple CPU bound activities from
 * IO bound ones.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AsynchResourceIterator<T> implements Iterator<T>, Closeable {

    static final Logger LOGGER = Logging.getLogger(AsynchResourceIterator.class);

    static final int ASYNCH_RESOURCE_THREADS;

    static {
        String value = GeoServerExtensions.getProperty("org.geoserver.catalog.loadingThreads");
        if (value != null) {
            ASYNCH_RESOURCE_THREADS = Integer.parseInt(value);
        } else {
            // empirical determination after benchmarks, the value is related not the
            // available CPUs, but by how well the disk handles parallel access, different
            // disk subsystems will have a different optimal value
            ASYNCH_RESOURCE_THREADS = 4;
        }
    }

    /**
     * Maps a resource into a target object
     *
     * @author Andrea Aime - GeoSolutions
     */
    @FunctionalInterface
    public interface ResourceMapper<T> {

        T apply(Resource t) throws IOException;
    }

    /** Indicates the end of a blocking queue contents */
    static final Object TERMINATOR = new Object();

    /** The queue connecting the background threads loading the resources with the iterator */
    final BlockingQueue<Object> queue;

    /** The thread coordinating the background resource load */
    Thread thread;

    /** The current mapped resource for the iterator */
    T mapped;

    /** A flag used to mark completion */
    volatile boolean completed = false;

    /**
     * Builds an asynchronous {@link Resource} iterator
     *
     * @param root The directory resource
     * @param filter The filter getting specific child resources out of the root
     * @param mapper The mapper performing work on the resources found
     */
    @SuppressFBWarnings("SC_START_IN_CTOR")
    public AsynchResourceIterator(
            Resource root, Filter<Resource> filter, ResourceMapper<T> mapper) {
        // parallelize filtering (this is still synch'ed, cannot do anything in parallel with this)
        List<Resource> resources =
                root.list()
                        .parallelStream()
                        .filter(r -> filter.accept(r))
                        .collect(Collectors.toList());
        // decide if we want to have a background thread for loading resources, or not
        if (resources.size() > 1) {
            queue = new LinkedBlockingQueue<>(10000);
            // create a background thread allowing this constructor to return immediately and
            // start accumulating in the queue asynchronously
            thread =
                    new Thread(
                            () -> {
                                // parallelize IO in a local thread pool
                                ExecutorService executor =
                                        Executors.newFixedThreadPool(ASYNCH_RESOURCE_THREADS);
                                BlockingQueue<Object> sourceQueue =
                                        new LinkedBlockingQueue<>(resources);
                                for (int i = 0; i < ASYNCH_RESOURCE_THREADS; i++) {
                                    // each IO thread will exit when close is called or when the
                                    // terminator is
                                    // reached, we need one terminator per IO thread
                                    sourceQueue.add(TERMINATOR);
                                    executor.submit(
                                            () -> {
                                                try {
                                                    Object o;
                                                    while (!completed
                                                            && (o = sourceQueue.take())
                                                                    != TERMINATOR) {
                                                        Resource r = (Resource) o;
                                                        try {
                                                            T mapped = mapper.apply(r);
                                                            if (mapped != null) {
                                                                queue.put(mapped);
                                                            }
                                                        } catch (IOException e) {
                                                            LOGGER.log(
                                                                    Level.WARNING,
                                                                    "Failed to load resource '"
                                                                            + r.name()
                                                                            + "'",
                                                                    e);
                                                        }
                                                    }
                                                } catch (InterruptedException e) {
                                                    return;
                                                }
                                            });
                                }
                                // wait for everything to comlete and then add the terminator marker
                                try {
                                    executor.shutdown();
                                    executor.awaitTermination(
                                            Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                                    // add the terminator
                                    queue.put(TERMINATOR);
                                } catch (InterruptedException e) {
                                    LOGGER.log(
                                            Level.WARNING,
                                            "Failed to put the terminator in the queue",
                                            e);
                                }
                            },
                            "Loader" + root.name());
            thread.start();
        } else if (resources.size() == 1) {
            // don't start a thread for a single resource, there is no parallelism advantage
            queue = null;
            final Resource r = resources.get(0);
            try {
                mapped = mapper.apply(r);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load resource '" + r.name() + "'", e);
            } finally {
                completed = true;
            }
        } else {
            // nothing found
            queue = null;
            mapped = null;
            completed = true;
        }
    }

    @Override
    public boolean hasNext() {
        if (mapped != null) {
            return true;
        }
        // important that this is second to handle the "1 item" case correctly
        if (completed) {
            return false;
        }
        try {
            Object o = queue.take();
            if (o == TERMINATOR) {
                completed = true;
                return false;
            } else {
                mapped = (T) o;
                return true;
            }
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public T next() {
        if (hasNext()) {
            T result = mapped;
            mapped = null;
            return result;
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void close() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            completed = true;
            queue.clear();
        }
    }
}

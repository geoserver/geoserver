/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Active object (using a ScheduledExecutorService) used to watch file system for changes.
 *
 * <p>This implementation currently polls the file system and should be updated with Java 7
 * WatchService when available. The internal design is similar to WatchService, WatchKey and
 * WatchEvent in order to facilitate this transition.
 *
 * <p>This implementation makes a few concessions to being associated with ResourceStore, reporting
 * changes with resource paths rather than files.
 *
 * @author Jody Garnett (Boundless)
 */
public class FileSystemWatcher implements ResourceNotificationDispatcher, DisposableBean {

    private static final Logger LOGGER = Logging.getLogger(FileSystemWatcher.class);

    /** Change to file system */
    static class Delta {
        /** Watched directory where changes occurred */
        final File context;

        final Kind kind;

        /** Paths relative to the context directory that have been created */
        final List<String> created;

        /** Paths relative to the context directory that have been removed */
        final List<String> removed;

        /** Paths relative to the context directory that have been modified */
        final List<String> modified;

        public Delta(File context, Kind kind) {
            this.context = context;
            this.kind = kind;
            this.created = this.removed = this.modified = Collections.emptyList();
        }

        public Delta(
                File context,
                Kind kind,
                List<String> created,
                List<String> removed,
                List<String> modified) {
            this.context = context;
            this.kind = kind;
            this.created = created == null ? Collections.emptyList() : created;
            this.removed = removed == null ? Collections.emptyList() : removed;
            this.modified = modified == null ? Collections.emptyList() : modified;
        }

        public int size() {
            return created.size() + removed.size() + modified.size();
        }

        @Override
        public String toString() {
            return "Delta [context="
                    + context
                    + ", created="
                    + created
                    + ", removed="
                    + removed
                    + ", modified="
                    + modified
                    + "]";
        }
    }

    /** Record of a ResourceListener that wishes to be notified of changes to a path. */
    private class Watch implements Comparable<Watch> {
        /** File being watched */
        final File file;

        /** Path to use during notification */
        final String path;

        final List<ResourceListener> listeners = new CopyOnWriteArrayList<ResourceListener>();

        /** When last notification was sent */
        long last = 0;

        /** Used to track resource creation / deletion */
        boolean exsists;

        private Set<File> children = null;
        private long childrenLastModifiedMax = 0L;

        public Watch(File file, String path) {
            Objects.requireNonNull(file);
            Objects.requireNonNull(path);
            this.file = file;
            this.path = path;
            this.exsists = file.exists();
            this.last = exsists ? file.lastModified() : 0;
            if (file.isDirectory()) {
                this.children = loadDirectoryContents(file);
                this.childrenLastModifiedMax =
                        this.children
                                .parallelStream()
                                .mapToLong(File::lastModified)
                                .max()
                                .orElse(0L);
            }
        }

        private Set<File> loadDirectoryContents(File directory) {
            File[] files = directory.listFiles();
            if (files == null) {
                return new HashSet<>();
            }
            return Arrays.stream(files).collect(Collectors.toSet());
        }

        public void addListener(ResourceListener listener) {
            listeners.add(listener);
        }

        public void removeListener(ResourceListener listener) {
            listeners.remove(listener);
        }

        /** Path used for notification */
        public String getPath() {
            return path;
        }

        public List<ResourceListener> getListeners() {
            return listeners;
        }

        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + file.hashCode();
            result = prime * result + path.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Watch)) return false;

            Watch other = (Watch) obj;
            return file.equals(other.file) && path.equals(other.path);
        }

        @Override
        public String toString() {
            return "Watch [path="
                    + path
                    + ", file="
                    + file
                    + ", listeners="
                    + listeners.size()
                    + "]";
        }

        @Override
        public int compareTo(Watch other) {
            return path.compareTo(other.path);
        }

        public Delta changed(long now) {
            if (!file.exists()) {
                return watchedFileRemoved(now);
            }
            if (file.isFile()) {
                return simpleFileCheck();
            }
            // file exists and is a directory
            return pollDirectory();
        }

        private Delta pollDirectory() {
            final long fileModified = file.lastModified();
            final Kind kind;
            if (this.exsists) {
                kind = Kind.ENTRY_MODIFY;
            } else {
                this.children = new HashSet<>();
                kind = Kind.ENTRY_CREATE;
                this.exsists = true;
            }
            this.last = fileModified;

            long childrenMaxLastModified = this.childrenLastModifiedMax;

            final CompletableFuture<File[]> contentsFuture =
                    CompletableFuture.supplyAsync(() -> this.file.listFiles());

            final Map<Kind, List<String>> itemsByType = new EnumMap<>(Kind.class);
            // check for updates. Fastest path, no need to list current directory contents
            // nor check for removed files here
            Iterator<File> it = this.children.iterator();
            while (it.hasNext()) {
                File child = it.next();
                long lastModified = child.lastModified();
                if (0L == lastModified) { // removed
                    it.remove();
                    itemsByType
                            .computeIfAbsent(Kind.ENTRY_DELETE, k -> new ArrayList<>())
                            .add(child.getName());
                } else if (lastModified > this.childrenLastModifiedMax) {
                    childrenMaxLastModified = lastModified;
                    itemsByType
                            .computeIfAbsent(Kind.ENTRY_MODIFY, k -> new ArrayList<>())
                            .add(child.getName());
                }
            }

            final File[] contents = contentsFuture.join();
            if (null != contents) {
                // find new
                for (File child : contents) {
                    if (this.children.add(child)) {
                        childrenMaxLastModified =
                                Math.max(childrenMaxLastModified, child.lastModified());
                        itemsByType
                                .computeIfAbsent(Kind.ENTRY_CREATE, k -> new ArrayList<>())
                                .add(child.getName());
                    }
                }
            }

            if (itemsByType.isEmpty()) {
                // win only check of directory contents
                return null; // no change to directory contents
            }
            this.childrenLastModifiedMax = childrenMaxLastModified;
            List<String> created = itemsByType.get(Kind.ENTRY_CREATE);
            List<String> removed = itemsByType.get(Kind.ENTRY_DELETE);
            List<String> modified = itemsByType.get(Kind.ENTRY_MODIFY);
            Delta delta = new Delta(file, kind, created, removed, modified);
            return delta;
        }

        private Delta simpleFileCheck() {
            long fileModified = file.lastModified();
            if (fileModified > last || !exsists) {
                Kind kind = this.exsists ? Kind.ENTRY_MODIFY : Kind.ENTRY_CREATE;
                this.exsists = true;
                this.last = fileModified;
                return new Delta(file, kind);
            } else {
                return null; // no change!
            }
        }

        private Delta watchedFileRemoved(long now) {
            Delta delta = null;
            if (this.exsists) {
                if (this.children == null) {
                    // file has been deleted!
                    delta = new Delta(file, Kind.ENTRY_DELETE);
                } else {
                    // delete directory
                    List<String> deleted =
                            this.children.stream().map(File::getName).collect(Collectors.toList());
                    delta = new Delta(file, Kind.ENTRY_DELETE, null, deleted, null);
                }
                this.last = now;
                this.exsists = false;
                this.children = null;
            }
            return delta;
        }

        public boolean isMatch(File file, String path) {
            return this.file.equals(file) && this.path.equals(path);
        }
    }

    private ScheduledExecutorService pool;

    private final Function<String, File> fileExtractor;

    protected long lastmodified;

    CopyOnWriteArrayList<Watch> watchers = new CopyOnWriteArrayList<Watch>();

    /**
     * Note we have a single runnable here to review all outstanding Watch instances. The focus is
     * on using minimal system resources while we wait for Java 7 WatchService (to be more
     * efficient).
     */
    private Runnable sync =
            new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    for (Watch watch : watchers) {
                        if (watch.getListeners().isEmpty()) {
                            watchers.remove(watch);
                            continue;
                        }
                        final boolean directory = watch.file.isDirectory();
                        Level level = Level.FINER;
                        long start = System.nanoTime();
                        if (directory) LOGGER.log(level, "polling contents of " + watch.file);
                        Delta delta;
                        try {
                            delta = watch.changed(now);
                        } catch (RuntimeException e) {
                            LOGGER.log(Level.WARNING, "Error polling contents of " + watch.file, e);
                            return;
                        }
                        if (directory && LOGGER.isLoggable(level)) {
                            long ellapsedMicros =
                                    MICROSECONDS.convert(System.nanoTime() - start, NANOSECONDS);
                            long ellapsedMillis =
                                    MILLISECONDS.convert(ellapsedMicros, MICROSECONDS);
                            String unit = ellapsedMillis == 0L ? "us" : "ms";
                            long time = ellapsedMillis == 0L ? ellapsedMicros : ellapsedMillis;
                            LOGGER.log(
                                    level,
                                    String.format(
                                            "delta computed in %,d%s for %s",
                                            time, unit, watch.file));
                        }
                        if (delta != null) {
                            notify(watch, delta);
                        }
                    }
                }

                private void notify(Watch watch, Delta delta) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info(
                                String.format(
                                        "Notifying %s change on %s. Created: %,d, removed: %,d, modified: %,d",
                                        delta.kind,
                                        delta.context,
                                        delta.created.size(),
                                        delta.removed.size(),
                                        delta.modified.size()));
                    }
                    // do not call listeners on the watch thread, they may take a
                    // considerable amount of time to process the events
                    CompletableFuture.runAsync(
                            () -> {
                                /** Created based on created/removed/modified files */
                                List<ResourceNotification.Event> events =
                                        ResourceNotification.delta(
                                                watch.file,
                                                delta.created,
                                                delta.removed,
                                                delta.modified);

                                ResourceNotification notify =
                                        new ResourceNotification(
                                                watch.getPath(), delta.kind, watch.last, events);

                                for (ResourceListener listener : watch.getListeners()) {
                                    try {
                                        listener.changed(notify);
                                    } catch (Throwable t) {
                                        Logger logger =
                                                Logger.getLogger(
                                                        listener.getClass().getPackage().getName());
                                        logger.log(
                                                Level.FINE,
                                                "Unable to notify " + watch + ":" + t.getMessage(),
                                                t);
                                    }
                                }
                            });
                }
            };

    private ScheduledFuture<?> monitor;

    private TimeUnit unit = TimeUnit.SECONDS;

    private long delay = 5;

    private static CustomizableThreadFactory tFactory;

    static {
        tFactory = new CustomizableThreadFactory("FileSystemWatcher-");
        tFactory.setDaemon(true);
    }

    /**
     * FileSystemWatcher used to track file changes.
     *
     * <p>Internally a single threaded schedule executor is used to monitor files.
     */
    FileSystemWatcher(Function<String, File> fileExtractor) {
        Objects.requireNonNull(fileExtractor);
        this.pool = Executors.newSingleThreadScheduledExecutor(tFactory);
        this.fileExtractor = fileExtractor;
    }

    FileSystemWatcher() {
        this(path -> new File(path.replace('/', File.separatorChar)));
    }

    private Watch watch(File file, String path) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(path);

        for (Watch watch : watchers) {
            if (watch.isMatch(file, path)) {
                return watch;
            }
        }
        return null; // not found
    }

    public synchronized void addListener(String path, ResourceListener listener) {
        Objects.requireNonNull(path, "Path for notification is required");
        File file = fileExtractor.apply(path);
        Objects.requireNonNull(file, "File to watch is required");
        Watch watch = watch(file, path);
        if (watch == null) {
            watch = new Watch(file, path);
            watchers.add(watch);
            if (monitor == null) {
                monitor = pool.scheduleWithFixedDelay(sync, delay, delay, unit);
            }
        }
        watch.addListener(listener);
    }

    public synchronized boolean removeListener(String path, ResourceListener listener) {
        Objects.requireNonNull(path, "Path for notification is required");
        File file = fileExtractor.apply(path);
        Objects.requireNonNull(file, "File to watch is required");

        Watch watch = watch(file, path);
        boolean removed = false;
        if (watch != null) {
            watch.removeListener(listener);
            if (watch.getListeners().isEmpty()) {
                removed = watchers.remove(watch);
            }
        }
        if (removed && watchers.isEmpty()) {
            if (monitor != null) {
                monitor.cancel(false); // stop watching nobody is looking
                monitor = null;
            }
        }
        return removed;
    }

    /** To allow test cases to set a shorter delay for testing. */
    public void schedule(long delay, TimeUnit unit) {
        this.delay = delay;
        this.unit = unit;
        if (monitor != null) {
            monitor.cancel(false);
            monitor = pool.scheduleWithFixedDelay(sync, delay, delay, unit);
        }
    }

    @Override
    public void destroy() throws Exception {
        pool.shutdown();
        monitor = null;
    }

    @Override
    public void changed(ResourceNotification notification) {
        throw new UnsupportedOperationException();
    }
}

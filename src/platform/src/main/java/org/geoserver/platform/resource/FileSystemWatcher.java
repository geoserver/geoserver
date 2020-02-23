/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.resource.ResourceNotification.Kind;
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

    interface FileExtractor {
        public File getFile(String path);
    }

    /** Change to file system */
    static class Delta {
        final File context;

        final Kind kind;

        final List<File> created;

        final List<File> removed;

        final List<File> modified;

        public Delta(File context, Kind kind) {
            this.context = context;
            this.kind = kind;
            this.created = null;
            this.removed = null;
            this.modified = null;
        }

        public Delta(
                File context,
                Kind kind,
                List<File> created,
                List<File> removed,
                List<File> modified) {
            this.context = context;
            this.kind = Kind.ENTRY_MODIFY;
            this.created = created != null ? created : (List<File>) Collections.EMPTY_LIST;
            this.removed = removed != null ? removed : (List<File>) Collections.EMPTY_LIST;
            this.modified = modified != null ? modified : (List<File>) Collections.EMPTY_LIST;
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

        List<ResourceListener> listeners = new CopyOnWriteArrayList<ResourceListener>();

        /** When last notification was sent */
        long last = 0;

        /** Used to track resource creation / deletion */
        boolean exsists;

        File[] contents; // directory contents at last check

        public Watch(File file, String path) {
            this.file = file;
            this.path = path;
            this.exsists = file.exists();
            this.last = exsists ? file.lastModified() : 0;
            if (file.isDirectory()) {
                contents = file.listFiles();
            }
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
            result = prime * result + ((file == null) ? 0 : file.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Watch other = (Watch) obj;
            if (file == null) {
                if (other.file != null) return false;
            } else if (!file.equals(other.file)) return false;
            if (path == null) {
                if (other.path != null) return false;
            } else if (!path.equals(other.path)) return false;
            return true;
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
                if (exsists) {
                    exsists = false;
                    if (contents != null) {
                        // delete directory
                        List<File> deleted = Arrays.asList(contents);
                        this.last = now;
                        this.contents = null;
                        return new Delta(file, Kind.ENTRY_DELETE, null, deleted, null);
                    } else {
                        // file has been deleted!
                        this.last = now;
                        return new Delta(file, Kind.ENTRY_DELETE);
                    }
                } else {
                    return null; // no change file still deleted!
                }
            }
            if (file.isFile()) {
                long fileModified = file.lastModified();
                if (fileModified > last || !exsists) {
                    if (exsists) {
                        this.last = fileModified;
                        return new Delta(file, Kind.ENTRY_MODIFY);
                    } else {
                        exsists = true;
                        this.last = fileModified;
                        return new Delta(file, Kind.ENTRY_CREATE);
                    }
                } else {
                    return null; // no change!
                }
            }
            if (file.isDirectory()) {
                Kind kind = null;
                long fileModified = file.lastModified();
                if (fileModified > last || !exsists) {
                    kind = exsists ? Kind.ENTRY_MODIFY : Kind.ENTRY_CREATE;
                    exsists = true;
                }
                File[] files = file.listFiles();
                if (files == null) {
                    return null;
                }

                List<File> removed = new ArrayList<File>(files.length);
                List<File> created = new ArrayList<File>(files.length);
                List<File> modified = new ArrayList<File>(files.length);

                removed.addAll(Arrays.asList(this.contents));
                removed.removeAll(Arrays.asList(files));
                if (!removed.isEmpty()) {
                    fileModified = Math.max(fileModified, last + 1);
                }

                created.addAll(Arrays.asList(files));
                created.removeAll(Arrays.asList(this.contents));
                for (File check : created) {
                    long checkModified = check.lastModified();
                    fileModified = Math.max(fileModified, checkModified);
                }
                // check contents
                List<File> review = new ArrayList<File>(files.length);
                review.addAll(Arrays.asList(files));
                review.removeAll(created); // no need to check these they are new
                for (File check : review) {
                    long checkModified = check.lastModified();
                    if (checkModified > last) {
                        modified.add(check);
                        fileModified = Math.max(fileModified, checkModified);
                    }
                }
                if (kind == null) {
                    if (removed.isEmpty() && created.isEmpty() && modified.isEmpty()) {
                        // win only check of directory contents
                        return null; // no change to directory contents
                    } else {
                        kind = Kind.ENTRY_MODIFY;
                    }
                }
                this.last = fileModified;
                this.contents = files;
                return new Delta(file, kind, created, removed, modified);
            }
            return null; // no change
        }

        public boolean isMatch(File file, String path) {
            if (this.file == null) {
                if (file != null) {
                    return false;
                }
            } else if (!this.file.equals(file)) {
                return false;
            }

            if (this.path == null) {
                if (path != null) {
                    return false;
                }
            } else if (!this.path.equals(path)) {
                return false;
            }
            return true;
        }
    }

    private ScheduledExecutorService pool;

    private FileExtractor fileExtractor;

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
                        Delta delta = watch.changed(now);
                        if (delta != null) {

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
                        }
                    }
                }
            };

    private ScheduledFuture<?> monitor;

    private TimeUnit unit = TimeUnit.SECONDS;

    private long delay = 10;

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
    FileSystemWatcher(FileExtractor fileExtractor) {
        this.pool = Executors.newSingleThreadScheduledExecutor(tFactory);
        this.fileExtractor = fileExtractor;
    }

    FileSystemWatcher() {
        this(
                new FileExtractor() {
                    @Override
                    public File getFile(String path) {
                        return new File(path.replace('/', File.separatorChar));
                    }
                });
    }

    private Watch watch(File file, String path) {
        if (file == null || path == null) {
            return null;
        }
        for (Watch watch : watchers) {
            if (watch.isMatch(file, path)) {
                return watch;
            }
        }
        return null; // not found
    }

    public synchronized void addListener(String path, ResourceListener listener) {
        File file = fileExtractor.getFile(path);
        if (file == null) {
            throw new NullPointerException("File to watch is required");
        }
        if (path == null) {
            throw new NullPointerException("Path for notification is required");
        }
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
        File file = fileExtractor.getFile(path);
        if (file == null) {
            throw new NullPointerException("File to watch is required");
        }
        if (path == null) {
            throw new NullPointerException("Path for notification is required");
        }
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
    }

    @Override
    public void changed(ResourceNotification notification) {
        throw new UnsupportedOperationException();
    }
}

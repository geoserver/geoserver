/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.geoserver.util.IOUtils.rename;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** Implementation of ResourceStore backed by the file system. */
public class FileSystemResourceStore implements ResourceStore {

    static final Logger LOGGER = Logging.getLogger(FileSystemResource.class);

    public static final String GS_LOCK_TRACE = "gs.lock.trace";
    /**
     * When true, the stack trace that got an input stream that wasn't closed is recorded and then
     * printed out when warning the user about this.
     */
    protected static final Boolean TRACE_ENABLED =
            "true".equalsIgnoreCase(System.getProperty(GS_LOCK_TRACE));

    /** LockProvider used to secure resources for exclusive access */
    protected LockProvider lockProvider = new NullLockProvider();

    /** Base directory for ResourceStore content */
    protected File baseDirectory = null;

    protected FileSystemWatcher watcher;

    protected FileSystemResourceStore() {
        // Used by Spring, baseDirectory set by subclass
    }

    /**
     * LockProvider used during {@link Resource#out()}.
     *
     * <p>Client code that insists on using {@link Resource#file()} can do us using:
     *
     * <pre><code>
     * Resource resource = resoures.get( "example.txt" );
     * Lock lock = resources.getLockProvider().acquire( resource.path() );
     * try {
     *    File file = resoruce.file();
     *    ..
     * }
     * finally {
     *    lock.release();
     * }
     * </code></pre>
     *
     * @return LockProvider used for {@link Resource#out}
     */
    public LockProvider getLockProvider() {
        return lockProvider;
    }

    /**
     * Configure LockProvider used during {@link Resource#out()}.
     *
     * @param lockProvider LockProvider used for Resource#out()
     */
    public void setLockProvider(LockProvider lockProvider) {
        this.lockProvider = lockProvider;
    }

    public FileSystemResourceStore(File resourceDirectory) {
        if (resourceDirectory == null) {
            throw new NullPointerException("root resource directory required");
        }
        if (resourceDirectory.isFile()) {
            throw new IllegalArgumentException(
                    "Directory required, file present at this location " + resourceDirectory);
        }
        if (!resourceDirectory.exists()) {
            boolean create = resourceDirectory.mkdirs();
            if (!create) {
                throw new IllegalArgumentException(
                        "Unable to create directory " + resourceDirectory);
            }
        }
        if (resourceDirectory.exists() && resourceDirectory.isDirectory()) {
            this.baseDirectory = resourceDirectory;
        } else {
            throw new IllegalArgumentException("Unable to acess directory " + resourceDirectory);
        }
    }

    @Override
    public Resource get(String path) {
        path = Paths.valid(path);
        return new FileSystemResource(path);
    }

    @Override
    public boolean remove(String path) {
        path = Paths.valid(path);

        File file = Paths.toFile(baseDirectory, path);

        return Files.delete(file);
    }

    @Override
    public boolean move(String path, String target) {
        path = Paths.valid(path);
        target = Paths.valid(target);

        File file = Paths.toFile(baseDirectory, path);
        File dest = Paths.toFile(baseDirectory, target);

        if (!file.exists() && !dest.exists()) {
            return true; // moving an undefined resource
        }

        try {
            dest.getParentFile().mkdirs(); // Make sure there's somewhere to move to.
            java.nio.file.Files.move(
                    java.nio.file.Paths.get(file.getAbsolutePath()),
                    java.nio.file.Paths.get(dest.getAbsolutePath()),
                    StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to move " + path + " to " + target, e);
        }
    }

    @Override
    public String toString() {
        return "ResourceStore " + baseDirectory;
    }

    /**
     * Direct implementation of Resource.
     *
     * <p>This implementation is a stateless data object, acting as a simple handle around a File.
     */
    class FileSystemResource implements Resource {

        String path;

        File file;

        public FileSystemResource(String path) {
            this.path = path;
            this.file = Paths.toFile(baseDirectory, path);
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String name() {
            return Paths.name(path);
        }

        @Override
        public Lock lock() {
            return lockProvider.acquire(path);
        }

        @Override
        public void addListener(ResourceListener listener) {
            getResourceNotificationDispatcher().addListener(path, listener);
        }

        @Override
        public void removeListener(ResourceListener listener) {
            getResourceNotificationDispatcher().removeListener(path, listener);
        }

        @Override
        public InputStream in() {
            File actualFile = file();
            if (!actualFile.exists()) {
                throw new IllegalStateException("File not found " + actualFile);
            }
            final Lock lock = lock();
            final Throwable tracer;
            if (TRACE_ENABLED) {
                tracer = new Exception();
                tracer.fillInStackTrace();
            } else {
                tracer = null;
            }
            try {
                return new FileInputStream(file) {
                    boolean closed = false;

                    @Override
                    public void close() throws IOException {
                        closed = true;
                        super.close();
                        lock.release();
                    }

                    @Override
                    @SuppressWarnings("deprecation") // finalize is deprecated in Java 9
                    protected void finalize() throws IOException {
                        if (!closed) {
                            String warn =
                                    "There is code leaving resource input streams open, locks around them might not be cleared! ";
                            if (!TRACE_ENABLED) {
                                warn +=
                                        "Add -D"
                                                + GS_LOCK_TRACE
                                                + "=true to your JVM options to get a full stack trace of the code that acquired the input stream";
                            }
                            LOGGER.warning(warn);

                            if (TRACE_ENABLED) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "The unclosed input stream originated on this stack trace",
                                        tracer);
                            }
                        }
                        super.finalize();
                    }
                };
            } catch (FileNotFoundException e) {
                lock.release();
                throw new IllegalStateException("File not found " + actualFile, e);
            }
        }

        @Override
        public OutputStream out() {
            final File actualFile = file();
            if (!actualFile.exists()) {
                throw new IllegalStateException("Cannot access " + actualFile);
            }
            try {
                // first save to a temp file
                final File temp;
                synchronized (this) {
                    File tryTemp;
                    do {
                        UUID uuid = UUID.randomUUID();
                        tryTemp =
                                new File(
                                        actualFile.getParentFile(),
                                        String.format("%s.%s.tmp", actualFile.getName(), uuid));
                    } while (tryTemp.exists());

                    temp = tryTemp;
                }
                // OutputStream wrapper used to write to a temporary file
                // (and only lock during move to actualFile)
                return new OutputStream() {
                    FileOutputStream delegate = new FileOutputStream(temp);

                    @Override
                    public void close() throws IOException {
                        delegate.close();
                        // if already closed, there should be no exception (see spec Closeable)
                        if (temp.exists()) {
                            Lock lock = lock();
                            try {
                                // no errors, overwrite the original file
                                Files.move(temp, actualFile);
                            } finally {
                                lock.release();
                            }
                        }
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        delegate.write(b, off, len);
                    }

                    @Override
                    public void flush() throws IOException {
                        delegate.flush();
                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        delegate.write(b);
                    }

                    @Override
                    public void write(int b) throws IOException {
                        delegate.write(b);
                    }
                };
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Cannot access " + actualFile, e);
            }
        }

        @Override
        public File file() {
            if (!file.exists()) {
                try {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        boolean created = parent.mkdirs();
                        if (!created) {
                            throw new IllegalStateException(
                                    "Unable to create " + parent.getAbsolutePath());
                        }
                    }
                    if (parent.isDirectory()) {
                        Lock lock = lock();
                        boolean created;
                        try {
                            created = file.createNewFile();
                        } finally {
                            lock.release();
                        }
                        if (!created) {
                            throw new FileNotFoundException(
                                    "Unable to create " + file.getAbsolutePath());
                        }
                    } else {
                        throw new FileNotFoundException(
                                "Unable to create"
                                        + file.getName()
                                        + " - not a directory "
                                        + parent.getAbsolutePath());
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot create " + path, e);
                }
            }
            if (file.isDirectory()) {
                throw new IllegalStateException("Directory (not a file) at " + path);
            } else {
                return file;
            }
        }

        @Override
        public File dir() {
            if (!file.exists()) {
                try {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        boolean created = parent.mkdirs();
                        if (!created) {
                            throw new IllegalStateException(
                                    "Unable to create " + parent.getAbsolutePath());
                        }
                    }
                    if (parent.isDirectory()) {
                        Lock lock = lock();
                        boolean created;
                        try {
                            created = file.mkdir();
                        } finally {
                            lock.release();
                        }
                        if (!created) {
                            throw new FileNotFoundException(
                                    "Unable to create " + file.getAbsolutePath());
                        }
                    } else {
                        throw new FileNotFoundException(
                                "Unable to create"
                                        + file.getName()
                                        + " - not a directory "
                                        + parent.getAbsolutePath());
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot create " + path, e);
                }
            }
            if (file.isFile()) {
                throw new IllegalStateException("File (not a directory) at " + path);
            } else {
                return file;
            }
        }

        @Override
        public long lastmodified() {
            return file.lastModified();
        }

        @Override
        public List<Resource> list() {
            if (!file.exists()) {
                return Collections.emptyList();
            }
            if (file.isFile()) {
                return Collections.emptyList();
            }
            String array[] = file.list();
            if (array == null) {
                return Collections.emptyList();
            }
            List<Resource> list = new ArrayList<Resource>(array.length);
            for (String filename : array) {
                Resource resource = FileSystemResourceStore.this.get(Paths.path(path, filename));
                list.add(resource);
            }
            return list;
        }

        @Override
        public Resource parent() {
            int split = path.lastIndexOf('/');
            if (split == -1) {
                return FileSystemResourceStore.this.get(Paths.BASE); // root
            } else {
                return FileSystemResourceStore.this.get(path.substring(0, split));
            }
        }

        @Override
        public Resource get(String resourcePath) {
            if (resourcePath == null) {
                throw new NullPointerException("Resource path required");
            }
            if ("".equals(resourcePath)) {
                return this;
            }
            return FileSystemResourceStore.this.get(Paths.path(path, resourcePath));
        }

        @Override
        public Type getType() {
            try {
                BasicFileAttributes attributes =
                        java.nio.file.Files.readAttributes(
                                file.toPath(), BasicFileAttributes.class);
                if (attributes.isDirectory()) {
                    return Type.DIRECTORY;
                } else if (attributes.isRegularFile()) {
                    return Type.RESOURCE;
                } else {
                    throw new IllegalStateException(
                            "Path does not represent a configuration resource: " + path);
                }
            } catch (NoSuchFileException e) {
                return Type.UNDEFINED;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            FileSystemResource other = (FileSystemResource) obj;
            if (path == null) {
                if (other.path != null) return false;
            } else if (!path.equals(other.path)) return false;
            return true;
        }

        @Override
        public String toString() {
            return file.getAbsolutePath();
        }

        @Override
        public boolean delete() {
            Lock lock = lock();
            try {
                return Files.delete(file);
            } finally {
                lock.release();
            }
        }

        @Override
        public boolean renameTo(Resource dest) {
            if (dest.parent().path().contains(path())) {
                LOGGER.log(Level.FINE, "Cannot rename a resource to a descendant of itself");
                return false;
            }
            try {
                if (dest instanceof FileSystemResource) {
                    rename(file, ((FileSystemResource) dest).file);
                } else if (dest instanceof Files.ResourceAdaptor) {
                    rename(file, ((Files.ResourceAdaptor) dest).file);
                } else {
                    return Resources.renameByCopy(this, dest);
                }
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to rename file resource " + path + " to " + dest.path(),
                        e);
                return false;
            }
            return true;
        }

        @Override
        public byte[] getContents() throws IOException {
            return java.nio.file.Files.readAllBytes(file.toPath());
        }

        @Override
        public void setContents(byte[] byteArray) throws IOException {
            final File actualFile = file();
            if (!actualFile.exists()) {
                throw new IllegalStateException("Cannot access " + actualFile);
            }
            try {
                // first save to a temp file
                final File temp;
                synchronized (this) {
                    File tryTemp;
                    do {
                        UUID uuid = UUID.randomUUID();
                        tryTemp =
                                new File(
                                        actualFile.getParentFile(),
                                        String.format("%s.%s.tmp", actualFile.getName(), uuid));
                    } while (tryTemp.exists());

                    temp = tryTemp;
                }

                java.nio.file.Files.write(temp.toPath(), byteArray);
                Lock lock = lock();
                try {
                    // no errors, overwrite the original file
                    Files.move(temp, actualFile);
                } finally {
                    lock.release();
                }
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Cannot access " + actualFile, e);
            }
        }
    }

    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        if (watcher == null) {
            watcher =
                    new FileSystemWatcher(
                            new FileSystemWatcher.FileExtractor() {

                                @Override
                                public File getFile(String path) {
                                    return Paths.toFile(baseDirectory, path);
                                }
                            });
        }
        return watcher;
    }
}

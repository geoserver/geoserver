/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of ResourceStore backed by the file system.
 */
public class FileSystemResourceStore implements ResourceStore {
    
    /** LockProvider used to secure resources for exclusive access */
    protected LockProvider lockProvider = new NullLockProvider();
    
    /** Base directory for ResourceStore content */
    protected File baseDirectory = null;

    protected FileSystemWatcher watcher;

    protected FileSystemResourceStore(){
        // Used by Spring, baseDirectory set by subclass
    }

    
    /**
     * LockProvider used during {@link Resource#out()}.
     * 
     * Client code that insists on using {@link Resource#file()} can do us using:
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
            throw new IllegalArgumentException("Directory required, file present at this location "
                    + resourceDirectory);
        }
        if (!resourceDirectory.exists()) {
            boolean create = resourceDirectory.mkdirs();
            if (!create) {
                throw new IllegalArgumentException("Unable to create directory "
                        + resourceDirectory);
            }
        }
        if (resourceDirectory.exists() && resourceDirectory.isDirectory()) {
            this.baseDirectory = resourceDirectory;
        } else {
            throw new IllegalArgumentException("Unable to acess directory " + resourceDirectory);
        }
    }
    
    public synchronized void addListener(File file, String path, ResourceListener listener) {
        if( watcher == null ){
            watcher = new FileSystemWatcher();
        }
        watcher.addListener( file, path, listener );
    }
    
    public synchronized void removeListener(File file, String path, ResourceListener listener) {
        if( watcher != null ){
            watcher.removeListener(file, path, listener );
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
            return Files.move(file, dest);
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
     * <p>
     * This implementation is a stateless data object, acting as a simple handle around a File.
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
            FileSystemResourceStore.this.addListener( file, path, listener );
        }
        
        @Override
        public void removeListener(ResourceListener listener) {
            FileSystemResourceStore.this.removeListener( file, path, listener );
        }
        @Override
        public InputStream in() {
            File actualFile = file();
            if (!actualFile.exists()) {
                throw new IllegalStateException("File not found " + actualFile);
            }
            final Lock lock = lock();
            try {
                return new FileInputStream(file) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                        lock.release();
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
                synchronized(this) {
                    File tryTemp;
                    do {
                        UUID uuid = UUID.randomUUID();
                        tryTemp = new File(actualFile.getParentFile(), String.format("%s.%s.tmp", actualFile.getName(), uuid));
                    } while(tryTemp.exists());
                    
                    temp = tryTemp;
                }
                // OutputStream wrapper used to write to a temporary file
                // (and only lock during move to actualFile)
                return new OutputStream() {
                    FileOutputStream delegate = new FileOutputStream(temp);
                
                    @Override
                    public void close() throws IOException {
                        delegate.close();
                        Lock lock = lock();
                        try {
                            // no errors, overwrite the original file
                            Files.move(temp, actualFile);
                        }
                        finally {
                            lock.release();
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
                            throw new IllegalStateException("Unable to create "
                                    + parent.getAbsolutePath());
                        }
                    }
                    if (parent.isDirectory()) {
                        Lock lock = lock();
                        boolean created;
                        try {
                            created = file.createNewFile();
                        }
                        finally {
                            lock.release();
                        }
                        if (!created) {
                            throw new FileNotFoundException("Unable to create "
                                    + file.getAbsolutePath());
                        }
                    } else {
                        throw new FileNotFoundException("Unable to create" + file.getName()
                                + " - not a directory " + parent.getAbsolutePath());
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
                            throw new IllegalStateException("Unable to create "
                                    + parent.getAbsolutePath());
                        }
                    }
                    if (parent.isDirectory()) {
                        Lock lock = lock();
                        boolean created;
                        try {
                            created = file.mkdir();
                        }
                        finally {
                            lock.release();
                        }
                        if (!created) {
                            throw new FileNotFoundException("Unable to create "
                                    + file.getAbsolutePath());
                        }
                    } else {
                        throw new FileNotFoundException("Unable to create" + file.getName()
                                + " - not a directory " + parent.getAbsolutePath());
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
        public List<Resource> list() {
            String array[] = file.list();
            if (array == null) {
                return null; // not a directory
            }
            List<Resource> list = new ArrayList<Resource>(array.length);
            for (String filename : array) {
                Resource resource = FileSystemResourceStore.this.get(Paths.path(path, filename));
                list.add(resource);
            }
            return list;
        }

        @Override
        public Type getType() {
            if (!file.exists()) {
                return Type.UNDEFINED;
            } else if (file.isDirectory()) {
                return Type.DIRECTORY;
            } else if (file.isFile()) {
                return Type.RESOURCE;
            } else {
                throw new IllegalStateException(
                        "Path does not represent a configuration resource: " + path);
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
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FileSystemResource other = (FileSystemResource) obj;
            if (path == null) {
                if (other.path != null)
                    return false;
            } else if (!path.equals(other.path))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return file.getAbsolutePath();
        }

        @Override
        public boolean delete() {
            return file.delete();
        }

        @Override
        public boolean renameTo(Resource dest) {
            if(dest instanceof FileSystemResource) {
                return file.renameTo(((FileSystemResource)dest).file);
            } else if(dest instanceof Files.ResourceAdaptor) {
                    return file.renameTo(((Files.ResourceAdaptor)dest).file);
            } else {
                return Resources.renameByCopy(this, dest);
            }
        }

    }
}

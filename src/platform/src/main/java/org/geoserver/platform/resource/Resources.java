/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.util.Filter;
import org.geotools.util.URLs;

/**
 * Utility methods for working with {@link ResourceStore}.
 *
 * <p>These methods are suitable for static import and are intended automate common tasks.
 *
 * @author Jody Garnett
 */
public class Resources {

    private static final int MAX_RENAME_ATTEMPTS = 100;

    /**
     * Test if the file or directory denoted by this resource exists.
     *
     * @see File#exists()
     * @param resource Resource indicated
     * @return true If resource is not UNDEFINED
     */
    public static boolean exists(Resource resource) {
        return resource != null && resource.getType() != Resource.Type.UNDEFINED;
    }

    /**
     * Test if the file or directory can be read.
     *
     * @see File#canRead()
     * @param resource Resource indicated
     * @return true If resource is not UNDEFINED
     */
    public static boolean canRead(Resource resource) {
        try {
            InputStream is = resource.in();
            is.read();
            is.close();
            return true;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }

    /**
     * Test if the file or directory behind the resource is hidden. For file system based resources,
     * the platform-dependent hidden property is used. For other resource implementations, filenames
     * starting with a "." are considered hidden, irrespective of the platform.
     *
     * @see File#isHidden()
     * @param resource Resource indicated
     * @return true If resource is hidden
     */
    public static boolean isHidden(Resource resource) {
        if (resource instanceof SerializableResourceWrapper) {
            resource = ((SerializableResourceWrapper) resource).delegate;
        }
        if (resource instanceof FileSystemResourceStore.FileSystemResource
                || resource instanceof Files.ResourceAdaptor) {
            // this is a file based resource, just check the file
            return find(resource).isHidden();
        } else {
            // not a file system based resource, no point in caching
            // we only support linux style hidden file.
            return resource.name().startsWith(".");
        }
    }

    /**
     * Checks {@link Resource#getType()} and returns existing file() or dir() as appropriate, or
     * null for {@link Resource.Type#UNDEFINED}.
     *
     * <p>This approach is a reproduction of GeoServerResourceLoader find logic.
     *
     * @see Resource#dir()
     * @see Resource#file()
     * @param resource Resource indicated
     * @return Existing file, or null for {@link Resource.Type#UNDEFINED}.
     */
    public static File find(Resource resource) {
        if (resource == null) {
            return null;
        }
        switch (resource.getType()) {
            case DIRECTORY:
                return resource.dir();

            case RESOURCE:
                return resource.file();

            default:
                return null;
        }
    }

    /**
     * Checks {@link Resource#getType()} and returns existing file() or dir() as appropriate, or
     * null for {@link Resource.Type#UNDEFINED}.
     *
     * <p>This approach is a reproduction of GeoServerResourceLoader find logic.
     *
     * @see Resource#dir()
     * @see Resource#file()
     * @param resource Resource indicated
     * @param force false to return null for {@link Resource.Type#UNDEFINED}, true to force a File
     *     to be created.
     * @return The file if exists, null if {@link Resource.Type#UNDEFINED} and force is false, a
     *     File with the resource path otherwise
     */
    public static File find(Resource resource, boolean force) {
        if (resource == null) {
            return null;
        }
        switch (resource.getType()) {
            case DIRECTORY:
                return resource.dir();

            case RESOURCE:
                return resource.file();

            default:
                if (force) {
                    return new File(resource.path());
                } else {
                    return null;
                }
        }
    }

    /**
     * Checks {@link Resource#getType()} and returns existing dir() if available, or null for {@link
     * Resource.Type#UNDEFINED} or {@link Resource.Type#RESOURCE}.
     *
     * <p>This approach is a reproduction of GeoServerDataDirectory findDataDir logic and will not
     * create a new directory.
     *
     * @see Resource#dir()
     * @param resource Resource indicated
     * @return File reference to existing directory, or null for an existing file (or if directory
     *     does not exist)
     */
    public static File directory(Resource resource) {
        return directory(resource, false);
    }

    /**
     * If create is true or if a directory exists returns resource.dir, otherwise it returns null.
     *
     * @see Resource#dir()
     * @param resource Resource indicated
     * @param create true to create directory (if it does not exsist)
     * @return File reference to (possibly new) directory
     */
    public static File directory(Resource resource, boolean create) {
        final File f;
        if (resource == null) {
            f = null;
        } else if (create) {
            f = resource.dir();
        } else {
            if (resource.getType() == Type.DIRECTORY) {
                f = resource.dir();
            } else {
                f = null;
            }
        }
        return f;
    }

    /**
     * Checks {@link Resource#getType()} and returns existing file() if available, or null for
     * {@link Resource.Type#UNDEFINED} or {@link Resource.Type#DIRECTORY}.
     *
     * <p>This approach is a reproduction of GeoServerDataDirectory findDataFile logic and will not
     * create a new file.
     *
     * @see Resource#file()
     * @param resource Resource indicated
     * @return Existing file, or null
     */
    public static File file(Resource resource) {
        return file(resource, false);
    }

    /**
     * If create is true or if a file exists returns resource.file, otherwise it returns null.
     *
     * @see Resource#file()
     * @param resource Resource indicated
     * @param create true to create (if needed)
     * @return file, or null
     */
    public static File file(Resource resource, boolean create) {
        final File f;
        if (resource == null) {
            f = null;
        } else if (create) {
            f = resource.file();
        } else {
            if (resource.getType() == Type.RESOURCE) {
                f = resource.file();
            } else {
                f = null;
            }
        }
        return f;
    }

    /**
     * Create a new directory for the provided resource (this will only work for {@link
     * Resource.Type#UNDEFINED}).
     *
     * <p>This approach is a reproduction of GeoServerResourceLoader createNewDirectory logic.
     *
     * @param resource Resource indicated
     * @return newly created file
     * @throws IOException If directory could not be created (as file or directory already exists)
     */
    public static File createNewDirectory(Resource resource) throws IOException {
        switch (resource.getType()) {
            case DIRECTORY:
                throw new IOException(
                        "New directory " + resource.path() + " already exists as DIRECTORY");
            case RESOURCE:
                throw new IOException(
                        "New directory " + resource.path() + " already exists as RESOURCE");
            case UNDEFINED:
                return resource.dir(); // will create directory as needed
            default:
                return null;
        }
    }

    /**
     * Create a new file for the provided resource (this will only work for {@link
     * Resource.Type#UNDEFINED}).
     *
     * <p>This approach is a reproduction of GeoServerResourceLoader createNewFile logic.
     *
     * @param resource Resource indicated
     * @return newly created file
     * @throws IOException If path indicates a file (or directory) that already exists
     */
    public static File createNewFile(Resource resource) throws IOException {
        switch (resource.getType()) {
            case DIRECTORY:
                throw new IOException(
                        "New file " + resource.path() + " already exists as DIRECTORY");
            case RESOURCE:
                throw new IOException(
                        "New file " + resource.path() + " already exists as RESOURCE");
            case UNDEFINED:
                return resource.file(); // will create directory as needed
            default:
                return null;
        }
    }

    /**
     * Search for resources using pattern and last modified time.
     *
     * @param resource Resource indicated
     * @param lastModified time stamp to search from
     * @return list of modified resources
     */
    public static List<Resource> search(Resource resource, long lastModified) {
        if (resource.getType() == Type.DIRECTORY) {
            ArrayList<Resource> results = new ArrayList<Resource>();
            for (Resource child : resource.list()) {
                switch (child.getType()) {
                    case RESOURCE:
                        if (child.lastmodified() > lastModified) {
                            results.add(child);
                        }
                        break;

                    default:
                        break;
                }
            }
            return results;
        }
        return Collections.emptyList();
    }

    /**
     * Write the contents of a stream into a resource
     *
     * @param data data to write
     * @param destination resource to write to
     * @throws IOException If data could not be copied to destination
     */
    public static void copy(InputStream data, Resource destination) throws IOException {
        try (OutputStream out = destination.out()) {
            IOUtils.copy(data, out);
        }
    }

    /**
     * Write the contents of a resource into another resource. Also supports directories
     * (recursively).
     *
     * @param data resource to read
     * @param destination resource to write to
     * @throws IOException If data could not be copied to destination
     */
    public static void copy(Resource data, Resource destination) throws IOException {
        if (data.getType() == Type.DIRECTORY) {
            for (Resource child : data.list()) {
                copy(child, destination.get(child.name()));
            }
        } else {
            try (InputStream in = data.in()) {
                copy(in, destination);
            }
        }
    }

    /**
     * Write the contents of a stream to a new Resource inside a directory
     *
     * @param data data to write
     * @param directory parent directory to create the resource in
     * @param filename file name of the new resource
     * @throws IOException If data could not be copied into indicated location
     */
    public static void copy(InputStream data, Resource directory, String filename)
            throws IOException {
        copy(data, directory.get(filename));
    }

    /**
     * Write the contents of a File to a new Resource with the same name inside a directory
     *
     * @param data data to write
     * @param directory parent directory to create the resource in
     * @throws IOException If file could not be copied into directory
     */
    public static void copy(File data, Resource directory) throws IOException {
        String filename = data.getName();
        try (InputStream in = new FileInputStream(data)) {
            copy(in, directory.get(filename));
        }
    }

    /**
     * Renames a resource by reading it and writing to the new resource, then deleting the old one.
     * This is not atomic.
     *
     * @param source Resource to rename
     * @param destination New resource location
     * @return true if successful, false if either the write or delete failed.
     */
    public static boolean renameByCopy(Resource source, Resource destination) {
        try {
            copy(source, destination);
            return source.delete();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns filtered children of a directory
     *
     * @param dir parent directory
     * @param filter the filter that selects children
     * @param recursive searches recursively
     * @return filtered list
     */
    public static List<Resource> list(Resource dir, Filter<Resource> filter, boolean recursive) {
        List<Resource> res = new ArrayList<Resource>();
        for (Resource child : dir.list()) {
            if (filter.accept(child)) {
                res.add(child);
            }
            if (recursive && child.getType() == Type.DIRECTORY) {
                res.addAll(list(child, filter, true));
            }
        }
        return res;
    }

    /**
     * Convenience method for non recursive listing
     *
     * @param dir parent directory
     * @param filter parent directory
     * @return filtered list
     */
    public static List<Resource> list(Resource dir, Filter<Resource> filter) {
        return list(dir, filter, false);
    }

    /**
     * Recursively loops through directory to provide all children
     *
     * @param dir Resource of directory to list from
     * @return list of children with recursive children
     */
    public static List<Resource> listRecursively(Resource dir) {
        return list(dir, AnyFilter.INSTANCE, true);
    }

    /** File Extension based filtering */
    public static class ExtensionFilter implements Filter<Resource> {

        private Set<String> extensions;

        /**
         * Create extension filter
         *
         * @param extensions in upper case
         */
        public ExtensionFilter(String... extensions) {
            this.extensions = new HashSet<String>(Arrays.asList(extensions));
        }

        @Override
        public boolean accept(Resource obj) {
            return extensions.contains(
                    obj.name().substring(obj.name().lastIndexOf(".") + 1).toUpperCase());
        }
    }

    public static class DirectoryFilter implements Filter<Resource> {

        public static final DirectoryFilter INSTANCE = new DirectoryFilter();

        private DirectoryFilter() {};

        @Override
        public boolean accept(Resource obj) {
            return obj.getType() == Type.DIRECTORY;
        }
    }

    public static class AnyFilter implements Filter<Resource> {

        public static final AnyFilter INSTANCE = new AnyFilter();

        private AnyFilter() {};

        @Override
        public boolean accept(Resource obj) {
            return true;
        }
    }

    /**
     * Creates resource from a path, if the path is relative it will return a resource from the
     * default resource loader otherwise it will return a file based resource
     *
     * @param path relative or absolute path
     * @return resource
     */
    public static Resource fromPath(String path) {
        return ((GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader"))
                .fromPath(path);
    }

    /**
     * Creates resource from a path, if the path is relative it will return a resource relative to
     * the provided directory otherwise it will return a file based resource
     *
     * @param path relative or absolute path
     * @param relativeDir directory to which relative paths are relative
     * @return resource
     */
    public static org.geoserver.platform.resource.Resource fromPath(
            String path, org.geoserver.platform.resource.Resource relativeDir) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return Files.asResource(file);
        } else {
            return relativeDir.get(path.replace(File.separatorChar, '/'));
        }
    }

    public static Resource createRandom(String prefix, String suffix, Resource dir)
            throws IOException {
        // Use only the file name from the supplied prefix
        prefix = (new File(prefix)).getName();

        Resource res;
        do {
            UUID uuid = UUID.randomUUID();
            String name = prefix + uuid + suffix;
            res = dir.get(name);
        } while (exists(res));

        return res;
    }

    /**
     * Used to look up resources based on user provided url (or path) using the Data Directory as
     * base directory.
     *
     * <p>This method is used to process a URL provided by a user: <i>Given a path, tries to
     * interpret it as a file into the data directory, or as an absolute location, and returns the
     * actual absolute location of the file.</i>
     *
     * <p>Over time this url method has grown in the telling to support:
     *
     * <ul>
     *   <li>Actual URL to external resource using http or ftp protocol - will return null
     *   <li>Resource URL - will support resources from resource store
     *   <li>File URL - will support absolute file references
     *   <li>File URL - will support relative file references - this is deprecated, use resource:
     *       instead
     *   <li>Fake URLs - sde://user:pass@server:port - will return null.
     *   <li>path - user supplied file path (operating specific specific)
     * </ul>
     *
     * @param path File URL, or path, relative to data directory
     * @return Resource indicated by provided URL
     */
    public static Resource fromURL(String path) {
        return ((GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader")).fromURL(path);
    }

    /**
     * Used to look up resources based on user provided url (or path).
     *
     * <p>This method is used to process a URL provided by a user: <i>iven a path, tries to
     * interpret it as a file into the data directory, or as an absolute location, and returns the
     * actual absolute location of the file.</i>
     *
     * <p>Over time this url method has grown in the telling to support:
     *
     * <ul>
     *   <li>Actual URL to external resoruce using http or ftp protocol - will return null
     *   <li>Resource URL - will support resources from resource store
     *   <li>File URL - will support absolute file references
     *   <li>File URL - will support relative file references - this is deprecated, use resource:
     *       instead
     *   <li>Fake URLs - sde://user:pass@server:port - will return null.
     *   <li>path - user supplied file path (operating specific specific)
     * </ul>
     *
     * Note that the baseDirectory is optional (and may be null).
     *
     * @param baseDirectory Optional base directory used to resolve relative file URLs
     * @param url File URL or path relative to data directory
     * @return Resource indicated by provided URL
     */
    public static Resource fromURL(Resource baseDirectory, String url) {
        String ss;
        if (!Objects.equals(url, ss = StringUtils.removeStart(url, "resource:"))) {
            return baseDirectory.get(ss);
        }

        // if path looks like an absolute file: URL, try standard conversion
        if (url.startsWith("file:/")) {
            try {
                return Files.asResource(URLs.urlToFile(new URL(url)));
            } catch (Exception e) {
                // failure, so fall through
            }
        }

        // do we ever have something that is not a file system reference?
        // yes. See GEOS-5931: cases like sde://user:pass@server:port or
        // pgraster://user:pass@server:port or similar custom store URLs.
        if (url.startsWith("file:")) {
            url = url.substring(5); // remove 'file:' prefix

            File f = new File(url);

            if (f.isAbsolute() || f.exists()) {
                return Files.asResource(f); // if it's an absolute path, use it as such

            } else {
                // otherwise try to map it inside the data dir
                if (baseDirectory != null) {
                    return baseDirectory.get(url);
                }
                return Files.asResource(f); // fine return it as is
            }
        } else {
            // Treating 'url' as a normal file path
            File file = new File(url);
            if (file.isAbsolute() || file.exists()) {
                return Files.asResource(file); // if it's an absolute path, use it as such
            }
            // otherwise try to map it inside the data dir
            if (baseDirectory != null) {
                Resource res = baseDirectory.get(url);
                if (exists(res)) {
                    return res;
                }
            }
            // Allows dealing with custom URL Strings. Don't return a file for them
            return null;
        }
    }

    /**
     * Used to look up resources based on user provided url, using the Data Directory as base
     * directory.
     *
     * <p>Supports
     *
     * <ul>
     *   <li>Actual URL to external resource using http or ftp protocol - will return null
     *   <li>Resource URL - will support resources from resource store
     *   <li>File URL - will support absolute file references
     *   <li>File URL - will support relative file references - this is deprecated, use resource:
     *       instead
     *   <li>Fake URLs - sde://user:pass@server:port - will return null.
     * </ul>
     *
     * @param url the url
     * @return corresponding Resource
     */
    public static Resource fromURL(URL url) {
        return ((GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader")).fromURL(url);
    }

    /**
     * Used to look up a resource based on user provided url.
     *
     * <p>Supports
     *
     * <ul>
     *   <li>Actual URL to external resource using http or ftp protocol - will return null
     *   <li>Resource URL - will support resources from resource store
     *   <li>File URL - will support absolute file references
     *   <li>File URL - will support relative file references - this is deprecated, use resource:
     *       instead
     *   <li>Fake URLs - sde://user:pass@server:port - will return null.
     * </ul>
     *
     * @param baseDirectory base directory for resource: or relative file: paths
     * @param url the url
     * @return corresponding Resource
     */
    public static Resource fromURL(Resource baseDirectory, URL url) {
        if (url.getProtocol().equalsIgnoreCase("resource")) {
            return baseDirectory.get(Paths.convert(url.getPath()));
        } else if (url.getProtocol().equalsIgnoreCase("file")) {
            return Files.asResource(URLs.urlToFile(url));
        } else {
            return null;
        }
    }

    /**
     * Create a URL from a resource.
     *
     * @param res Resource to represent as a URL
     * @return URL from an internal resource
     */
    public static URL toURL(final Resource res) {
        try {
            if (res instanceof Files.ResourceAdaptor) {
                return res.file().toURI().toURL();
            }

            if (res instanceof URIs.ResourceAdaptor) {
                return ((URIs.ResourceAdaptor) res).getURL();
            }

            return new URL(
                    "resource",
                    null,
                    -1,
                    String.format(res.getType() == Type.DIRECTORY ? "/%s/" : "/%s", res.path()),
                    new URLStreamHandler() {

                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            return new URLConnection(u) {

                                @Override
                                public void connect() throws IOException {}

                                @Override
                                public long getLastModified() {
                                    return res.lastmodified();
                                }

                                @Override
                                public InputStream getInputStream() throws IOException {
                                    return res.in();
                                }

                                @Override
                                public OutputStream getOutputStream() throws IOException {
                                    return res.out();
                                }
                            };
                        }
                    });
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Should not happen", e);
        }
    }

    /** Resource wrapper, serialization using resource path. */
    private static class SerializableResourceWrapper implements Serializable, Resource {
        private static final long serialVersionUID = 1758097257412707071L;

        private transient Resource delegate;
        private String path;

        private void readObject(ObjectInputStream stream)
                throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            delegate = Resources.fromPath(path);
        }

        public SerializableResourceWrapper(Resource delegate) {
            this.delegate = delegate;
            path = delegate.path();
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String name() {
            return delegate.name();
        }

        @Override
        public Lock lock() {
            return delegate.lock();
        }

        @Override
        public void addListener(ResourceListener listener) {
            delegate.addListener(listener);
        }

        @Override
        public void removeListener(ResourceListener listener) {
            delegate.removeListener(listener);
        }

        @Override
        public InputStream in() {
            return delegate.in();
        }

        @Override
        public OutputStream out() {
            return delegate.out();
        }

        @Override
        public File file() {
            return delegate.file();
        }

        @Override
        public File dir() {
            return delegate.dir();
        }

        @Override
        public long lastmodified() {
            return delegate.lastmodified();
        }

        @Override
        public Resource parent() {
            return delegate.parent() == null
                    ? null
                    : new SerializableResourceWrapper(delegate.parent());
        }

        @Override
        public Resource get(String resourcePath) {
            return delegate.get(resourcePath);
        }

        @Override
        public List<Resource> list() {
            List<Resource> children = new ArrayList<Resource>();
            for (Resource child : delegate.list()) {
                children.add(new SerializableResourceWrapper(child));
            }
            return children;
        }

        @Override
        public Type getType() {
            return delegate.getType();
        }

        @Override
        public boolean delete() {
            return delegate.delete();
        }

        @Override
        public boolean renameTo(Resource dest) {
            return delegate.renameTo(dest);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SerializableResourceWrapper)) {
                return false;
            }
            return delegate.equals(((SerializableResourceWrapper) o).delegate);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    public static Resource serializable(Resource resource) {
        if (resource instanceof Serializable) {
            return resource;
        }
        if (resource == null) {
            return null;
        }
        return new SerializableResourceWrapper(resource);
    }

    /**
     * Determine unique name of the form <code>newName.extension</code>. newName will have a number
     * appended as required to produce a unique resource name.
     *
     * @param resource Resource being renamed
     * @param newName proposed name to use as a template
     * @param extension extension
     * @return New UNDEFINED resource suitable for use with rename
     * @throws IOException If unique resource cannot be produced
     */
    public static Resource uniqueResource(Resource resource, String newName, String extension)
            throws IOException {
        Resource target = resource.parent().get(newName + "." + extension);

        int i = 0;
        while (target.getType() != Type.UNDEFINED && ++i <= MAX_RENAME_ATTEMPTS) {
            target = resource.parent().get(newName + i + "." + extension);
        }
        if (i > MAX_RENAME_ATTEMPTS) {
            throw new IOException(
                    "All target files between "
                            + newName
                            + "1."
                            + extension
                            + " and "
                            + newName
                            + MAX_RENAME_ATTEMPTS
                            + "."
                            + extension
                            + " are in use already, giving up");
        }
        return target;
    }
}

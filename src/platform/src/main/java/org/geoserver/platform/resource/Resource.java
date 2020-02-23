/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 * Resource used for configuration storage.
 *
 * <p>Resources represent {@link Type#DIRECTORY}, {@link Type#RESOURCE} and {@link Type#UNDEFINED}
 * content and is primarily used to manage configuration information.
 *
 * <p>Resource creation is handled in a lazy fashion, simply use {@link #file()} or {@link #out()}
 * and the resource will be created as required. In a similar fashion setting up a child resource
 * will create any required parent directories.
 */
public interface Resource {
    /** Enumeration indicating kind of resource used. */
    public enum Type {
        /**
         * Resource directory (contents available using {@link Resource#list()}).
         *
         * @see File#isDirectory()
         */
        DIRECTORY,
        /**
         * Resource used for content. Content access available through {@link Resource#in()} and
         * {@link Resource#out()}.
         */
        RESOURCE,

        /**
         * Undefined resource.
         *
         * @see File#exists()
         */
        UNDEFINED
    }

    /** Token used to reserve resource for use. */
    public interface Lock {
        /** Releases the lock on the specified key */
        public void release();
    }

    /**
     * Resource path used by {@link ResourceStore#get(String)}. The path uses unix conventions, thus
     * uses "/" as the separator.
     *
     * @return resource path
     */
    String path();

    /**
     * Name of the resource denoted by {@link #path()} . This is the last name in the path name
     * sequence corresponding to {@link File#getName()}.
     *
     * @return Resource name
     */
    String name();

    /**
     * Acquires an exclusive lock on resource content.
     *
     * @return an exclusive lock
     */
    Lock lock();

    /**
     * Registers listener with ResourceNotificationDispatcher.
     *
     * @see ResourceNotificationDispatcher#addListener(String, ResourceListener)
     */
    void addListener(ResourceListener listener);

    /**
     * Removes listener from ResourceNotificationDispatcher.
     *
     * @see ResourceNotificationDispatcher#removeListener(String, ResourceListener)
     */
    void removeListener(ResourceListener listener);

    /**
     * Steam access to resource contents.
     *
     * @return stream access to resource contents.
     */
    InputStream in();

    /**
     * Steam access to resource contents.
     *
     * @return stream acecss to resource contents.
     */
    OutputStream out();

    /**
     * File access to resource contents.
     *
     * <p>The resource may need to be unpacked into the GeoServer data directory prior to use. Do
     * not assume the file exists before calling this method.
     *
     * @return file access to resource contents.
     */
    File file();

    /**
     * Directory access to resource contents.
     *
     * <p>Directory contents may need to be unpacked into the GeoServer data directory prior to use.
     * Do not assume the file exists before calling this method.
     *
     * @return directory access to resource contents.
     */
    File dir();

    /**
     * Time this resource was last modified.
     *
     * @see File#lastModified()
     * @return time resource was last modified
     */
    long lastmodified();

    /**
     * Resource parent, or null for ResourceStore base diretory.
     *
     * @see File#getParentFile()
     * @return Resource located parent path, or null ResourceStore base directory
     */
    Resource parent();

    /**
     * Path based resource access which can be used to access {@link #list()} contents or create a
     * new undefined resource.
     *
     * <p>The returned Resource acts as a handle, and may be UNDEFINED. In general Resources are
     * created in a lazy fashion when used for the first time.
     *
     * <p>This method is used to access directory contents, relative paths such as ".." and "." are
     * not supported.
     *
     * @param resourcePath path to child resource (using unix conventions, forward slash as
     *     separator)
     * @return Resource at the indicated path
     */
    Resource get(String resourcePath);

    /**
     * List of directory contents.
     *
     * <p>The listed files exist (and may be DIRECTORY or RESOURCE items).
     *
     * @see File#listFiles()
     * @return List of directory contents, or an empty list for UNDEFINED or RESOURCE
     */
    List<Resource> list();

    /**
     * Resource type.
     *
     * @see File#exists()
     * @see File#isDirectory()
     * @see File#isFile()
     */
    Type getType();

    /**
     * Deletes a resource, if the resource is a directory contents will be recursively deleted.
     *
     * @return <code>true</code> if and only if the file is deleted
     */
    boolean delete();

    /**
     * Move the resource to the specified location.
     *
     * @see File#renameTo(File)
     */
    boolean renameTo(Resource dest);

    /**
     * Returns a resource full contents as a byte array. Usage is suggested only if the resource is
     * known to be small (e.g. a configuration file).
     */
    default byte[] getContents() throws IOException {
        try (InputStream in = in()) {
            return org.apache.commons.io.IOUtils.toByteArray(in);
        }
    }

    /**
     * Writes a resource contents as a byte array. Usage is suggested only if the resource is known
     * to be small (e.g. a configuration file).
     */
    default void setContents(byte[] byteArray) throws IOException {
        try (OutputStream os = out()) {
            IOUtils.write(byteArray, os);
        }
    }
}

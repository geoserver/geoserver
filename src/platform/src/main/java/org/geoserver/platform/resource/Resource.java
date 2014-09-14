/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Resource used for configuration storage.
 * 
 * Resources represent {@link Type#DIRECTORY}, {@link Type#RESOURCE} and {@link Type#UNDEFINED} content and is primarily used to manage configuration
 * information.
 * 
 * Resource creation is handled in a lazy fashion, simply use {@link #file()} or {@link #out()} and the resource will be created as required. In a
 * similar fashion setting up a child resource will create any required parent directories.
 */
public interface Resource {
    /**
     * Enumeration indicating kind of resource used.
     */
    public enum Type {
        /**
         * Resource directory (contents available using {@link Resource#list()}).
         * 
         * @see File#isDirectory()
         */
        DIRECTORY,
        /**
         * Resource used for content. Content access available through {@link Resource#in()} and {@link Resource#out()}.
         */
        RESOURCE,
    
        /**
         * Undefined resource.
         * 
         * @see File#exists()
         */
        UNDEFINED
    }

    /**
     * Token used to reserve resource for use.
     */
    public interface Lock {
        /**
         * Releases the lock on the specified key
         * 
         * @param lockKey
         */
        public void release();
    }

    /**
     * Resource path used by {@link ResourceStore.get}.
     * 
     * @return resource path
     */
    String path();

    /**
     * Name of the resource denoted by {@link #getPath()} . This is the last name in the path name sequence corresponding to {@link File#getName()}.
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
     * Listen for changes to ResourceStore content.
     * <p>
     * Listeners can be configured to check for changes to individual files or directory contents.
     * </p>
     * <ul>
     * <li>styles: listener receives events for any change to the contents of the styles directory</li>
     * <li>user_projections/epsg.properties: listener notified for any change to the epsg.properties resource</li>
     * </ul>
     * <p>
     * Notification is course grained, often just based on change of last modified time stamp, as such they are issued after the change has been
     * performed.
     * </p>
     * 
     * @param listener Listener to receive change notification
     */
    void addListener( ResourceListener listener);
    
    /**
     * Remove resource store content listener.
     * @param path
     * @param listener
     */
    void removeListener( ResourceListener listener);
    
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
     * The resource may need to be unpacked into the GeoServer data directory prior to use. Do not assume the file exists before calling this method.
     * 
     * @return file access to resource contents.
     */
    File file();

    /**
     * Directory access to resource contents.
     * 
     * Directory contents may need to be unpacked into the GeoServer data directory prior to use. Do not assume the file exists before calling this
     * method.
     * 
     * @return directory access to resource contents.
     */
    File dir();
    
    /**
     * Time this resource was last modified.
     * 
     * @see File#lastModified()
     * 
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
     * Path based resource access which can be used to access {@link #list()} contents or create a new undefined resource.
     * 
     * The returned Resource acts as a handle, and may be UNDEFINED. In general Resources are created in a lazy fashion when used for the first time.
     * 
     * This method is used to access directory contents, relative paths such as ".." and "." are not supported.
     * 
     * @param resourcePath path to child resource
     * @return Resource at the indicated path
     */
    Resource get(String resourcePath);
    
    /**
     * List of directory contents.
     * 
     * The listed files exist (and may be DIRECTORY or RESOURCE items).
     * 
     * @see File#listFiles()
     * @return List of directory contents, or null if this resource is not a directory
     */
    List<Resource> list();

    /**
     * Resource type.
     * 
     * @see File#exists()
     * @see File#isDirectory()
     * @see File#isFile()
     * @return
     */
    Type getType();
    
    /**
     * Delete the resource.
     * @see File#delete()
     * @return
     */
    boolean delete();
    
    /**
     * Move the resource to the specified location.
     * @see File#renameTo(File)
     * @return
     */
    boolean renameTo(Resource dest);
}

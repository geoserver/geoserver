/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

/**
 * Used to manage configuration storage (file system, test harness, or database blob).
 *
 * <p>InputStream used to access configuration information:
 *
 * <pre>
 * <code>
 * Properties properties = new Properties();
 * properties.load( resourceStore.get("module/configuration.properties").in() );
 * </code>
 * </pre>
 *
 * An OutputStream is provided for storage (Resources will be created as needed):
 *
 * <pre>
 * <code>
 * Properties properties = new Properties();
 * properties.put("hello","world");
 * OutputStream out = resourceStore.get("module/configuration.properties").out();
 * properties.store( out, null );
 * out.close();
 * </code>
 * </pre>
 *
 * Resources can also be extracted to a file if needed.
 *
 * <pre>
 * <code>
 * File file = resourceStore.get("module/logo.png");
 * BufferedImage img = ImageIO.read( file );
 * </code>
 * </pre>
 *
 * The base directory is available using {@link Paths#BASE} (as "") but relative paths ("." and
 * "..") are not supported.
 *
 * <p>This abstraction assumes a unix like file system, all paths should use forward slash "/" as
 * the separator.
 *
 * @see Resources
 * @see Resource
 */
public interface ResourceStore {
    /**
     * Empty placeholder for ResourceStore.
     *
     * <p>Empty placeholder intended for test cases (used as spring context default when a base
     * directory is not provided). This implementation prevents client code from requiring null
     * checks on {@link ResourceStore#get(String)}. IllegalStateException are thrown by in(), out()
     * and file() which are the usual methods clients require error handling.
     */
    public static ResourceStore EMPTY = new NullResourceStore();

    /**
     * Path based resource access.
     *
     * <p>The returned Resource acts as a handle, and may be UNDEFINED. In general Resources are
     * created in a lazy fashion when used for the first time.
     *
     * @param path Path (using unix conventions, forward slash as separator) of requested resource
     * @return Resource at the indicated location (null is never returned although Resource may be
     *     UNDEFINED).
     * @throws IllegalArgumentException If path is invalid
     */
    Resource get(String path);

    /**
     * Remove resource at indicated path (including individual resources or directories).
     *
     * <p>Returns <code>true</code> if Resource existed and was successfully removed. For read-only
     * content (or if a security check) prevents the resource from being removed <code>false</code>
     * is returned.
     *
     * @param path Path of resource to remove (using unix conventions, forward slash as separator)
     * @return <code>false</code> if doesn't exist or unable to remove
     */
    boolean remove(String path);

    /**
     * Move resource at indicated path (including individual resources or directories).
     *
     * @param path Path of resource to move (using unix conventions, forward slash as separator)
     * @param target path for moved resource
     * @return true if resource was moved target path
     */
    boolean move(String path, String target);

    /**
     * The Resource Notification Dispatcher
     *
     * @return resource notification dispatcher
     */
    ResourceNotificationDispatcher getResourceNotificationDispatcher();
}

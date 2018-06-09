/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * (c) 2008-2010 GeoSolutions
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original: LockProvider from GeoWebCache 1.5.1 under a LGPL license
 */
package org.geoserver.platform.resource;

/**
 * Used to acquire and release resource locks.
 *
 * <p>ResoruceStore implementations can make use of a LockProvider when writing to resources. Locks
 * {@link #acquire(String)} makes use of {@link Resource#path()} as a key.
 *
 * <p>Implementations are provided for in-memory and NIO FileLocks.
 *
 * @author Andrea Aime (GeoSolutions)
 * @author Jody Garnett (Boundless)
 */
public interface LockProvider {

    /**
     * Acquires a exclusive lock (using resource path as key).
     *
     * <p>The use of Resource path allows use of NIO FileLock if appropriate.
     *
     * @param path Resource path used as lock key
     */
    public Resource.Lock acquire(String path);
}

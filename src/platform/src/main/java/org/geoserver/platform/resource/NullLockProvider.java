/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * (c) 2008-2010 GeoSolutions
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoWebCache 1.5.1 under a LGPL license
 */
package org.geoserver.platform.resource;

/**
 * A no-op implementation of LockProvider. It does not actually lock anything, can be used to test
 * if the other subsystems continue to work properly in face of absence of locks
 *
 * @author Andrea Aime - GeoSolutions
 */
public class NullLockProvider implements LockProvider {

    public Resource.Lock acquire(final String lockKey) {
        return new Resource.Lock() {
            public void release() {
                // nothing to do
            }

            @Override
            public String toString() {
                return "NullLock " + lockKey;
            }
        };
    }

    @Override
    public String toString() {
        return "NullLockProvider";
    }
}

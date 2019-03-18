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

import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * An in memory lock provider based on a striped lock
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MemoryLockProvider implements LockProvider {

    java.util.concurrent.locks.Lock[] locks;

    public MemoryLockProvider() {
        this(1024);
    }

    public MemoryLockProvider(int concurrency) {
        locks = new java.util.concurrent.locks.Lock[concurrency];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public Resource.Lock acquire(String lockKey) {
        final int idx = getIndex(lockKey);
        locks[idx].lock();
        return new Resource.Lock() {

            boolean released = false;

            public void release() {
                if (!released) {
                    released = true;
                    locks[idx].unlock();
                }
            }

            public String toString() {
                return "MemoryLock " + idx;
            }
        };
    }

    private int getIndex(String lockKey) {
        // Simply hashing the lock key generated a significant number of collisions,
        // doing the SHA1 digest of it provides a much better distribution
        int idx = Math.abs(DigestUtils.sha1Hex(lockKey).hashCode() % locks.length);
        return idx;
    }

    @Override
    public String toString() {
        return "MemoryLockProvider";
    }
}

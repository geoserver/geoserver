/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;

/** @author Alessio Fabiani, GeoSolutions */
public class BackupRestoreConfigurationLockCallback implements BackupRestoreCallback {

    GeoServerConfigurationLock locker;

    static ThreadLocal<LockType> THREAD_LOCK =
            new ThreadLocal<GeoServerConfigurationLock.LockType>();

    public BackupRestoreConfigurationLockCallback(GeoServerConfigurationLock locker) {
        this.locker = locker;
    }

    @Override
    public void onBeginRequest(String requestedType) {
        LockType type = THREAD_LOCK.get();
        if (type != null || requestedType == null) {
            return;
        }

        type = LockType.READ;
        if (requestedType.equals(Backup.RESTORE_JOB_NAME)) {
            type = LockType.WRITE;
        }

        locker.lock(type);
        THREAD_LOCK.set(type);
    }

    @Override
    public void onEndRequest() {
        LockType type = THREAD_LOCK.get();
        if (type != null) {
            THREAD_LOCK.remove();
            locker.unlock();
        }
    }
}

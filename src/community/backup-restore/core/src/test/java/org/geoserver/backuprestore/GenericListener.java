/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import org.geoserver.backuprestore.listener.BackupRestoreJobExecutionListener;
import org.springframework.batch.core.JobExecution;

/** Tests listener that keeps a trace of how many times it was invoked. */
public final class GenericListener implements BackupRestoreJobExecutionListener {

    private static int backupBeforeInvocations = 0;
    private static int backupAfterInvocations = 0;
    private static int restoreBeforeInvocations = 0;
    private static int restoreAfterInvocations = 0;

    @Override
    public void beforeJob(JobType type, JobExecution jobExecution) {
        switch (type) {
            case BACKUP:
                backupBeforeInvocations++;
                break;
            case RESTORE:
                restoreBeforeInvocations++;
                break;
        }
    }

    @Override
    public void afterJob(JobType type, JobExecution jobExecution) {
        switch (type) {
            case BACKUP:
                backupAfterInvocations++;
                break;
            case RESTORE:
                restoreAfterInvocations++;
                break;
        }
    }

    public static int getBackupBeforeInvocations() {
        return backupBeforeInvocations;
    }

    public static int getBackupAfterInvocations() {
        return backupAfterInvocations;
    }

    public static int getRestoreBeforeInvocations() {
        return restoreBeforeInvocations;
    }

    public static int getRestoreAfterInvocations() {
        return restoreAfterInvocations;
    }

    public static void reset() {
        // reset all counter to zero.
        backupBeforeInvocations = 0;
        backupAfterInvocations = 0;
        restoreBeforeInvocations = 0;
        restoreAfterInvocations = 0;
    }
}

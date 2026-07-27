/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

/**
 * Unit tests for the boolean job-option helpers on {@link Backup}, which are the single source of truth for the options
 * whose documented default is {@code true} ({@code BK_SKIP_SECURITY}, {@code BK_SKIP_SETTINGS},
 * {@code BK_PURGE_RESOURCES}). Guards both the default-when-omitted and the explicit-value behaviour, so the
 * (deliberately destructive) defaults cannot drift again.
 */
public class BackupOptionDefaultsTest {

    private static JobParameters params(String... keyValues) {
        JobParametersBuilder b = new JobParametersBuilder();
        for (int i = 0; i < keyValues.length; i += 2) {
            b.addString(keyValues[i], keyValues[i + 1]);
        }
        return b.toJobParameters();
    }

    @Test
    public void defaultsAreTrueWhenOmitted() {
        JobParameters empty = params();
        assertTrue("BK_SKIP_SECURITY should default to true", Backup.isSkipSecuritySettings(empty));
        assertTrue("BK_SKIP_SETTINGS should default to true", Backup.isSkipSettings(empty));
        assertTrue(
                "BK_PURGE_RESOURCES should default to true (restore is destructive)", Backup.isPurgeResources(empty));
    }

    @Test
    public void explicitFalseIsHonored() {
        assertFalse(Backup.isSkipSecuritySettings(params(Backup.PARAM_SKIP_SECURITY_SETTINGS, "false")));
        assertFalse(Backup.isSkipSettings(params(Backup.PARAM_SKIP_SETTINGS, "false")));
        assertFalse(Backup.isPurgeResources(params(Backup.PARAM_PURGE_RESOURCES, "false")));
    }

    @Test
    public void explicitTrueIsHonored() {
        assertTrue(Backup.isSkipSecuritySettings(params(Backup.PARAM_SKIP_SECURITY_SETTINGS, "true")));
        assertTrue(Backup.isSkipSettings(params(Backup.PARAM_SKIP_SETTINGS, "true")));
        assertTrue(Backup.isPurgeResources(params(Backup.PARAM_PURGE_RESOURCES, "true")));
    }
}

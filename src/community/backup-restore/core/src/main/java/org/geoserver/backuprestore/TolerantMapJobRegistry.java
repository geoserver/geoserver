/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.Job;

/**
 * A {@link MapJobRegistry} that tolerates duplicate job registrations instead of aborting start-up.
 *
 * <p>Spring Batch's stock {@link MapJobRegistry} throws {@link DuplicateJobException} when a job name is registered a
 * second time. In the full GeoServer application context the {@code backupJob} and {@code restoreJob} beans are
 * presented to the registry more than once (this does not happen in the leaner test context), which previously aborted
 * GeoServer start-up with {@code FatalBeanException: Cannot register job configuration} &mdash; the "Spring 7
 * incompatibility" that kept this module out of the assembly.
 *
 * <p>The duplicate registrations refer to the same, identically-defined job, so silently keeping the first one is safe
 * and lets the assembly start cleanly.
 */
public class TolerantMapJobRegistry extends MapJobRegistry {

    private static final Logger LOGGER = Logging.getLogger(TolerantMapJobRegistry.class);

    @Override
    public void register(Job job) throws DuplicateJobException {
        try {
            super.register(job);
        } catch (DuplicateJobException e) {
            LOGGER.fine(() -> "Job '"
                    + (job != null ? job.getName() : null)
                    + "' is already registered; keeping the existing registration.");
        }
    }
}

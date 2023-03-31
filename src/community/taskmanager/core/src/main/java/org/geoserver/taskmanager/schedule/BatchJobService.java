/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import java.util.Collection;
import java.util.function.Consumer;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;

/**
 * The batch job is responsible for scheduling the batches.
 *
 * @author Niels Charlier
 */
public interface BatchJobService {

    /**
     * Saves this batch and update the schedule according to its new settings. This may also mean
     * that the batch is actually unscheduled, or that it scheduling is changed, if it was
     * previously scheduled.
     *
     * <p>Batches are scheduled if they are ACTIVE, ENABLED and have a FREQUENCY set. Batches which
     * are ACTIVE, but not ENABLED _or_ have FREQUENCY set to NULL, are known by the quartz
     * scheduler but are never triggered unless explicitly done. Batches which are NOT ACTIVE, are
     * entirely removed from the quartz scheduler.
     *
     * @param batch the batch.
     * @return the saved batch.
     */
    Batch saveAndSchedule(Batch batch);

    /** Refreshes the scheduler completely based on all of the batches in the database. */
    void reloadFromData();

    /**
     * Saves this configuration and update the schedule according to its new settings of each batch.
     *
     * @param config the Configuration.
     * @return the saved config.
     */
    Configuration saveAndSchedule(Configuration config);

    /**
     * Interrupt a batch run. This method will also check if it can verify the batch run has
     * actually already ended (for example when the server was restarted), and if that is the case
     * update its status.
     *
     * @param batchRun the batch run to interrupt
     */
    void interrupt(BatchRun batchRun);

    /**
     * Start a batch right now.
     *
     * @return a (unique) scheduler reference that can be used to identify the batch run
     */
    String scheduleNow(Batch batch);

    /** Remove configuration from database and scheduler at once */
    Configuration remove(Configuration config);

    /** Remove batch from database and scheduler at once */
    Batch remove(Batch batch);

    /**
     * Start a collection of batches right now.
     *
     * @param batches the batches to be run
     * @param waitInSeconds number of seconds to wait before the first batch
     * @param intervalInSeconds number of seconds to wait between batches, may be be zero to
     *     schedule all at once.
     */
    void scheduleNow(Collection<Batch> batches, int waitInSeconds, int intervalInSeconds);

    /**
     * Start a collection of batches right now.
     *
     * @param batches the batches to be run
     * @param waitInSeconds number of seconds to wait before the first batch
     * @param intervalInSeconds number of seconds to wait between batches, may be be zero to
     *     schedule all at once.
     * @param callback run afterwards
     */
    void scheduleNow(
            Collection<Batch> batches,
            int waitInSeconds,
            int intervalInSeconds,
            Consumer<Batch> callback);

    void closeInactiveBatchruns();

    boolean isInit();

    void startup();
}

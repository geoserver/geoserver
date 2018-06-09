/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

/**
 * A handle of a task that was run but must still be committed or rolled back.
 *
 * @author Niels Charlier
 */
public interface TaskResult {

    /** finalize and clean-up resources any roll-back data */
    void commit() throws TaskException;

    /** batch has failed - cancel all changes */
    void rollback() throws TaskException;
}

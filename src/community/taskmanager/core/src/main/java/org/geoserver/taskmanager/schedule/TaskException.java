/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

/**
 * Whenever a tasks fails to run, commit, rollback or clean this exception is called.
 *
 * @author Niels Charlier
 */
public class TaskException extends Exception {

    private static final long serialVersionUID = 2357752792499129080L;

    public TaskException(String message) {
        super(message);
    }

    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskException(Throwable cause) {
        super(cause);
    }
}

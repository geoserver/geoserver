/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

import java.io.Serializable;
import java.util.Date;

/**
 * A particular run of a task within a batch.
 *
 * @author Niels Charlier
 */
public interface Run extends Serializable, Identifiable {

    public enum Status {
        RUNNING,
        FAILED,
        ROLLED_BACK,
        READY_TO_COMMIT,
        COMMITTING,
        COMMITTED,
        NOT_ROLLED_BACK,
        NOT_COMMITTED,
        ROLLING_BACK;

        public boolean isClosed() {
            return this != Status.RUNNING
                    && this != Status.READY_TO_COMMIT
                    && this != Status.COMMITTING
                    && this != Status.ROLLING_BACK;
        }
    }

    Date getStart();

    void setStart(Date start);

    Date getEnd();

    void setEnd(Date end);

    Status getStatus();

    void setStatus(Status status);

    BatchElement getBatchElement();

    void setBatchElement(BatchElement batchElement);

    String getMessage();

    void setMessage(String message);

    BatchRun getBatchRun();

    void setBatchRun(BatchRun br);
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import org.geoserver.taskmanager.data.Run.Status;

public interface BatchRun extends Serializable, Identifiable {

    Batch getBatch();

    void setBatch(Batch batch);

    List<Run> getRuns();

    boolean isInterruptMe();

    void setInterruptMe(boolean interruptMe);

    default Date getStart() {
        return getRuns().isEmpty() ? null : getRuns().get(0).getStart();
    }

    default Date getEnd() {
        return getRuns().isEmpty() ? null : getRuns().get(getRuns().size() - 1).getEnd();
    }

    default Status getStatus() {
        if (getRuns().isEmpty()) {
            return null;
        } else {
            for (int i = getRuns().size() - 1; i >= 0; i--) {
                if (getRuns().get(i).getStatus() != Status.COMMITTED
                        && getRuns().get(i).getStatus() != Status.ROLLED_BACK) {
                    return getRuns().get(i).getStatus();
                }
            }
            return getRuns().get(0).getStatus();
        }
    }

    default String getMessage() {
        if (getRuns().isEmpty()) {
            return null;
        } else {
            for (int i = getRuns().size() - 1; i >= 0; i--) {
                if (getRuns().get(i).getMessage() != null) {
                    return getRuns().get(i).getMessage();
                }
            }
            return null;
        }
    }

    /**
     * The scheduler reference is unique for runs that were started by scheduleNow(), but not for
     * runs automatically started by the time schedule.
     */
    String getSchedulerReference();

    void setSchedulerReference(String qReference);
}

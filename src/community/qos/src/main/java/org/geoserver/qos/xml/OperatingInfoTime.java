/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import java.time.OffsetTime;
import java.util.List;

/** @author Fernando Miño, Geosolutions */
public class OperatingInfoTime implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String TIME_PATTERN = "HH:mm:ssXXX";

    private List<DayOfWeek> days;
    private OffsetTime startTime;
    private OffsetTime endTime;

    public OperatingInfoTime() {}

    public List<DayOfWeek> getDays() {
        return days;
    }

    public void setDays(List<DayOfWeek> days) {
        this.days = days;
    }

    public OffsetTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetTime startTime) {
        this.startTime = startTime;
    }

    public OffsetTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Days:[");
        if (days != null) days.forEach(d -> sb.append(d.value() + " "));
        sb.append("], ");
        return sb.toString();
    }
}

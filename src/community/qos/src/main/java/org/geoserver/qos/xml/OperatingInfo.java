/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.io.Serializable;
import java.util.List;

@XStreamAlias(value = "operatingInfo")
public class OperatingInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private ReferenceType operationalStatus;
    private String description;
    private List<OperatingInfoTime> byDaysOfWeek;

    public OperatingInfo() {}

    public OperatingInfo(
            ReferenceType operationalStatus,
            String description,
            List<OperatingInfoTime> byDaysOfWeek) {
        super();
        this.operationalStatus = operationalStatus;
        this.description = description;
        this.byDaysOfWeek = byDaysOfWeek;
    }

    public ReferenceType getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(ReferenceType operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<OperatingInfoTime> getByDaysOfWeek() {
        return byDaysOfWeek;
    }

    public void setByDaysOfWeek(List<OperatingInfoTime> byDaysOfWeek) {
        this.byDaysOfWeek = byDaysOfWeek;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("operationalStatus:{ \r\n");
        sb.append(operationalStatus.toString());
        sb.append(" }");
        sb.append("byDaysOfWeek:[");
        byDaysOfWeek.forEach(
                bd -> {
                    sb.append(bd.toString());
                    sb.append("\r\n");
                });
        sb.append("]");
        return sb.toString();
    }
}

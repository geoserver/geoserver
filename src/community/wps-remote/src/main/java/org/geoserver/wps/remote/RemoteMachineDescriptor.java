/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import org.opengis.feature.type.Name;

/**
 * Just a utility class to store info associated to the remote processing machines
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class RemoteMachineDescriptor {

    private String nodeJID;

    private Name serviceName;

    private Boolean available;

    private Double memPercUsed;

    private Double loadAverage;

    /** */
    public RemoteMachineDescriptor(
            String nodeJID,
            Name serviceName,
            Boolean available,
            Double memPercUsed,
            Double loadAverage) {
        super();
        this.nodeJID = nodeJID;
        this.serviceName = serviceName;
        this.available = available;
        this.memPercUsed = memPercUsed;
        this.loadAverage = loadAverage;
    }

    /** @return the nodeJID */
    public String getNodeJID() {
        return nodeJID;
    }

    /** @param nodeJID the nodeJID to set */
    public void setNodeJID(String nodeJID) {
        this.nodeJID = nodeJID;
    }

    /** @return the serviceName */
    public Name getServiceName() {
        return serviceName;
    }

    /** @param serviceName the serviceName to set */
    public void setServiceName(Name serviceName) {
        this.serviceName = serviceName;
    }

    /** @return the available */
    public Boolean getAvailable() {
        return available;
    }

    /** @param available the available to set */
    public void setAvailable(Boolean available) {
        this.available = available;
    }

    /** @return the memPercUsed */
    public Double getMemPercUsed() {
        return memPercUsed;
    }

    /** @param memPercUsed the memPercUsed to set */
    public void setMemPercUsed(Double memPercUsed) {
        this.memPercUsed = memPercUsed;
    }

    /** @return the loadAverage */
    public Double getLoadAverage() {
        return loadAverage;
    }

    /** @param loadAverage the loadAverage to set */
    public void setLoadAverage(Double loadAverage) {
        this.loadAverage = loadAverage;
    }
}

/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

/**
 * The get executions list request
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetExecutionsType {

    String service;

    String version;

    String owner;

    String identifier;

    String status;

    String orderBy;

    Integer startIndex;

    Integer maxFeatures;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    /** @return the startIndex */
    public Integer getStartIndex() {
        return startIndex;
    }

    /** @param startIndex the startIndex to set */
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    /** @return the maxFeatures */
    public Integer getMaxFeatures() {
        return maxFeatures;
    }

    /** @param maxFeatures the maxFeatures to set */
    public void setMaxFeatures(Integer maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    @Override
    public String toString() {
        return "StatusType [service="
                + service
                + ", version="
                + version
                + ", owner="
                + owner
                + ", identifier="
                + identifier
                + ", status="
                + status
                + ", orderBy="
                + orderBy
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 7961;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((orderBy == null) ? 0 : orderBy.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((startIndex == null) ? 0 : startIndex.hashCode());
        result = prime * result + ((maxFeatures == null) ? 0 : maxFeatures.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        GetExecutionsType other = (GetExecutionsType) obj;
        if (owner == null) {
            if (other.owner != null) return false;
        } else if (!owner.equals(other.owner)) return false;
        if (identifier == null) {
            if (other.identifier != null) return false;
        } else if (!identifier.equals(other.identifier)) return false;
        if (status == null) {
            if (other.status != null) return false;
        } else if (!status.equals(other.status)) return false;
        if (orderBy == null) {
            if (other.orderBy != null) return false;
        } else if (!orderBy.equals(other.orderBy)) return false;
        if (service == null) {
            if (other.service != null) return false;
        } else if (!service.equals(other.service)) return false;
        if (version == null) {
            if (other.version != null) return false;
        } else if (!version.equals(other.version)) return false;
        if (startIndex == null) {
            if (other.startIndex != null) return false;
        } else if (!startIndex.equals(other.startIndex)) return false;
        if (maxFeatures == null) {
            if (other.maxFeatures != null) return false;
        } else if (!maxFeatures.equals(other.maxFeatures)) return false;
        return true;
    }
}

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.io.Serializable;
import java.util.List;

/**
 * QoS Metadata xml
 *
 * @author Fernando Miño, Geosolutions
 */
@XStreamAlias(value = "metadata")
public class QoSMetadata implements Serializable {
    @XStreamImplicit(itemFieldName = "operatingInfo")
    private List<OperatingInfo> operatingInfo;

    private List<QualityOfServiceStatement> statements;
    private List<ReferenceType> operationAnomalyFeed;
    private List<QosRepresentativeOperation> representativeOperations;

    public QoSMetadata() {}

    public List<OperatingInfo> getOperatingInfo() {
        return operatingInfo;
    }

    public void setOperatingInfo(List<OperatingInfo> operatingInfo) {
        this.operatingInfo = operatingInfo;
    }

    public List<QualityOfServiceStatement> getStatements() {
        return statements;
    }

    public void setStatements(List<QualityOfServiceStatement> statements) {
        this.statements = statements;
    }

    public List<ReferenceType> getOperationAnomalyFeed() {
        return operationAnomalyFeed;
    }

    public void setOperationAnomalyFeed(List<ReferenceType> operationAnomalyFeed) {
        this.operationAnomalyFeed = operationAnomalyFeed;
    }

    public List<QosRepresentativeOperation> getRepresentativeOperations() {
        return representativeOperations;
    }

    public void setRepresentativeOperations(
            List<QosRepresentativeOperation> representativeOperations) {
        this.representativeOperations = representativeOperations;
    }
}

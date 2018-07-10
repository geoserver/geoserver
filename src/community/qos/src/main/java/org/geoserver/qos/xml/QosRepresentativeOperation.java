/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import java.util.List;

public class QosRepresentativeOperation implements Serializable {

    /* WMS */
    private List<QosWMSOperation> getMapOperations;
    private List<QosWMSOperation> getFeatureInfoOperations;

    /* common use */
    private List<QualityOfServiceStatement> qualityOfServiceStatements;

    /* WFS */
    private List<WfsGetFeatureOperation> getFeatureOperations;

    public QosRepresentativeOperation() {}

    public List<QosWMSOperation> getGetMapOperations() {
        return getMapOperations;
    }

    public void setGetMapOperations(List<QosWMSOperation> getMapOperations) {
        this.getMapOperations = getMapOperations;
    }

    public List<QosWMSOperation> getGetFeatureInfoOperations() {
        return getFeatureInfoOperations;
    }

    public void setGetFeatureInfoOperations(List<QosWMSOperation> getFeatureInfoOperations) {
        this.getFeatureInfoOperations = getFeatureInfoOperations;
    }

    public List<QualityOfServiceStatement> getQualityOfServiceStatements() {
        return qualityOfServiceStatements;
    }

    public void setQualityOfServiceStatements(
            List<QualityOfServiceStatement> qualityOfServiceStatements) {
        this.qualityOfServiceStatements = qualityOfServiceStatements;
    }

    public List<WfsGetFeatureOperation> getGetFeatureOperations() {
        return getFeatureOperations;
    }

    public void setGetFeatureOperations(List<WfsGetFeatureOperation> getFeatureOperations) {
        this.getFeatureOperations = getFeatureOperations;
    }
}

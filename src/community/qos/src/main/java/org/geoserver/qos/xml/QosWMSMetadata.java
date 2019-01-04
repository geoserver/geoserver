/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import java.util.List;

public class QosWMSMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<WMSGetMapOperation> getMapOperation;
    private List<WMSGetFeatureInfoOperation> getFeatureInfoOperation;

    public QosWMSMetadata() {}

    public List<WMSGetMapOperation> getGetMapOperation() {
        return getMapOperation;
    }

    public void setGetMapOperation(List<WMSGetMapOperation> getMapOperation) {
        this.getMapOperation = getMapOperation;
    }

    public List<WMSGetFeatureInfoOperation> getGetFeatureInfoOperation() {
        return getFeatureInfoOperation;
    }

    public void setGetFeatureInfoOperation(
            List<WMSGetFeatureInfoOperation> getFeatureInfoOperation) {
        this.getFeatureInfoOperation = getFeatureInfoOperation;
    }
}

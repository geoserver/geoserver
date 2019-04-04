/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import java.util.List;

public class AbstractOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    // ows:DCP list
    private List<OwsDCP> dcp;
    // Constrain    type: ows:DomainType [0...*]
    private List<OwsDomainType> constrain;

    public AbstractOperation() {}

    public List<OwsDCP> getDcp() {
        return dcp;
    }

    public void setDcp(List<OwsDCP> dcp) {
        this.dcp = dcp;
    }

    public List<OwsDomainType> getConstrain() {
        return constrain;
    }

    public void setConstrain(List<OwsDomainType> constrain) {
        this.constrain = constrain;
    }
}

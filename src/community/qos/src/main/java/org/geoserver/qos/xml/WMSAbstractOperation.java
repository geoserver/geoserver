/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.util.List;

public abstract class WMSAbstractOperation extends AbstractOperation {

    // <element name="RequestOption" type="qos:LimitedAreaRequestConstraintsType"
    // maxOccurs="unbounded" />
    private List<LimitedAreaRequestConstraints> requestOption;

    public List<LimitedAreaRequestConstraints> getRequestOption() {
        return requestOption;
    }

    public void setRequestOption(List<LimitedAreaRequestConstraints> requestOption) {
        this.requestOption = requestOption;
    }
}

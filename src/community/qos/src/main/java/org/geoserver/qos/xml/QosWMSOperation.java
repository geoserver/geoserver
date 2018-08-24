/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.util.List;
import org.geoserver.qos.util.ListUtl;

public class QosWMSOperation extends QosAbstractOperation {

    private List<LimitedAreaRequestConstraints> requestOptions;

    public QosWMSOperation() {}

    public List<LimitedAreaRequestConstraints> getRequestOptions() {
        return requestOptions;
    }

    public void setRequestOptions(List<LimitedAreaRequestConstraints> requestOptions) {
        this.requestOptions = requestOptions;
    }

    public static List<String> httpMethods() {
        return ListUtl.asList("GET", "POST");
    }
}

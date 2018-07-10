/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;

public class OwsHttp implements Serializable {
    private static final long serialVersionUID = 1L;

    private OwsRequestType requestType;

    public OwsHttp() {}

    //    @XmlElements({
    //        @XmlElement(name="GET", type=OwsGET.class),
    //        @XmlElement(name="POST", type=OwsPOST.class)
    //    })
    public OwsRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(OwsRequestType requestType) {
        this.requestType = requestType;
    }
}

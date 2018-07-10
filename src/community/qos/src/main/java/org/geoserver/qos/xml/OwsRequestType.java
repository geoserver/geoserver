/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "RequestType", namespace = QoSConfiguration.OWS_NS)
public abstract class OwsRequestType implements Serializable {
    private static final long serialVersionUID = 1L;

    public OwsRequestType() {}
}

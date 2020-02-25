/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "POST", namespace = QoSConfiguration.OWS_NS)
public class OwsPOST extends OwsRequestType {
    private static final long serialVersionUID = 1L;

    public OwsPOST() {}
}

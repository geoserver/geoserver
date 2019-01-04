/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.io.Serializable;

public class QoSConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String QOS_NS = "http://www.opengis.net/qos/1.0";
    public static final String OWS_NS = "http://www.opengis.net/ows/2.0";
    public static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    @XStreamAlias(value = "activated", impl = Boolean.class)
    protected Boolean activated;

    public QoSConfiguration() {}

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }
}

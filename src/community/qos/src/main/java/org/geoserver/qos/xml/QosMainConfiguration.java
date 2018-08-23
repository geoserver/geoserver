/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(value = "qosMainConfiguration")
public class QosMainConfiguration extends QoSConfiguration {
    private static final long serialVersionUID = 1L;

    @XStreamAlias(value = "qosMetadata")
    private QosMainMetadata metadata;

    public QosMainConfiguration() {}

    public QosMainMetadata getWmsQosMetadata() {
        return metadata;
    }

    public void setWmsQosMetadata(QosMainMetadata wmsQosMetadata) {
        this.metadata = wmsQosMetadata;
    }
}

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos;

public final class QosSchema {

    public static final String QOS_PREFIX = "qos";
    public static final String QOS_NAMESPACE = "http://www.opengis.net/qos/1.0";
    public static final String QOS_SCHEMA = "./ows-qos-common.xsd";

    public static final String QOS_WMS_PREFIX = "qos-wms";
    public static final String QOS_WMS_NAMESPACE = "http://www.opengis.net/qos/wms/1.0";
    public static final String QOS_WMS_SCHEMA = "../ows-qos-capabilities_wms.xsd";

    public static final String QOS_WFS_PREFIX = "qos-wfs";
    public static final String QOS_WFS_NAMESPACE = "http://www.opengis.net/qos/wms/1.0";
    public static final String QOS_WFS_SCHEMA = "../ows-qos-capabilities_wms.xsd";

    public static final String OWS_PREFIX = "ows";
    public static final String OWS_NAMESPACE = "http://www.opengis.net/ows/2.0";

    private QosSchema() {}
}

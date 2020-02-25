/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.wfs;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.qos.BaseConfigurationLoader;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.wfs.WFSInfo;

public class WfsQosConfigurationLoader extends BaseConfigurationLoader<WFSInfo> {

    public static final String FILE_NAME = "qos_wfs.xml";
    public static final String SPRING_BEAN_NAME = "wfsQosConfigurationLoader";

    public WfsQosConfigurationLoader() {
        super();
    }

    public WfsQosConfigurationLoader(GeoServerDataDirectory dataDirectory) {
        super(dataDirectory);
    }

    @Override
    protected String getFileName() {
        return FILE_NAME;
    }

    @Override
    protected void validate(QosMainConfiguration config) {
        // pending
    }
}

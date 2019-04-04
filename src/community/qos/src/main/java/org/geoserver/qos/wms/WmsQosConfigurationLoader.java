/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.wms;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.qos.BaseConfigurationLoader;
import org.geoserver.qos.QosMainConfigurationWMSValidator;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.wms.WMSInfo;

/**
 * Loads the configuration file in workspace or global
 *
 * @author Fernando Miño, Geosolutions
 */
public class WmsQosConfigurationLoader extends BaseConfigurationLoader<WMSInfo> {

    public static final String SPRING_KEY = "wmsQosConfigurationLoader";
    public static final String FILE_NAME = "qos_wms.xml";

    public WmsQosConfigurationLoader() {}

    public WmsQosConfigurationLoader(GeoServerDataDirectory dataDirectory) {
        super(dataDirectory);
    }

    @Override
    protected void validate(QosMainConfiguration config) {
        QosMainConfigurationWMSValidator validator = new QosMainConfigurationWMSValidator();
        validator.validate(config);
    }

    @Override
    protected String getFileName() {
        return FILE_NAME;
    }
}

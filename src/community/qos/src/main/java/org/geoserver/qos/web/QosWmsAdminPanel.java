/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import org.apache.wicket.model.IModel;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.qos.wms.WmsQosConfigurationLoader;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.wms.WMSInfo;

/** @author Fernando Miño, Geosolutions */
public class QosWmsAdminPanel extends QosAdminPanel {
    private static final long serialVersionUID = 1L;

    public QosWmsAdminPanel(String id, IModel<ServiceInfo> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public void onMainFormSubmit() {
        getLoader().setConfiguration((WMSInfo) serviceInfo, config);
    }

    protected RepresentativeOperationPanel buildRepOperationPanel(
            String id, IModel<QosRepresentativeOperation> model) {
        return new WmsRepresentativeOperationPanel("repOperationPanel", model);
    }

    @Override
    protected WmsQosConfigurationLoader getLoader() {
        return (WmsQosConfigurationLoader)
                GeoServerExtensions.bean(WmsQosConfigurationLoader.SPRING_KEY);
    }
}

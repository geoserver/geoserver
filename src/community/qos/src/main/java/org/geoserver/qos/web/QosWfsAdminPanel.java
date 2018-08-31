/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import org.apache.wicket.model.IModel;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.qos.wfs.WfsQosConfigurationLoader;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.wfs.WFSInfo;

public class QosWfsAdminPanel extends QosAdminPanel {

    public QosWfsAdminPanel(String id, IModel<ServiceInfo> model) {
        super(id, model);
    }

    @Override
    public void onMainFormSubmit() {
        getLoader().setConfiguration((WFSInfo) serviceInfo, config);
    }

    @Override
    protected RepresentativeOperationPanel buildRepOperationPanel(
            String id, IModel<QosRepresentativeOperation> model) {
        return new WfsRepresentativeOperationPanel(id, model);
    }

    @Override
    protected WfsQosConfigurationLoader getLoader() {
        return (WfsQosConfigurationLoader)
                GeoServerExtensions.bean(WfsQosConfigurationLoader.SPRING_BEAN_NAME);
    }
}

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.qos.xml.QosRepresentativeOperation;

public class WfsRepresentativeOperationPanel extends RepresentativeOperationPanel {

    public WfsRepresentativeOperationPanel(String id, IModel<QosRepresentativeOperation> model) {
        super(id, model);
        initExtraComponents(model);
    }

    protected void initExtraComponents(IModel<QosRepresentativeOperation> model) {
        // GetFeaturesOperation
        final WfsGetFeatureOperationPanelList getFeaturesOperationList =
                new WfsGetFeatureOperationPanelList(
                        "getFeaturesOperationList",
                        new PropertyModel<>(model, "getFeatureOperations"));
        this.mainDiv.add(getFeaturesOperationList);
    }
}

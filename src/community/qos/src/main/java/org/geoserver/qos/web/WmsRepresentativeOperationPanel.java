/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.List;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.qos.xml.QosWMSOperation;

public class WmsRepresentativeOperationPanel extends RepresentativeOperationPanel {

    protected QosWMSOperationsListPanel getMapOperationsPanel;
    protected QosWMSOperationsListPanel getFeatureInfoOperationsPanel;

    public WmsRepresentativeOperationPanel(String id, IModel<QosRepresentativeOperation> model) {
        super(id, model);
        initExtraComponents();
    }

    protected void initExtraComponents() {
        getMapOperationsPanel =
                new QosWMSOperationsListPanel(
                        "getMapOperationsPanel",
                        new PropertyModel<List<QosWMSOperation>>(repOpModel, "getMapOperations"));
        mainDiv.add(getMapOperationsPanel);

        getFeatureInfoOperationsPanel =
                new QosWMSOperationsListPanel(
                        "getFeatureInfoOperationsPanel",
                        new PropertyModel<List<QosWMSOperation>>(
                                repOpModel, "getFeatureInfoOperations")) {
                    @Override
                    public LimitedConstraintsPanel buildConstraintsPanel(
                            String id, IModel<LimitedAreaRequestConstraints> model) {
                        return new GetFeatureInfoConstraintsPanel(id, model);
                    }
                };
        mainDiv.add(getFeatureInfoOperationsPanel);
    }
}

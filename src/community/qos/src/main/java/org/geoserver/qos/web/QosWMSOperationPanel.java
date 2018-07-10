/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.qos.xml.QosWMSOperation;

public class QosWMSOperationPanel extends Panel {

    protected IModel<QosWMSOperation> wmsOpModel;
    protected WebMarkupContainer opDiv;
    protected SerializableConsumer<AjaxTargetAndModel<QosWMSOperation>> onDeleteCallback;

    public QosWMSOperationPanel(String id, IModel<QosWMSOperation> model) {
        super(id, model);
        wmsOpModel = model;

        opDiv = new WebMarkupContainer("opDiv");
        opDiv.setOutputMarkupId(true);
        add(opDiv);

        final DropDownChoice<String> methodSelect =
                new DropDownChoice<>(
                        "methodSelect",
                        new PropertyModel<>(wmsOpModel.getObject(), "httpMethod"),
                        QosWMSOperation.httpMethods());
        opDiv.add(methodSelect);

        final LimitedConstraintsPanelList requestOptionsPanelList =
                buildPanelList(
                        "requestOptionsPanelList",
                        new PropertyModel<>(wmsOpModel.getObject(), "requestOptions"));
        opDiv.add(requestOptionsPanelList);

        final AjaxSubmitLink deleteLink =
                new AjaxSubmitLink("deleteLink") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        if (onDeleteCallback != null)
                            onDeleteCallback.accept(
                                    new AjaxTargetAndModel<QosWMSOperation>(
                                            model.getObject(), target));
                    }
                };
        opDiv.add(deleteLink);
    }

    public void setOnDeleteCallback(
            SerializableConsumer<AjaxTargetAndModel<QosWMSOperation>> onDeleteCallback) {
        this.onDeleteCallback = onDeleteCallback;
    }

    protected LimitedConstraintsPanelList buildPanelList(
            String id, IModel<List<LimitedAreaRequestConstraints>> model) {
        return new LimitedConstraintsPanelList(id, model);
    }
}

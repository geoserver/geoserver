/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.qos.xml.QosWMSOperation;

public class QosWMSOperationsListPanel extends Panel {

    protected IModel<List<QosWMSOperation>> wmsOpModel;

    public QosWMSOperationsListPanel(String id, IModel<List<QosWMSOperation>> model) {
        super(id, model);
        wmsOpModel = model;

        final WebMarkupContainer divOp = new WebMarkupContainer("opDiv");
        divOp.setOutputMarkupId(true);
        add(divOp);

        final ListView<QosWMSOperation> wmsOperationsListView =
                new ListView<QosWMSOperation>("wmsOperationsList", model) {
                    @Override
                    protected void populateItem(ListItem<QosWMSOperation> item) {
                        QosWMSOperationPanel panel =
                                new QosWMSOperationPanel("wmsOpPanel", item.getModel());
                        panel.setOnDeleteCallback(
                                x -> {
                                    if (model.getObject() != null)
                                        model.getObject().remove(x.getModel());
                                    x.getTarget().add(divOp);
                                });
                        item.add(panel);
                    }
                };
        divOp.add(wmsOperationsListView);

        final AjaxSubmitLink addLink =
                new AjaxSubmitLink("addLink") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        if (wmsOpModel.getObject() == null)
                            wmsOpModel.setObject(new ArrayList<QosWMSOperation>());
                        wmsOpModel.getObject().add(new QosWMSOperation());
                        target.add(divOp);
                    }
                };
        divOp.add(addLink);
    }

    public LimitedConstraintsPanel buildConstraintsPanel(
            String id, IModel<LimitedAreaRequestConstraints> model) {
        return new LimitedConstraintsPanel(id, model);
    }
}

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

public class LimitedConstraintsPanelList extends Panel {

    protected WebMarkupContainer mainDiv;
    protected IModel<List<LimitedAreaRequestConstraints>> mainModel;

    public LimitedConstraintsPanelList(
            String id, IModel<List<LimitedAreaRequestConstraints>> model) {
        super(id, model);
        mainModel = model;

        mainDiv = new WebMarkupContainer("mainDiv");
        mainDiv.setOutputMarkupId(true);
        add(mainDiv);

        final AjaxSubmitLink addLink =
                new AjaxSubmitLink("addLink") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        if (mainModel.getObject() == null) mainModel.setObject(new ArrayList<>());
                        mainModel.getObject().add(new LimitedAreaRequestConstraints());
                        target.add(mainDiv);
                    }
                };
        mainDiv.add(addLink);

        final ListView<LimitedAreaRequestConstraints> constrainsList =
                new ListView<LimitedAreaRequestConstraints>("list", model) {
                    @Override
                    protected void populateItem(ListItem<LimitedAreaRequestConstraints> item) {
                        LimitedConstraintsPanel panel =
                                buildConstraintsPanel("innerPanel", item.getModel());
                        panel.setOnDelete(
                                x -> {
                                    if (mainModel.getObject() == null)
                                        mainModel.setObject(new ArrayList<>());
                                    mainModel.getObject().add(new LimitedAreaRequestConstraints());
                                    x.getTarget().add(mainDiv);
                                });
                        item.add(panel);
                    }
                };
        mainDiv.add(constrainsList);
    }

    protected LimitedConstraintsPanel buildConstraintsPanel(
            String id, IModel<LimitedAreaRequestConstraints> model) {
        QosWMSOperationsListPanel opPanel = this.findParent(QosWMSOperationsListPanel.class);
        if (opPanel != null) return opPanel.buildConstraintsPanel(id, model);
        return new LimitedConstraintsPanel(id, model);
    }
}

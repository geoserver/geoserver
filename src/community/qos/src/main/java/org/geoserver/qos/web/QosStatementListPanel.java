/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.qos.xml.QualityOfServiceStatement;

public class QosStatementListPanel extends Panel {

    protected IModel<List<QualityOfServiceStatement>> statementsModel;

    public QosStatementListPanel(String id, IModel<List<QualityOfServiceStatement>> model) {
        super(id, model);
        statementsModel = model;

        final WebMarkupContainer statementsDiv = new WebMarkupContainer("statemenstDiv");
        statementsDiv.setOutputMarkupId(true);
        add(statementsDiv);

        final ListView<QualityOfServiceStatement> statementsListView =
                new ListView<QualityOfServiceStatement>("statementsListView", statementsModel) {
                    @Override
                    protected void populateItem(ListItem<QualityOfServiceStatement> item) {
                        QualityOfServiceStatementPanel panel =
                                new QualityOfServiceStatementPanel(
                                        "statementPanel", item.getModel());
                        // onDelete
                        panel.setOnDelete(
                                x -> {
                                    if (statementsModel.getObject() != null)
                                        statementsModel.getObject().remove(x.getModel());
                                    x.getTarget().add(statementsDiv);
                                });
                        item.add(panel);
                    }
                };
        statementsDiv.add(statementsListView);

        final AjaxButton addStatementButton = new AjaxButton("addStatement") {};
        statementsDiv.add(addStatementButton);
        addStatementButton.add(
                new AjaxFormSubmitBehavior("click") {
                    @Override
                    protected void onAfterSubmit(final AjaxRequestTarget target) {
                        if (statementsModel.getObject() == null)
                            statementsModel.setObject(new ArrayList<>());
                        statementsModel.getObject().add(new QualityOfServiceStatement());
                        target.add(statementsDiv);
                    }
                });
    }
}

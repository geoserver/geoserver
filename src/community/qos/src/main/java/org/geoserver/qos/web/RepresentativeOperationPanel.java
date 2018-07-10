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
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.qos.xml.QualityOfServiceStatement;

public abstract class RepresentativeOperationPanel extends Panel {

    protected IModel<QosRepresentativeOperation> repOpModel;
    protected SerializableConsumer<AjaxTargetAndModel<QosRepresentativeOperation>> onDelete;

    protected WebMarkupContainer mainDiv;

    public RepresentativeOperationPanel(String id, IModel<QosRepresentativeOperation> model) {
        super(id, model);
        repOpModel = model;
        mainDiv = new WebMarkupContainer("opDiv");
        mainDiv.setOutputMarkupId(true);
        add(mainDiv);

        final QosStatementListPanel statementsPanel =
                new QosStatementListPanel(
                        "statementsPanel",
                        new PropertyModel<List<QualityOfServiceStatement>>(
                                model, "qualityOfServiceStatements"));
        mainDiv.add(statementsPanel);

        final AjaxSubmitLink deleteLink =
                new AjaxSubmitLink("deleteLink") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        if (onDelete != null) {
                            onDelete.accept(
                                    new AjaxTargetAndModel<QosRepresentativeOperation>(
                                            model.getObject(), target));
                        }
                    }
                };
        mainDiv.add(deleteLink);
    }

    public void setOnDelete(
            SerializableConsumer<AjaxTargetAndModel<QosRepresentativeOperation>> onDelete) {
        this.onDelete = onDelete;
    }
}

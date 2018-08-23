/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.qos.xml.QosWMSOperation;
import org.geoserver.qos.xml.WfsGetFeatureOperation;

public class WfsGetFeatureOperationPanel extends Panel {

    private SerializableConsumer<AjaxTargetAndModel<WfsGetFeatureOperation>> onDeleteCallback;

    public WfsGetFeatureOperationPanel(String id, IModel<WfsGetFeatureOperation> model) {
        super(id, model);

        final WebMarkupContainer opDiv = new WebMarkupContainer("mainDiv");
        opDiv.setOutputMarkupId(true);
        add(opDiv);

        final DropDownChoice<String> methodSelect =
                new DropDownChoice<>(
                        "methodSelect",
                        new PropertyModel<>(model, "httpMethod"),
                        QosWMSOperation.httpMethods());
        opDiv.add(methodSelect);

        // AdHocQueryConstraints PanelList
        final WfsAdHocQueryConstraintsPanelList adHocsPanel =
                new WfsAdHocQueryConstraintsPanelList(
                        "AdHocsPanel", new PropertyModel<>(model, "adHocQueryConstraints"));
        opDiv.add(adHocsPanel);

        final AjaxSubmitLink deleteLink =
                new AjaxSubmitLink("deleteLink") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        if (onDeleteCallback != null)
                            onDeleteCallback.accept(
                                    new AjaxTargetAndModel<WfsGetFeatureOperation>(
                                            model.getObject(), target));
                    }
                };
        opDiv.add(deleteLink);
    }

    public SerializableConsumer<AjaxTargetAndModel<WfsGetFeatureOperation>> getOnDeleteCallback() {
        return onDeleteCallback;
    }

    public void setOnDeleteCallback(
            SerializableConsumer<AjaxTargetAndModel<WfsGetFeatureOperation>> onDeleteCallback) {
        this.onDeleteCallback = onDeleteCallback;
    }
}

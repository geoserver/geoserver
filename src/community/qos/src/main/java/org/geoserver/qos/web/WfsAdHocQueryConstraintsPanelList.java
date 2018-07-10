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
import org.geoserver.qos.xml.WfsAdHocQueryConstraints;

public class WfsAdHocQueryConstraintsPanelList extends Panel {

    protected WebMarkupContainer mainDiv;

    public WfsAdHocQueryConstraintsPanelList(
            String id, IModel<List<WfsAdHocQueryConstraints>> model) {
        super(id, model);

        mainDiv = new WebMarkupContainer("mainDiv");
        mainDiv.setOutputMarkupId(true);
        add(mainDiv);

        final AjaxSubmitLink addLink =
                new AjaxSubmitLink("addLink") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        if (model.getObject() == null) model.setObject(new ArrayList<>());
                        model.getObject().add(new WfsAdHocQueryConstraints());
                        target.add(mainDiv);
                    }
                };
        mainDiv.add(addLink);

        final ListView<WfsAdHocQueryConstraints> constrainsList =
                new ListView<WfsAdHocQueryConstraints>("list", model) {
                    @Override
                    protected void populateItem(ListItem<WfsAdHocQueryConstraints> item) {
                        WfsAdHocQueryConstraintsPanel panel =
                                new WfsAdHocQueryConstraintsPanel("innerPanel", item.getModel());
                        panel.setOnDelete(
                                x -> {
                                    if (model.getObject() == null)
                                        model.setObject(new ArrayList<>());
                                    model.getObject().add(new WfsAdHocQueryConstraints());
                                    x.getTarget().add(mainDiv);
                                });
                        item.add(panel);
                    }
                };
        mainDiv.add(constrainsList);
    }
}

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
import org.geoserver.qos.xml.WfsGetFeatureOperation;

public class WfsGetFeatureOperationPanelList extends Panel {

    public WfsGetFeatureOperationPanelList(String id, IModel<List<WfsGetFeatureOperation>> model) {
        super(id, model);

        final WebMarkupContainer divOp = new WebMarkupContainer("mainDiv");
        divOp.setOutputMarkupId(true);
        add(divOp);

        final ListView<WfsGetFeatureOperation> getFeatureOperationList =
                new ListView<WfsGetFeatureOperation>("listView", model) {
                    @Override
                    protected void populateItem(ListItem<WfsGetFeatureOperation> item) {
                        WfsGetFeatureOperationPanel panel =
                                new WfsGetFeatureOperationPanel("itemPanel", item.getModel());
                        panel.setOnDeleteCallback(
                                x -> {
                                    if (model.getObject() != null)
                                        model.getObject().remove(x.getModel());
                                    x.getTarget().add(divOp);
                                });
                        item.add(panel);
                    }
                };
        divOp.add(getFeatureOperationList);

        final AjaxSubmitLink addLink =
                new AjaxSubmitLink("addLink") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        if (model.getObject() == null)
                            model.setObject(new ArrayList<WfsGetFeatureOperation>());
                        model.getObject().add(new WfsGetFeatureOperation());
                        target.add(divOp);
                    }
                };
        divOp.add(addLink);
    }
}

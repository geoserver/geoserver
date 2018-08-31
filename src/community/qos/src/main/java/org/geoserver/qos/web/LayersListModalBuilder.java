/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.io.Serializable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.web.data.layergroup.LayerListPanel;

public class LayersListModalBuilder
        implements Serializable, LayersListBuilder<LimitedAreaRequestConstraints> {

    public LayersListModalBuilder() {}

    /* (non-Javadoc)
     * @see org.geoserver.qos.web.LayersListBuilder#build()
     */
    @Override
    public WebMarkupContainer build(
            WebMarkupContainer mainDiv,
            ModalWindow modalWindow,
            IModel<LimitedAreaRequestConstraints> model) {
        WebMarkupContainer layersDiv = new WebMarkupContainer("layersDiv");
        layersDiv.setOutputMarkupId(true);
        mainDiv.add(layersDiv);

        final ListView<String> layersListView =
                new ListView<String>("layersList", new PropertyModel<>(model, "layerNames")) {
                    @Override
                    protected void populateItem(ListItem<String> item) {
                        TextField<String> layerField =
                                new TextField<>("layerName", item.getModel());
                        layerField.setEnabled(false);
                        item.add(layerField);
                        AjaxSubmitLink deleteLayerLink =
                                new AjaxSubmitLink("deleteLayer") {
                                    @Override
                                    protected void onAfterSubmit(
                                            AjaxRequestTarget target, Form<?> form) {
                                        super.onAfterSubmit(target, form);
                                        model.getObject()
                                                .getLayerNames()
                                                .remove(item.getModel().getObject());
                                        target.add(mainDiv);
                                    }
                                };
                        item.add(deleteLayerLink);
                    }
                };
        layersDiv.add(layersListView);

        final AjaxLink addLayerLink =
                new AjaxLink("addLayer") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        modalWindow.setInitialHeight(375);
                        modalWindow.setInitialWidth(525);
                        modalWindow.setTitle("Choose layer");
                        WorkspaceInfo wsi =
                                mainDiv.findParent(QosWmsAdminPanel.class)
                                        .getMainModel()
                                        .getObject()
                                        .getWorkspace();
                        modalWindow.setContent(
                                new LayerListPanel(modalWindow.getContentId(), wsi) {
                                    @Override
                                    protected void handleLayer(
                                            org.geoserver.catalog.LayerInfo layer,
                                            AjaxRequestTarget target) {
                                        if (!model.getObject()
                                                .getLayerNames()
                                                .contains(model.getObject().getLayerNames())) {
                                            model.getObject()
                                                    .getLayerNames()
                                                    .add(layer.prefixedName());
                                        }
                                        modalWindow.close(target);
                                        target.add(mainDiv);
                                    };
                                });
                        modalWindow.show(target);
                    }
                };
        layersDiv.add(addLayerLink);
        return layersDiv;
    }
}

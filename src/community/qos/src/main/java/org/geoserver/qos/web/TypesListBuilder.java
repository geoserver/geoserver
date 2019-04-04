/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.ArrayList;
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
import org.geoserver.qos.xml.WfsAdHocQueryConstraints;
import org.geoserver.web.data.layergroup.LayerListPanel;

public class TypesListBuilder implements LayersListBuilder<WfsAdHocQueryConstraints> {

    public TypesListBuilder() {}

    @Override
    public WebMarkupContainer build(
            WebMarkupContainer mainDiv,
            ModalWindow modalWindow,
            IModel<WfsAdHocQueryConstraints> model) {
        WebMarkupContainer layersDiv = new WebMarkupContainer("layersDiv");
        layersDiv.setOutputMarkupId(true);
        mainDiv.add(layersDiv);

        final ListView<String> layersListView =
                new ListView<String>("layersList", new PropertyModel<>(model, "typeNames")) {
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
                                                .getTypeNames()
                                                .remove(item.getModel().getObject());
                                        target.add(mainDiv);
                                    }
                                };
                        item.add(deleteLayerLink);
                    }
                };
        layersDiv.add(layersListView);

        // Autocomplete add to list:
        //        final AutoCompleteTextField<String> addTypeNameField =
        //                new AutoCompleteTextField<String>("addTypeNameField", new
        // PropertyModel<>(typeToAdd, "value")) {
        //                    @Override
        //                    protected Iterator<String> getChoices(String arg0) {
        //                        return null;
        //                    }
        //                };

        final AjaxLink addLayerLink =
                new AjaxLink("addLayer") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        WorkspaceInfo wsi =
                                mainDiv.findParent(QosWfsAdminPanel.class)
                                        .getMainModel()
                                        .getObject()
                                        .getWorkspace();
                        modalWindow.setInitialHeight(375);
                        modalWindow.setInitialWidth(525);
                        modalWindow.setTitle("Choose layer");
                        modalWindow.setContent(
                                new LayerListPanel(modalWindow.getContentId(), wsi) {
                                    @Override
                                    protected void handleLayer(
                                            org.geoserver.catalog.LayerInfo layer,
                                            AjaxRequestTarget target) {
                                        if (model.getObject().getTypeNames() == null) {
                                            model.getObject().setTypeNames(new ArrayList<>());
                                        }
                                        if (!model.getObject()
                                                .getTypeNames()
                                                .contains(layer.prefixedName())) {
                                            model.getObject()
                                                    .getTypeNames()
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

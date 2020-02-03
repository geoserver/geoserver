/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;

/** A panel that lets the user select a layer and copy its metadata to the current layer. */
public abstract class CopyFromLayerPanel extends Panel {
    private static final long serialVersionUID = 1297739738862860160L;

    private String resourceId;

    public CopyFromLayerPanel(String id, String resourceId) {
        super(id);
        this.resourceId = resourceId;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        GeoServerDialog dialog = new GeoServerDialog("copyFromLayerDialog");
        dialog.setInitialHeight(120);
        add(dialog);

        DropDownChoice<String> dropDown = createDropDown();
        dropDown.setNullValid(true);
        add(dropDown);

        add(
                new FeedbackPanel("copyFeedback", new ContainerFeedbackMessageFilter(this))
                        .setOutputMarkupId(true));

        add(createCopyAction(dropDown, dialog));
    }

    private DropDownChoice<String> createDropDown() {
        Catalog catalog =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(GeoServer.class)
                        .getCatalog();
        SortedSet<String> layers = new TreeSet<>();
        for (ResourceInfo res : catalog.getResources(ResourceInfo.class)) {
            if (!res.getId().equals(resourceId)) {
                layers.add(res.prefixedName());
            }
        }
        return new DropDownChoice<>("layer", new Model<String>(""), new ArrayList<>(layers));
    }

    private AjaxSubmitLink createCopyAction(
            final DropDownChoice<String> dropDown, GeoServerDialog dialog) {
        return new AjaxSubmitLink("link") {
            private static final long serialVersionUID = -8718015688839770852L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                ResourceInfo res;
                if (dropDown.getModelObject() == null || "".equals(dropDown.getModelObject())) {
                    error(
                            new ParamResourceModel("errorSelectLayer", CopyFromLayerPanel.this)
                                    .getString());
                    target.add(getFeedbackPanel());
                    return;
                } else {
                    Catalog catalog =
                            GeoServerApplication.get()
                                    .getApplicationContext()
                                    .getBean(GeoServer.class)
                                    .getCatalog();
                    res = catalog.getResourceByName(dropDown.getModelObject(), ResourceInfo.class);
                    Serializable map = res.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
                    if (map == null) {
                        error(
                                new ParamResourceModel(
                                                "errorNoMetadataInLayer", CopyFromLayerPanel.this)
                                        .getString());
                        target.add(getFeedbackPanel());
                        return;
                    }
                }

                dialog.setTitle(
                        new ParamResourceModel("confirmCopyDialog.title", CopyFromLayerPanel.this));
                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {

                            private static final long serialVersionUID = -5552087037163833563L;

                            @Override
                            protected Component getContents(String id) {
                                ParamResourceModel resource =
                                        new ParamResourceModel(
                                                "confirmCopyDialog.content",
                                                CopyFromLayerPanel.this);
                                return new MultiLineLabel(id, resource.getString());
                            }

                            @Override
                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                handleCopy(res, target);
                                return true;
                            }
                        });
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("copyFeedback");
    }

    public abstract void handleCopy(ResourceInfo res, AjaxRequestTarget target);
}

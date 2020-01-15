/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel;

import java.util.ArrayList;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.metadata.data.dto.GeonetworkConfiguration;
import org.geoserver.metadata.data.dto.MetadataConfiguration;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * A panel that lets the user select a geonetwork endpoint and input a uuid of the metadata record
 * in geonetwork.
 *
 * <p>The available geonetwork endpoints are configured in the yaml.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public abstract class ImportGeonetworkPanel extends Panel {
    private static final long serialVersionUID = 1297739738862860160L;

    public ImportGeonetworkPanel(String id) {
        super(id);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        GeoServerDialog dialog = new GeoServerDialog("importDialog");
        dialog.setInitialHeight(100);
        add(dialog);
        add(
                new FeedbackPanel("importFeedback", new ContainerFeedbackMessageFilter(this))
                        .setOutputMarkupId(true));

        DropDownChoice<String> dropDown = createDropDown();
        dropDown.setNullValid(true);
        add(dropDown);

        TextField<String> inputUUID = new TextField<>("textfield", new Model<String>(""));
        add(inputUUID);

        add(createImportAction(dropDown, inputUUID, dialog));
    }

    private AjaxSubmitLink createImportAction(
            final DropDownChoice<String> dropDown,
            final TextField<String> inputUUID,
            GeoServerDialog dialog) {
        return new AjaxSubmitLink("link") {
            private static final long serialVersionUID = -8718015688839770852L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                boolean valid = true;
                if (dropDown.getModelObject() == null || "".equals(dropDown.getModelObject())) {
                    error(
                            new ParamResourceModel(
                                            "errorSelectGeonetwork", ImportGeonetworkPanel.this)
                                    .getString());
                    valid = false;
                }
                final String uuId = inputUUID.getValue();
                if ("".equals(uuId)) {
                    error(
                            new ParamResourceModel("errorUuidRequired", ImportGeonetworkPanel.this)
                                    .getString());
                    valid = false;
                }
                if (valid) {
                    dialog.setTitle(
                            new ParamResourceModel(
                                    "confirmImportDialog.title", ImportGeonetworkPanel.this));
                    dialog.showOkCancel(
                            target,
                            new GeoServerDialog.DialogDelegate() {

                                private static final long serialVersionUID = -5552087037163833563L;

                                @Override
                                protected Component getContents(String id) {
                                    ParamResourceModel resource =
                                            new ParamResourceModel(
                                                    "confirmImportDialog.content",
                                                    ImportGeonetworkPanel.this);
                                    return new MultiLineLabel(id, resource.getString());
                                }

                                @Override
                                protected boolean onSubmit(
                                        AjaxRequestTarget target, Component contents) {
                                    handleImport(
                                            dropDown.getModelObject(),
                                            inputUUID.getModelObject(),
                                            target,
                                            getFeedbackPanel());
                                    return true;
                                }
                            });
                }

                target.add(getFeedbackPanel());
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            }
        };
    }

    public abstract void handleImport(
            String geoNetwork, String uuid, AjaxRequestTarget target, FeedbackPanel feedbackPanel);

    private DropDownChoice<String> createDropDown() {
        ConfigurationService configService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class);
        MetadataConfiguration configuration = configService.getMetadataConfiguration();
        ArrayList<String> optionsGeonetwork = new ArrayList<>();
        if (configuration != null && configuration.getGeonetworks() != null) {
            for (GeonetworkConfiguration geonetwork : configuration.getGeonetworks()) {
                optionsGeonetwork.add(geonetwork.getName());
            }
        }
        return new DropDownChoice<>("geonetworkName", new Model<String>(""), optionsGeonetwork);
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("importFeedback");
    }
}

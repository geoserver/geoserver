/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.metadata.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.model.impl.GlobalModel;
import org.geoserver.metadata.data.model.impl.MetadataTemplateImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.metadata.web.panel.LinkedLayersPanel;
import org.geoserver.metadata.web.panel.MetadataPanel;
import org.geoserver.metadata.web.panel.ProgressPanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

/**
 * The template page, view or edit the values in the template.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 * @author Niels Charlier
 */
public class MetadataTemplatePage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger(MetadataTemplatePage.class);

    private static final long serialVersionUID = 2273966783474224452L;

    private final IModel<List<MetadataTemplate>> templates;

    private final IModel<MetadataTemplate> metadataTemplateModel;

    private ProgressPanel progressPanel;

    private GeoServerDialog dialog;

    public MetadataTemplatePage(IModel<List<MetadataTemplate>> templates) {
        this(templates, new Model<>(newTemplate()));
    }

    private static MetadataTemplate newTemplate() {
        MetadataTemplateImpl template = new MetadataTemplateImpl();
        template.setId(UUID.randomUUID().toString());
        return template;
    }

    public MetadataTemplatePage(
            IModel<List<MetadataTemplate>> templates,
            IModel<MetadataTemplate> metadataTemplateModel) {
        this.templates = templates;
        this.metadataTemplateModel = metadataTemplateModel;
    }

    public void onInitialize() {
        super.onInitialize();

        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialHeight(100);
        ((ModalWindow) dialog.get("dialog")).showUnloadConfirmation(false);

        IModel<ComplexMetadataMap> metadataModel =
                new Model<ComplexMetadataMap>(
                        new ComplexMetadataMapImpl(
                                metadataTemplateModel.getObject().getMetadata()));
        ComplexMetadataService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ComplexMetadataService.class);
        service.clean(metadataModel.getObject());

        add(
                progressPanel =
                        new ProgressPanel(
                                "progress",
                                new ResourceModel("MetadataTemplatesPage.updatingMetadata")));

        Form<?> form = new Form<Object>("form");

        AjaxSubmitLink saveButton = createSaveButton();
        saveButton.setOutputMarkupId(true);
        form.add(saveButton);
        form.add(createCancelButton());

        TextField<String> nameField = createNameField(form, saveButton);
        form.add(nameField);

        TextField<String> desicription =
                new TextField<String>(
                        "description",
                        new PropertyModel<String>(metadataTemplateModel, "description"));
        form.add(desicription);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(
                new AbstractTab(new ResourceModel("editMetadata")) {
                    private static final long serialVersionUID = 4375160438369461475L;

                    public Panel getPanel(String panelId) {
                        return new MetadataPanel(panelId, metadataModel, null, null);
                    }
                });
        tabs.add(
                new AbstractTab(new ResourceModel("linkedLayers")) {
                    private static final long serialVersionUID = 871647379377450152L;

                    public Panel getPanel(String panelId) {
                        return new LinkedLayersPanel(panelId, metadataTemplateModel);
                    }
                });
        form.add(new TabbedPanel<ITab>("metadataTabs", tabs));

        this.add(form);
    }

    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    private TextField<String> createNameField(final Form<?> form, final AjaxSubmitLink saveButton) {
        return new TextField<String>(
                "name", new PropertyModel<String>(metadataTemplateModel, "name")) {
            private static final long serialVersionUID = -3736209422699508894L;

            @Override
            public boolean isRequired() {
                return form.findSubmittingButton() == saveButton;
            }
        };
    }

    private AjaxSubmitLink createSaveButton() {
        return new AjaxSubmitLink("save") {
            private static final long serialVersionUID = 8749672113664556346L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                if (metadataTemplateModel.getObject().getLinkedLayers().size() > 0) {
                    dialog.showOkCancel(
                            target,
                            new GeoServerDialog.DialogDelegate() {

                                private boolean ok = false;

                                private static final long serialVersionUID = 6769706050075583226L;

                                @Override
                                protected Component getContents(String id) {
                                    return new Label(
                                            id,
                                            new ParamResourceModel(
                                                    "saveWarning",
                                                    MetadataTemplatePage.this,
                                                    metadataTemplateModel
                                                            .getObject()
                                                            .getLinkedLayers()
                                                            .size()));
                                }

                                @Override
                                public void onClose(AjaxRequestTarget target) {
                                    if (ok) {
                                        save(form, target);
                                    }
                                }

                                @Override
                                protected boolean onSubmit(
                                        AjaxRequestTarget target, Component contents) {
                                    ok = true;
                                    return true;
                                }
                            });
                } else {
                    save(form, target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                addFeedbackPanels(target);
            }
        };
    }

    private AjaxLink<Object> createCancelButton() {
        return new AjaxLink<Object>("cancel") {
            private static final long serialVersionUID = -6892944747517089296L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                doReturn();
            }
        };
    }

    private void save(Form<?> form, AjaxRequestTarget target) {
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        try {
            boolean isOld = templates.getObject().contains(metadataTemplateModel.getObject());

            if (isOld) {
                // before saving,
                // update linked layers with latest version, in case it was elsewhere changed
                metadataTemplateModel.getObject().getLinkedLayers().clear();
                metadataTemplateModel
                        .getObject()
                        .getLinkedLayers()
                        .addAll(
                                service.getById(metadataTemplateModel.getObject().getId())
                                        .getLinkedLayers());
            }
            // save
            service.save(metadataTemplateModel.getObject());
            if (isOld) {
                templates
                        .getObject()
                        .set(
                                templates.getObject().indexOf(metadataTemplateModel.getObject()),
                                metadataTemplateModel.getObject());
            } else {
                templates.getObject().add(metadataTemplateModel.getObject());
            }

            if (isOld) {
                GlobalModel<Float> progressModel = new GlobalModel<Float>(0.0f);

                Executors.newSingleThreadExecutor()
                        .execute(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        service.update(
                                                metadataTemplateModel.getObject(),
                                                progressModel.getKey());
                                    }
                                });

                progressPanel.start(
                        target,
                        progressModel,
                        new ProgressPanel.EventHandler() {
                            private static final long serialVersionUID = 8967087707332457974L;

                            @Override
                            public void onFinished(AjaxRequestTarget target) {
                                doReturn();
                                progressModel.cleanUp();
                            }

                            @Override
                            public void onCanceled(AjaxRequestTarget target) {
                                doReturn();
                                progressModel.cleanUp();
                            }
                        });
            } else {
                doReturn();
            }
        } catch (IOException | IllegalArgumentException e) {
            if (e instanceof IOException) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String message =
                    rootCause == null ? e.getLocalizedMessage() : rootCause.getLocalizedMessage();
            if (message != null) {
                form.error(message);
            }
            addFeedbackPanels(target);
        }
    }
}

/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.metadata.web;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.GlobalModel;
import org.geoserver.metadata.data.model.impl.MetadataTemplateImpl;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.metadata.web.panel.ProgressPanel;
import org.geoserver.metadata.web.panel.TemplatesPositionPanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/**
 * Manages the metadata templates. Shows all existing templates,allows to create, edit and delete
 * templates.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 * @author Niels Charlier
 */
public class MetadataTemplatesPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 2273966783474224452L;

    private GeoServerTablePanel<MetadataTemplate> templatesPanel;

    private IModel<List<MetadataTemplate>> templates;

    private MetadataTemplateTracker tracker = new MetadataTemplateTracker();

    private ProgressPanel progressPanel;

    public MetadataTemplatesPage() {
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        templates = new ListModel<MetadataTemplate>(service.list());
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        GeoServerDialog dialog;
        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialHeight(150);
        ((ModalWindow) dialog.get("dialog")).showUnloadConfirmation(false);

        add(
                progressPanel =
                        new ProgressPanel(
                                "progress",
                                new ResourceModel("MetadataTemplatesPage.updatingMetadata")));

        add(
                new AjaxLink<Object>("addNew") {
                    private static final long serialVersionUID = 3581476968062788921L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(
                                new MetadataTemplatePage(templates)
                                        .setReturnPage(MetadataTemplatesPage.this));
                    }
                });

        // the removal button
        AjaxLink<Object> remove =
                new AjaxLink<Object>("removeSelected") {
                    private static final long serialVersionUID = 3581476968062788921L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        boolean noDelete = false;
                        for (MetadataTemplate template : templatesPanel.getSelection()) {
                            if (!template.getLinkedLayers().isEmpty()) {
                                StringBuilder layers = generateLayerNames(template);
                                StringResourceModel msg =
                                        new StringResourceModel("errorIsLinked", templatesPanel)
                                                .setParameters(template.getName(), layers);
                                error(msg.getString());
                                noDelete = true;
                            }
                        }

                        if (noDelete) {
                            addFeedbackPanels(target);
                        } else {
                            dialog.showOkCancel(
                                    target,
                                    new GeoServerDialog.DialogDelegate() {

                                        private static final long serialVersionUID =
                                                -5552087037163833563L;

                                        @Override
                                        protected Component getContents(String id) {
                                            ParamResourceModel resource =
                                                    new ParamResourceModel(
                                                            "deleteDialog.content",
                                                            MetadataTemplatesPage.this);
                                            StringBuffer sb = new StringBuffer();
                                            sb.append(resource.getString());
                                            for (MetadataTemplate template :
                                                    templatesPanel.getSelection()) {
                                                sb.append("\n&nbsp;&nbsp;");
                                                sb.append(escapeHtml(template.getName()));
                                            }
                                            return new MultiLineLabel(id, sb.toString())
                                                    .setEscapeModelStrings(false);
                                        }

                                        @Override
                                        protected boolean onSubmit(
                                                AjaxRequestTarget target, Component contents) {
                                            templates
                                                    .getObject()
                                                    .removeAll(templatesPanel.getSelection());
                                            tracker.removeTemplates(templatesPanel.getSelection());
                                            target.add(templatesPanel);
                                            return true;
                                        }
                                    });
                        }
                    }
                };
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);
        add(remove);

        // the copy button
        AjaxLink<Object> copy =
                new AjaxLink<Object>("copySelected") {
                    private static final long serialVersionUID = 3581476968062788921L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        MetadataTemplate clone = templatesPanel.getSelection().get(0).clone();
                        clone.getLinkedLayers().clear();
                        ((MetadataTemplateImpl) clone).setId(UUID.randomUUID().toString());
                        clone.setName(null);
                        setResponsePage(
                                new MetadataTemplatePage(templates, new Model<>(clone))
                                        .setReturnPage(MetadataTemplatesPage.this));
                    }
                };
        copy.setOutputMarkupId(true);
        copy.setEnabled(false);
        add(copy);

        // the panel
        templatesPanel =
                new GeoServerTablePanel<MetadataTemplate>(
                        "templatesPanel", new MetadataTemplateDataProvider(templates), true) {

                    private static final long serialVersionUID = -8943273843044917552L;

                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        remove.setEnabled(templatesPanel.getSelection().size() > 0);
                        copy.setEnabled(templatesPanel.getSelection().size() == 1);
                        target.add(remove);
                        target.add(copy);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<MetadataTemplate> itemModel,
                            GeoServerDataProvider.Property<MetadataTemplate> property) {
                        if (property.equals(MetadataTemplateDataProvider.NAME)) {
                            return new SimpleAjaxLink<String>(
                                    id, (IModel<String>) property.getModel(itemModel)) {
                                private static final long serialVersionUID = -9184383036056499856L;

                                @Override
                                protected void onClick(AjaxRequestTarget target) {
                                    ((MarkupContainer)
                                                    templatesPanel
                                                            .get("listContainer")
                                                            .get("items"))
                                            .removeAll();
                                    IModel<MetadataTemplate> model =
                                            new Model<>(itemModel.getObject().clone());
                                    setResponsePage(
                                            new MetadataTemplatePage(templates, model)
                                                    .setReturnPage(MetadataTemplatesPage.this));
                                }
                            };
                        } else if (property.equals(MetadataTemplateDataProvider.PRIORITY)) {
                            return new TemplatesPositionPanel(
                                    id, templates, tracker, itemModel, this);
                        }
                        return null;
                    }
                };
        templatesPanel.setOutputMarkupId(true);
        add(templatesPanel);

        add(
                new AjaxLink<Object>("save") {
                    private static final long serialVersionUID = 6152685206300932774L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        if (tracker.getAffectedResources().isEmpty()) {
                            save(target);
                            doReturn();
                        } else {

                            dialog.showOkCancel(
                                    target,
                                    new GeoServerDialog.DialogDelegate() {

                                        private static final long serialVersionUID =
                                                6769706050075583226L;

                                        private boolean ok = false;

                                        @Override
                                        protected Component getContents(String id) {
                                            return new Label(
                                                    id,
                                                    new ParamResourceModel(
                                                            "saveWarning",
                                                            MetadataTemplatesPage.this,
                                                            tracker.getAffectedResources().size()));
                                        }

                                        @Override
                                        public void onClose(AjaxRequestTarget target) {

                                            if (ok) {

                                                save(target);

                                                MetadataTemplateService service =
                                                        GeoServerApplication.get()
                                                                .getApplicationContext()
                                                                .getBean(
                                                                        MetadataTemplateService
                                                                                .class);

                                                GlobalModel<Float> progressModel =
                                                        new GlobalModel<Float>(0.0f);

                                                Collection<String> affectedResources =
                                                        tracker.getAffectedResources();

                                                Executors.newSingleThreadExecutor()
                                                        .execute(
                                                                new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        service.update(
                                                                                affectedResources,
                                                                                progressModel
                                                                                        .getKey());
                                                                    }
                                                                });

                                                progressPanel.start(
                                                        target,
                                                        progressModel,
                                                        new ProgressPanel.EventHandler() {
                                                            private static final long
                                                                    serialVersionUID =
                                                                            8967087707332457974L;

                                                            @Override
                                                            public void onFinished(
                                                                    AjaxRequestTarget target) {
                                                                doReturn();
                                                                progressModel.cleanUp();
                                                            }

                                                            @Override
                                                            public void onCanceled(
                                                                    AjaxRequestTarget target) {
                                                                doReturn();
                                                                progressModel.cleanUp();
                                                            }
                                                        });
                                            }
                                        }

                                        @Override
                                        protected boolean onSubmit(
                                                AjaxRequestTarget target, Component contents) {
                                            ok = true;
                                            return true;
                                        }
                                    });
                        }
                    }
                });
        add(
                new AjaxLink<Object>("cancel") {
                    private static final long serialVersionUID = -2023310159199302483L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doReturn();
                    }
                });
    }

    private StringBuilder generateLayerNames(MetadataTemplate template) {
        StringBuilder layers = new StringBuilder();
        for (String resourceId : template.getLinkedLayers()) {
            if (layers.length() > 0) {
                layers.append(",\n");
            }
            Catalog rawCatalog = (Catalog) GeoServerApplication.get().getBean("rawCatalog");
            ResourceInfo resource = rawCatalog.getResource(resourceId, ResourceInfo.class);
            if (resource != null) {
                layers.append(resource.prefixedName());
            } else {
                layers.append(resourceId);
            }
        }
        return layers;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    private void save(AjaxRequestTarget target) {
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        try {
            service.saveList(templates.getObject());
        } catch (IOException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String message =
                    rootCause == null ? e.getLocalizedMessage() : rootCause.getLocalizedMessage();
            if (message != null) {
                error(message);
            }
            addFeedbackPanels(target);
        }
    }
}

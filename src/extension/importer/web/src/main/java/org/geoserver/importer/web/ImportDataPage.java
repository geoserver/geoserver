/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.Importer;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.job.Task;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StoreChoiceRenderer;
import org.geoserver.web.data.store.StoreModel;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspaceDetachableModel;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.util.logging.Logging;

/**
 * First page of the import wizard.
 *
 * @author Andrea Aime - OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
// TODO WICKET8 - Verify this page works OK
@SuppressWarnings("serial")
public class ImportDataPage extends GeoServerSecuredPage {

    static Logger LOGGER = Logging.getLogger(ImportDataPage.class);

    AjaxRadioPanel<Source> sourceList;
    WebMarkupContainer sourcePanel;

    WorkspaceDetachableModel workspace;
    DropDownChoice<WorkspaceInfo> workspaceChoice;
    TextField<String> workspaceNameTextField;
    Component statusLabel;

    StoreModel<StoreInfo> store;
    DropDownChoice<StoreInfo> storeChoice;

    String storeName;

    ImportContextTable importTable;
    AjaxLink<?> removeImportLink;

    GeoServerDialog dialog;

    public ImportDataPage(PageParameters params) {
        Form<Object> form = new Form<>("form");
        add(form);

        sourceList = new AjaxRadioPanel<>("sources", Arrays.asList(Source.values()), Source.SPATIAL_FILES) {
            @Override
            protected void onRadioSelect(AjaxRequestTarget target, Source newSelection) {
                updateSourcePanel(newSelection, target);
            }

            @Override
            protected AjaxRadio<Source> newRadioCell(RadioGroup<Source> group, ListItem<Source> item) {
                AjaxRadio<Source> radio = super.newRadioCell(group, item);
                if (!item.getModelObject().isAvailable()) {
                    radio.setEnabled(false);
                }
                return radio;
            }

            @Override
            protected Component createLabel(String id, ListItem<Source> item) {
                return new SourceLabelPanel(id, item.getModelObject());
            }
        };

        form.add(sourceList);

        sourcePanel = new WebMarkupContainer("panel");
        sourcePanel.setOutputMarkupId(true);
        form.add(sourcePanel);

        Catalog catalog = GeoServerApplication.get().getCatalog();

        // workspace chooser
        workspace = new WorkspaceDetachableModel(catalog.getDefaultWorkspace());
        workspaceChoice =
                new DropDownChoice<>("workspace", workspace, new WorkspacesModel(), new WorkspaceChoiceRenderer());
        workspaceChoice.setOutputMarkupId(true);
        workspaceChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateTargetStore(target);
            }
        });
        workspaceChoice.setNullValid(true);
        form.add(workspaceChoice);

        WebMarkupContainer workspaceNameContainer = new WebMarkupContainer("workspaceNameContainer");
        workspaceNameContainer.setOutputMarkupId(true);
        form.add(workspaceNameContainer);

        workspaceNameTextField = new TextField<>("workspaceName", new Model<>());
        workspaceNameTextField.setOutputMarkupId(true);
        boolean defaultWorkspace = catalog.getDefaultWorkspace() != null;
        workspaceNameTextField.setVisible(!defaultWorkspace);
        workspaceNameTextField.setRequired(!defaultWorkspace);
        workspaceNameContainer.add(workspaceNameTextField);

        // store chooser
        WorkspaceInfo ws = workspace.getObject();
        store = new StoreModel<>(ws != null ? catalog.getDefaultDataStore(ws) : null);
        storeChoice =
                new DropDownChoice<>("store", store, new EnabledStoresModel(workspace), new StoreChoiceRenderer()) {
                    @Override
                    protected String getNullValidKey() {
                        return ImportDataPage.class.getSimpleName() + "." + super.getNullValidKey();
                    }
                };
        storeChoice.setOutputMarkupId(true);

        storeChoice.setNullValid(true);
        form.add(storeChoice);

        form.add(statusLabel = new Label("status", new Model<>()).setOutputMarkupId(true));
        form.add(new SubmitLink(form));
        form.add(new CancelLink().setOutputMarkupId(true).setEnabled(false));

        importTable =
                new ImportContextTable(
                        "imports",
                        new ImportContextProvider(true) {
                            @Override
                            protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<ImportContext>>
                                    getProperties() {
                                return Arrays.asList(ID, STATE, UPDATED);
                            }
                        },
                        true) {
                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        removeImportLink.setEnabled(!getSelection().isEmpty());
                        target.add(removeImportLink);
                    }
                };
        importTable.setOutputMarkupId(true);
        importTable.setFilterable(false);
        importTable.setSortable(false);
        form.add(importTable);

        form.add(
                removeImportLink = new AjaxLink("remove") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Importer importer = ImporterWebUtils.importer();
                        for (ImportContext c : importTable.getSelection()) {
                            try {
                                importer.delete(c);
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "Error deleting context", c);
                            }
                        }
                        importTable.clearSelection();
                        target.add(importTable);
                    }
                });
        removeImportLink.setOutputMarkupId(true).setEnabled(false);

        AjaxLink jobLink = new AjaxLink("jobs") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.showOkCancel(target, new DialogDelegate() {
                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        return true;
                    }

                    @Override
                    protected Component getContents(String id) {
                        return new JobQueuePanel(id);
                    }
                });
            }
        };
        jobLink.setVisible(ImporterWebUtils.isDevMode());
        form.add(jobLink);

        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialWidth(600);
        dialog.setInitialHeight(400);

        updateSourcePanel(Source.SPATIAL_FILES, null);
        updateTargetStore(null);
    }

    Long createContext() throws Exception {
        ImportSourcePanel panel = (ImportSourcePanel) sourcePanel.get("content");
        ImportData source = panel.createImportSource();

        WorkspaceInfo targetWorkspace = workspace.getObject();
        if (targetWorkspace == null) {
            Catalog cat = getCatalog();

            String wsName = workspaceNameTextField.getDefaultModelObjectAsString();
            targetWorkspace = cat.getWorkspaceByName(wsName);
            if (targetWorkspace == null) {
                targetWorkspace = cat.getFactory().createWorkspace();
                targetWorkspace.setName(wsName);

                NamespaceInfo ns = cat.getFactory().createNamespace();
                ns.setPrefix(wsName);
                try {
                    ns.setURI("http://opengeo.org/#" + URLEncoder.encode(wsName, "ASCII"));
                } catch (UnsupportedEncodingException e) {
                    error(e);
                }

                cat.add(targetWorkspace);
                cat.add(ns);
            }
        }

        StoreInfo targetStore = store.getObject();

        Importer importer = ImporterWebUtils.importer();
        return importer.createContextAsync(source, targetWorkspace, targetStore);
    }

    void updateTargetStore(AjaxRequestTarget target) {
        WorkspaceInfo ws = workspace.getObject();
        store.setObject(ws != null ? GeoServerApplication.get().getCatalog().getDefaultDataStore(ws) : null);

        workspaceNameTextField.setVisible(ws == null);
        workspaceNameTextField.setRequired(ws == null);

        if (target != null) {
            target.add(storeChoice);
            target.add(workspaceNameTextField.getParent());
        }
    }

    void updateSourcePanel(Source source, AjaxRequestTarget target) {
        if (sourcePanel.size() > 0) {
            sourcePanel.remove("content");
        }

        Panel p = source.createPanel("content");
        sourcePanel.add(p);

        if (target != null) {
            target.add(sourcePanel);
        }
    }

    void resetButtons(Form form, AjaxRequestTarget target) {
        form.get("next").setEnabled(true);
        form.get("cancel").setEnabled(false);
        statusLabel.setDefaultModelObject("");
        statusLabel.add(AttributeModifier.replace("class", ""));

        target.add(form.get("next"));
        target.add(form.get("cancel"));
        target.add(form.get("status"));
    }

    class SourceLabelPanel extends Panel {

        public SourceLabelPanel(String id, Source source) {
            super(id);

            add(new Label("name", source.getName(ImportDataPage.this)));
            add(new Label("description", source.getDescription(ImportDataPage.this)));

            Image icon = new Image("icon", source.getIcon());
            icon.add(new AttributeModifier("alt", source.getDescription(ImportDataPage.this)));
            add(icon);

            WebMarkupContainer extra = new WebMarkupContainer("extra");
            add(extra);
            extra.add(new ExternalLink("link", source.getHelpLink(ImportDataPage.this)));

            if (!source.isAvailable()) {
                get("name").add(AttributeModifier.replace("class", "italic"));
                add(AttributeModifier.replace(
                        "title", "Data source not available. Please " + "install required plugin and drivers."));
            } else {
                extra.setVisible(false);
            }
        }
    }

    /** A type data source. */
    enum Source {
        SPATIAL_FILES(DataIcon.FOLDER) {
            @Override
            ImportSourcePanel createPanel(String panelId) {
                return new SpatialFilePanel(panelId);
            }
        },
        MOSAIC(DataIcon.RASTER) {
            @Override
            ImportSourcePanel createPanel(String panelId) {
                return new MosaicPanel(panelId);
            }
        },
        POSTGIS(DataIcon.POSTGIS) {
            @Override
            ImportSourcePanel createPanel(String panelId) {
                return new PostGISPanel(panelId);
            }
        },
        ORACLE(DataIcon.DATABASE) {
            @Override
            ImportSourcePanel createPanel(String panelId) {
                return new OraclePanel(panelId);
            }

            @Override
            boolean isAvailable() {
                return isDataStoreFactoryAvaiable("org.geotools.data.oracle.OracleNGDataStoreFactory");
            }
        },
        SQLSERVER(DataIcon.DATABASE) {
            @Override
            ImportSourcePanel createPanel(String panelId) {
                return new SQLServerPanel(panelId);
            }

            @Override
            boolean isAvailable() {
                return isDataStoreFactoryAvaiable("org.geotools.data.sqlserver.SQLServerDataStoreFactory");
            }
        };

        DataIcon icon;

        Source(DataIcon icon) {
            this.icon = icon;
        }

        IModel<String> getName(Component component) {
            return new ParamResourceModel(this.name().toLowerCase() + "_name", component);
        }

        IModel<String> getDescription(Component component) {
            return new ParamResourceModel(this.name().toLowerCase() + "_description", component);
        }

        IModel<String> getHelpLink(Component component) {
            return new ParamResourceModel(this.name().toLowerCase() + "_helpLink", component);
        }

        PackageResourceReference getIcon() {
            return icon.getIcon();
        }

        boolean isAvailable() {
            return true;
        }

        boolean isDataStoreFactoryAvaiable(String className) {
            Class<DataStoreFactorySpi> clazz = getDataStoreFactorySpi(className);
            if (clazz == null) {
                return false;
            }

            DataStoreFactorySpi factory = null;
            try {
                factory = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Error creating DataStore factory: " + className, e);
                }
            }

            if (factory == null) {
                return false;
            }

            return factory.isAvailable();
        }

        @SuppressWarnings("unchecked")
        private Class<DataStoreFactorySpi> getDataStoreFactorySpi(String className) {
            try {
                return (Class<DataStoreFactorySpi>) Class.forName(className);
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "DataStore class not available: " + className, e);
                }
            }
            return null;
        }

        abstract ImportSourcePanel createPanel(String panelId);
    }

    private class SubmitLink extends AjaxSubmitLink {
        public SubmitLink(Form form) {
            super("next", form);
        }

        @Override
        protected void disableLink(ComponentTag tag) {
            super.disableLink(tag);
            tag.setName("a");
            tag.addBehavior(AttributeModifier.replace("class", "disabled"));
        }

        @Override
        protected void onError(AjaxRequestTarget target) {
            addFeedbackPanels(target);
        }

        @Override
        protected void onSubmit(AjaxRequestTarget target) {

            // update status to indicate we are working
            statusLabel.add(AttributeModifier.replace("class", "working-link"));
            statusLabel.setDefaultModelObject("Working");
            target.add(statusLabel);

            // enable cancel and disable this
            Component cancel = getForm().get("cancel");
            cancel.setEnabled(true);
            target.add(cancel);

            setEnabled(false);
            target.add(this);

            final AjaxSubmitLink self = this;

            final Long jobid;
            try {
                jobid = createContext();
            } catch (Exception e) {
                error(e);
                LOGGER.log(Level.WARNING, "Error creating import", e);
                resetButtons(getForm(), target);
                return;
            }

            cancel.setDefaultModelObject(jobid);
            this.add(new AbstractAjaxTimerBehavior(Duration.ofSeconds(3)) {
                @Override
                protected void onTimer(AjaxRequestTarget target) {
                    Importer importer = ImporterWebUtils.importer();
                    Task<ImportContext> t = importer.getTask(jobid);

                    if (t.isDone()) {
                        try {
                            if (t.getError() != null) {
                                error(t.getError());
                            } else if (!t.isCancelled()) {
                                ImportContext imp = t.get();

                                // check the import for actual things to do
                                boolean proceed = !imp.getTasks().isEmpty();

                                if (proceed) {
                                    imp.setArchive(false);
                                    importer.changed(imp);

                                    PageParameters pp = new PageParameters();
                                    pp.add("id", imp.getId());

                                    setResponsePage(ImportPage.class, pp);
                                } else {
                                    info("No data to import was found");
                                    importer.delete(imp);
                                }
                            }
                        } catch (Exception e) {
                            error(e);
                            LOGGER.log(Level.WARNING, "", e);
                        } finally {
                            stop(null);

                            // update the button back to original state
                            resetButtons(getForm(), target);

                            addFeedbackPanels(target);
                        }
                        return;
                    }

                    ProgressMonitor m = t.getMonitor();
                    String msg = m.getTask() != null ? m.getTask().toString() : "Working";

                    statusLabel.setDefaultModelObject(msg);
                    target.add(statusLabel);
                }

                @Override
                public boolean canCallListener(Component component) {
                    if (self.equals(component)) {
                        return true;
                    }
                    return super.canCallListener(component);
                }
            });
        }
    }

    private static class CancelLink extends AjaxLink<Long> {
        public CancelLink() {
            super("cancel", new Model<>());
        }

        @Override
        protected void disableLink(ComponentTag tag) {
            super.disableLink(tag);
            ImporterWebUtils.disableLink(tag);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            Importer importer = ImporterWebUtils.importer();
            Long jobid = getModelObject();
            Task<ImportContext> task = importer.getTask(jobid);
            if (task != null && !task.isDone() && !task.isCancelled()) {
                task.getMonitor().setCanceled(true);
                task.cancel(false);
                try {
                    task.get();
                } catch (Exception e) {
                }
            }

            setEnabled(false);

            Component next = getParent().get("next");
            next.setEnabled(true);

            target.add(this);
            target.add(next);
        }
    }
}

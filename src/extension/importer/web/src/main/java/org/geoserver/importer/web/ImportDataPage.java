/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.SharedResources;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.SimpleAttributeModifier;
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
import org.apache.wicket.util.time.Duration;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
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
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.util.logging.Logging;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.Importer;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.job.Task;

/**
 * First page of the import wizard.
 * 
 * @author Andrea Aime - OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class ImportDataPage extends GeoServerSecuredPage {
    
    static Logger LOGGER = Logging.getLogger(ImportDataPage.class);

    AjaxRadioPanel<Source> sourceList;
    WebMarkupContainer sourcePanel;
    
    WorkspaceDetachableModel workspace;
    DropDownChoice workspaceChoice;
    TextField workspaceNameTextField;
    Component statusLabel;

    StoreModel store;
    DropDownChoice storeChoice;
    
    String storeName;
    
    ImportContextTable importTable;
    AjaxLink removeImportLink;

    GeoServerDialog dialog;

    public ImportDataPage(PageParameters params) {
        Form form = new Form("form");
        add(form);

        sourceList = new AjaxRadioPanel<Source>("sources", Arrays.asList(Source.values()), Source.SPATIAL_FILES) {
            @Override
            protected void onRadioSelect(AjaxRequestTarget target, Source newSelection) {
                updateSourcePanel(newSelection, target);
            }

            @Override
            protected AjaxRadio<Source> newRadioCell(RadioGroup<Source> group,
                    ListItem<Source> item) {
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
        workspaceChoice = new DropDownChoice("workspace", workspace, new WorkspacesModel(), 
            new WorkspaceChoiceRenderer());
        workspaceChoice.setOutputMarkupId(true);
        workspaceChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
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

        workspaceNameTextField = new TextField("workspaceName", new Model());
        workspaceNameTextField.setOutputMarkupId(true);
        boolean defaultWorkspace = catalog.getDefaultWorkspace() != null;
        workspaceNameTextField.setVisible(!defaultWorkspace);
        workspaceNameTextField.setRequired(!defaultWorkspace);
        workspaceNameContainer.add(workspaceNameTextField);

        //store chooser
        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();
        store = new StoreModel(ws != null ? catalog.getDefaultDataStore(ws) : null);
        storeChoice = new DropDownChoice("store", store, new EnabledStoresModel(workspace),
            new StoreChoiceRenderer()) {
            protected String getNullValidKey() {
                return ImportDataPage.class.getSimpleName() + "." + super.getNullValidKey();
            };
        };
        storeChoice.setOutputMarkupId(true);

        storeChoice.setNullValid(true);
        form.add(storeChoice);

        form.add(statusLabel = new Label("status", new Model()).setOutputMarkupId(true));
        form.add(new AjaxSubmitLink("next", form) {
            @Override
            protected void disableLink(ComponentTag tag) {
                super.disableLink(tag);
                tag.setName("a");
                tag.addBehavior(new SimpleAttributeModifier("class", "disabled"));
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedbackPanel);
            }
            
            protected void onSubmit(AjaxRequestTarget target, final Form<?> form) {
                
                //update status to indicate we are working
                statusLabel.add(new SimpleAttributeModifier("class", "working-link"));
                statusLabel.setDefaultModelObject("Working");
                target.addComponent(statusLabel);
                
                //enable cancel and disable this
                Component cancel = form.get("cancel");
                cancel.setEnabled(true);
                target.addComponent(cancel);

                setEnabled(false);
                target.addComponent(this);
                
                final AjaxSubmitLink self = this;

                final Long jobid;
                try {
                    jobid = createContext();
                } catch (Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, "Error creating import", e);
                    resetButtons(form, target);
                    return;
                }

                cancel.setDefaultModelObject(jobid);
                this.add(new AbstractAjaxTimerBehavior(Duration.seconds(3)) {
                   protected void onTimer(AjaxRequestTarget target) {
                       Importer importer = ImporterWebUtils.importer();
                       Task<ImportContext> t = importer.getTask(jobid);

                       if (t.isDone()) {
                           try {
                               if (t.getError() != null) {
                                   error(t.getError());
                               }
                               else if (t.isCancelled()) {
                                   //do nothing
                               }
                               else {
                                   ImportContext imp = t.get();
                                   
                                   //check the import for actual things to do
                                   boolean proceed = !imp.getTasks().isEmpty();
                                  
                                   if (proceed) {
                                       imp.setArchive(false);
                                       importer.changed(imp);
           
                                       PageParameters pp = new PageParameters();
                                       pp.put("id", imp.getId());
           
                                       setResponsePage(ImportPage.class, pp);
                                   }
                                   else {
                                       info("No data to import was found");
                                       importer.delete(imp);
                                   }
                               }
                           }
                           catch(Exception e) {
                               error(e);
                               LOGGER.log(Level.WARNING, "", e);
                           }
                           finally {
                               stop();
                               
                               //update the button back to original state
                               resetButtons(form, target);

                               target.addComponent(feedbackPanel);
                           }
                           return;
                       }

                       ProgressMonitor m = t.getMonitor();
                       String msg = m.getTask() != null ? m.getTask().toString() : "Working";

                       statusLabel.setDefaultModelObject(msg);
                       target.addComponent(statusLabel);
                   }; 
                });
            }
        });
        
        form.add(new AjaxLink<Long>("cancel", new Model<Long>()) {
            protected void disableLink(ComponentTag tag) {
                super.disableLink(tag);
                ImporterWebUtils.disableLink(tag); 
            };
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
                    }
                    catch(Exception e) {
                    }
                }

                setEnabled(false);
                
                Component next = getParent().get("next");
                next.setEnabled(true);
                
                target.addComponent(this);
                target.addComponent(next);
            }
        }.setOutputMarkupId(true).setEnabled(false));

        importTable = new ImportContextTable("imports", new ImportContextProvider(true) {
            @Override
            protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<ImportContext>> getProperties() {
                return Arrays.asList(ID, STATE, UPDATED);
            }
        }, true) {
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removeImportLink.setEnabled(!getSelection().isEmpty());
                target.addComponent(removeImportLink);
            };
        };
        importTable.setOutputMarkupId(true);
        importTable.setFilterable(false);
        importTable.setSortable(false);
        form.add(importTable);

        form.add(removeImportLink = new AjaxLink("remove") {
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
                target.addComponent(importTable);
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
        dialog.setMinimalHeight(150);

        updateSourcePanel(Source.SPATIAL_FILES, null);
        updateTargetStore(null);
    }

    Long createContext() throws Exception {
        ImportSourcePanel panel = (ImportSourcePanel) sourcePanel.get("content");
        ImportData source = panel.createImportSource();

        WorkspaceInfo targetWorkspace = (WorkspaceInfo) workspace.getObject(); 
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
                    throw new RuntimeException(e);
                }

                cat.add( targetWorkspace );
                cat.add( ns );
            }
        }

        StoreInfo targetStore = (StoreInfo) (store.getObject() != null ? store
                .getObject() : null);

        Importer importer = ImporterWebUtils.importer();
        return importer.createContextAsync(source, targetWorkspace, targetStore);

    }
    void updateTargetStore(AjaxRequestTarget target) {
        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();
        store.setObject(ws != null ? 
            GeoServerApplication.get().getCatalog().getDefaultDataStore(ws) : null);
        
        workspaceNameTextField.setVisible(ws == null);
        workspaceNameTextField.setRequired(ws == null);

        if (target != null) {
            target.addComponent(storeChoice);
            target.addComponent(workspaceNameTextField.getParent());
        }
    }

    void updateSourcePanel(Source source, AjaxRequestTarget target) {
        Panel old = (Panel) sourcePanel.get(0);
        if (old != null) {
            sourcePanel.remove(old);
        }

        Panel p = source.createPanel("content");
        sourcePanel.add(p);

        if (target != null) {
            target.addComponent(sourcePanel);
        }
    }

    void resetButtons(Form form, AjaxRequestTarget target) {
        form.get("next").setEnabled(true);
        form.get("cancel").setEnabled(false);
        statusLabel.setDefaultModelObject("");
        statusLabel.add(new SimpleAttributeModifier("class", ""));
        
        target.addComponent(form.get("next"));
        target.addComponent(form.get("cancel"));
        target.addComponent(form.get("status"));
    }

    class SourceLabelPanel extends Panel {

        public SourceLabelPanel(String id, Source source) {
            super(id);
            
            add(new Label("name", source.getName(ImportDataPage.this)));
            add(new Label("description", source .getDescription(ImportDataPage.this)));
            
            Image icon = new Image("icon", source.getIcon());
            icon.add(new AttributeModifier("alt", true, source.getDescription(ImportDataPage.this)));
            add(icon);

            WebMarkupContainer extra = new WebMarkupContainer("extra");
            add(extra);
            extra.add(new ExternalLink("link", source.getHelpLink(ImportDataPage.this)));
            
            if (!source.isAvailable()) {
                get("name").add(new SimpleAttributeModifier("style", "font-style: italic;"));
                add(new SimpleAttributeModifier("title", "Data source not available. Please " +
                      "install required plugin and drivers."));
            }
            else {
                extra.setVisible(false);
            }
        }
    }

    /**
     * A type data source.
     */
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
        
//        directory(new ResourceReference(GeoServerApplication.class, "img/icons/silk/folder.png"),
//                DirectoryPage.class, "org.geotools.data.shapefile.ShapefileDataStoreFactory"), // 
//        postgis(new ResourceReference(GeoServerApplication.class,
//                "img/icons/geosilk/database_vector.png"), PostGISPage.class,
//                "org.geotools.data.postgis.PostgisNGDataStoreFactory"), //
//        oracle(new ResourceReference(GeoServerApplication.class,
//                "img/icons/geosilk/database_vector.png"), OraclePage.class,
//                "org.geotools.data.oracle.OracleNGDataStoreFactory"), //
//        sqlserver(new ResourceReference(GeoServerApplication.class,
//                "img/icons/geosilk/database_vector.png"), SQLServerPage.class,
//                "org.geotools.data.sqlserver.SQLServerDataStoreFactory"), //
//        arcsde(new ResourceReference(GeoServerApplication.class,
//                "img/icons/geosilk/database_vector.png"), ArcSDEPage.class,
//                "org.geotools.arcsde.ArcSDEDataStoreFactory");

        DataIcon icon;

        Source(DataIcon icon) {
            this.icon = icon;
        }

        IModel getName(Component component) {
            return new ParamResourceModel(this.name().toLowerCase() + "_name", component);
        }

        IModel getDescription(Component component) {
            return new ParamResourceModel(this.name().toLowerCase() + "_description", component);
        }

        IModel getHelpLink(Component component) {
            return new ParamResourceModel(this.name().toLowerCase() + "_helpLink", component);
        }

        ResourceReference getIcon() {
            return icon.getIcon();
        }

        boolean isAvailable() {
            return true;
        }

        boolean isDataStoreFactoryAvaiable(String className) {
            Class<DataStoreFactorySpi> clazz = null;
            try {
                clazz = (Class<DataStoreFactorySpi>) Class.forName(className);
            }
            catch(Exception e) {
                if(LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "DataStore class not available: " + className, e);
                }
            }
            if (clazz == null) {
                return false;
            }

            DataStoreFactorySpi factory = null;
            try {
                factory = clazz.newInstance();
            }
            catch(Exception e) {
                if(LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Error creating DataStore factory: " + className, e);
                }
            }

            if (factory == null) {
                return false;
            }

            return factory.isAvailable();
        }

        abstract ImportSourcePanel createPanel(String panelId);
    }
}

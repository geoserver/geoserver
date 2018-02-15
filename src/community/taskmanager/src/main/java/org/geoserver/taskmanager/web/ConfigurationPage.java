/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.util.ValidationError;
import org.geoserver.taskmanager.web.model.AttributesModel;
import org.geoserver.taskmanager.web.model.TasksModel;
import org.geoserver.taskmanager.web.panel.BatchesPanel;
import org.geoserver.taskmanager.web.panel.DropDownPanel;
import org.geoserver.taskmanager.web.panel.NewTaskPanel;
import org.geoserver.taskmanager.web.panel.SimpleAjaxSubmitLink;
import org.geoserver.taskmanager.web.panel.TaskParameterPanel;
import org.geoserver.taskmanager.web.panel.TextFieldPanel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.UnauthorizedPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geotools.util.logging.Logging;

public class ConfigurationPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger(ConfigurationPage.class);
    
    private static final long serialVersionUID = 3902645494421966388L;
        
    private IModel<Configuration> configurationModel;
    
    private Map<String, Batch> oldBatches;

    private Map<String, Task> oldTasks;
        
    private List<Task> removedTasks = new ArrayList<Task>();
    
    private GeoServerDialog dialog;
    
    private AjaxSubmitLink remove;

    private AttributesModel attributesModel;
    
    private GeoServerTablePanel<Attribute> attributesPanel;
    
    private GeoServerTablePanel<Task> tasksPanel;
    
    private Map<String, List<String>> domains;

    private BatchesPanel batchesPanel;
    
    public ConfigurationPage(IModel<Configuration> configurationModel) {
        if (configurationModel.getObject().getId() != null
                && !TaskManagerBeans.get().getSecUtil().isReadable(getSession().getAuthentication(), 
                configurationModel.getObject())) {
             throw new RestartResponseException(UnauthorizedPage.class); 
        } 
        this.configurationModel = configurationModel;
        oldTasks = new HashMap<>(configurationModel.getObject().getTasks());
        oldBatches = new HashMap<>(configurationModel.getObject().getBatches());
        if (configurationModel.getObject().isTemplate()) {
            setReturnPage(TemplatesPage.class);
        } else {
            setReturnPage(ConfigurationsPage.class);
        }
    }
    
    @Override
    public void onInitialize() {
        super.onInitialize();
        
        add(dialog = new GeoServerDialog("dialog"));
                
        Form<Configuration> form = new Form<Configuration>("configurationForm", configurationModel);
        add(form);
  
        AjaxSubmitLink saveButton = saveOrApplyButton("save", true);  
        form.add(saveButton);
        AjaxSubmitLink applyButton = saveOrApplyButton("apply", false);  
        form.add(applyButton);
                
        form.add(new TextField<String>("name", new PropertyModel<String>(configurationModel, "name")) {
            private static final long serialVersionUID = -3736209422699508894L;

            @Override
            public boolean isRequired() {
                return form.findSubmittingButton() == saveButton 
                        || form.findSubmittingButton() == applyButton;
            }
        });
        
        List<String> workspaces = new ArrayList<String>();
        for (WorkspaceInfo wi : GeoServerApplication.get().getCatalog().getWorkspaces()) {
            workspaces.add(wi.getName());
        }
                
        form.add(new DropDownChoice<String>("workspace", 
                new PropertyModel<String>(configurationModel, "workspace"), workspaces)
                .setNullValid(true));
        
        TextField<String> name = new TextField<String>("description", 
                new PropertyModel<String>(configurationModel, "description"));
        form.add(name);
                
        //the attributes panel
        attributesModel = new AttributesModel(configurationModel);
        form.add(attributesPanel = attributesPanel());
        attributesPanel.setFilterVisible(false);
        attributesPanel.setSelectable(false);
        attributesPanel.setPageable(false);
        attributesPanel.setSortable(false);
        attributesPanel.setOutputMarkupId(true);
        
        form.add(addButton());
        
        // the removal button
        form.add(remove = removeButton());
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);
        
        //the tasks panel
        form.add(tasksPanel = tasksPanel());
        tasksPanel.setFilterVisible(false);
        tasksPanel.setPageable(false);
        tasksPanel.setSortable(false);
        tasksPanel.setOutputMarkupId(true);
        
        //the batches panel
        form.add(batchesPanel = new BatchesPanel("batchesPanel", configurationModel));
        batchesPanel.setOutputMarkupId(true);
        
        form.add(new AjaxLink<Object>("cancel") {
            private static final long serialVersionUID = -6892944747517089296L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                //restore tasks
                configurationModel.getObject().getTasks().clear();
                configurationModel.getObject().getTasks().putAll(oldTasks);
                //restore batches
                configurationModel.getObject().getBatches().clear();
                configurationModel.getObject().getBatches().putAll(oldBatches);
                doReturn();
            }            
        });
        

        if (configurationModel.getObject().getId() != null
                && !TaskManagerBeans.get().getSecUtil().isWritable(
                getSession().getAuthentication(), configurationModel.getObject())) {
            form.get("name").setEnabled(false);
            form.get("workspace").setEnabled(false);
            form.get("description").setEnabled(false);
            attributesPanel.setEnabled(false);
            form.get("addNew").setEnabled(false);
            form.get("removeSelected").setEnabled(false);
            tasksPanel.setEnabled(false);
            batchesPanel.get("addNew").setEnabled(false);
            batchesPanel.get("removeSelected").setEnabled(false);
            saveButton.setEnabled(false);
            applyButton.setEnabled(false);
        }
        
    }
    
    protected String getTitle() {
        return new ParamResourceModel(configurationModel.getObject().isTemplate() ? 
                "temp.title" : "title", this).getString();
    }

    protected String getDescription() {
        return new ParamResourceModel(configurationModel.getObject().isTemplate() ? 
                "temp.description" : "description", this).getString();
    }
    
    protected AjaxSubmitLink addButton() {
        return new AjaxSubmitLink("addNew") {

            private static final long serialVersionUID = 7320342263365531859L;
            
            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                dialog.setTitle(new ParamResourceModel("newTaskDialog.title", getPage()));
                dialog.setInitialWidth(600);
                dialog.setInitialHeight(200);
                dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                    private static final long serialVersionUID = 7410393012930249966L;
                    
                    private NewTaskPanel panel;

                    @Override
                    protected Component getContents(String id) {
                        return panel = new NewTaskPanel(id);
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target,
                            Component contents) {
                        if (configurationModel.getObject().getTasks().get(
                                panel.getNameField().getModelObject()) != null) {
                            error(new ParamResourceModel("duplicateTaskName", getPage())
                                    .getString());
                            target.add(panel.getFeedbackPanel());
                            return false;
                        } else {
                            Task task = TaskManagerBeans.get().getTaskUtil().initTask(
                                    panel.getTypeField().getModelObject(), 
                                    panel.getNameField().getModelObject());
                            TaskManagerBeans.get().getDataUtil().addTaskToConfiguration(
                                    configurationModel.getObject(), task);
                            
                            attributesModel.save(false);
                            TaskManagerBeans.get().getTaskUtil().updateDomains(
                                    configurationModel.getObject(), domains);    
                            
                            //bit of a hack - updates the selected array inside the panel
                            //with the new count
                            tasksPanel.setPageable(false);  
    
                            target.add(tasksPanel);
                            target.add(attributesPanel);
                            return true;
                        }
                    }
                    
                    @Override
                    public void onError(AjaxRequestTarget target, Form<?> form) {
                        target.add(panel.getFeedbackPanel());
                    }

                });
            }
            
        };
    }
    
    protected AjaxSubmitLink removeButton() {
        return new AjaxSubmitLink("removeSelected") {
            private static final long serialVersionUID = 3581476968062788921L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                dialog.setTitle(new ParamResourceModel("confirmDeleteDialog.title", getPage()));
                dialog.setInitialWidth(600);
                dialog.setInitialHeight(150);
                dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                    private static final long serialVersionUID = -5552087037163833563L;
                    
                    @Override
                    protected Component getContents(String id) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(new ParamResourceModel("confirmDeleteDialog.content",
                                getPage()).getString());
                        for (Task task : tasksPanel.getSelection()) {
                            sb.append("\n&nbsp;&nbsp;");
                            sb.append(escapeHtml(task.getName()));
                        }
                        return new MultiLineLabel(id, sb.toString())
                                .setEscapeModelStrings(false);
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        for (Task task : tasksPanel.getSelection()) {
                            BatchElement element = taskInUse(task);
                            if (element == null) {
                                configurationModel.getObject().getTasks().remove(task.getName());
                                if (task.getId() != null) {
                                    removedTasks.add(task);                                
                                }
                            } else {
                                error(new ParamResourceModel("taskInUse", getPage(), task.getName(),
                                        element.getBatch().getFullName()).getString());
                            }
                        }
                        tasksPanel.clearSelection();
                        attributesModel.save(false);
                        TaskManagerBeans.get().getTaskUtil().updateDomains(
                                configurationModel.getObject(), domains);      
                        remove.setEnabled(false);                        
                        target.add(tasksPanel);
                        target.add(attributesPanel);
                        target.add(remove);
                        target.add(getFeedbackPanel());
                        return true;
                    }
                });
                
            }  
        };
    }
    
    private BatchElement taskInUse(Task task) {
        if (task.getId() != null) {
            task = TaskManagerBeans.get().getDataUtil().init(task);
            for (BatchElement element : task.getBatchElements()) {
                if (element.getBatch().isActive()) {
                    return element;
                }
            }
        } else {
            for (Batch batch : configurationModel.getObject().getBatches().values()) {
                for (BatchElement element : batch.getElements()) {
                    if (element.getTask().equals(task)) {
                        return element;
                    }
                }
            }
        }
        return null;
    }
    
    protected GeoServerTablePanel<Task> tasksPanel() {
        return new GeoServerTablePanel<Task>("tasksPanel", 
                new TasksModel(configurationModel), true) {

            private static final long serialVersionUID = -8943273843044917552L;
            
            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                remove.setEnabled(tasksPanel.getSelection().size() > 0);
                target.add(remove);
            }

            @Override
            protected Component getComponentForProperty(String id, IModel<Task> itemModel,
                    Property<Task> property) {
                if (property.equals(TasksModel.PARAMETERS)) {
                    final GeoServerTablePanel<Task> thisPanel = this;
                    return new SimpleAjaxSubmitLink(id, new IModel<String>() {
                        private static final long serialVersionUID = 519359570729184717L;
                        @Override public void detach() {}
                        @Override public void setObject(String object) {}
                        @Override
                        public String getObject() {
                            StringBuilder sb = new StringBuilder();
                            for (Parameter pam : itemModel.getObject().getParameters().values()) {
                                if (pam.getValue() != null) {
                                    sb.append(pam.getName()).append(" = ").append(pam.getValue())
                                            .append(", ");
                                }
                            }
                            if (sb.length() > 2) {
                                sb.setLength(sb.length() - 2);
                                return sb.toString();
                            } else {
                                return new ParamResourceModel("specifyParameters", getPage()).getString();
                            }
                        }
                    }) {
                        private static final long serialVersionUID = 3950104089264630053L;

                        @Override
                        public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                            dialog.setInitialWidth(800);
                            dialog.setInitialHeight(400);                            
                            dialog.setTitle(new Model<String>(itemModel.getObject().getFullName() + 
                                    " - " + itemModel.getObject().getType()));
                            dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                                private static final long serialVersionUID = 7410393012930249966L;

                                @Override
                                protected Component getContents(String id) {
                                    return new TaskParameterPanel(id, itemModel);
                                }

                                @Override
                                protected boolean onSubmit(AjaxRequestTarget target,
                                        Component contents) {
                                    attributesModel.save(false);
                                    TaskManagerBeans.get().getTaskUtil().updateDomains(
                                            configurationModel.getObject(), domains);                                    
                                    target.add(thisPanel);
                                    target.add(attributesPanel);
                                    return true;
                                }

                            });
                        }

                    };
                }                
                return null;
            }
        };
    }
    
    protected GeoServerTablePanel<Attribute> attributesPanel() {    
        attributesModel.save(false);
        domains = TaskManagerBeans.get().getTaskUtil().getDomains(configurationModel.getObject());
        return new GeoServerTablePanel<Attribute>("attributesPanel", attributesModel, true) {
    
                private static final long serialVersionUID = -8943273843044917552L;
            
                @SuppressWarnings("unchecked")
                @Override
                protected Component getComponentForProperty(String id, IModel<Attribute> itemModel,
                        Property<Attribute> property) {
                    if (property.equals(AttributesModel.VALUE)) {
                        List<String> domain = domains.get(itemModel.getObject().getName());
                        if (domain == null) {
                            return new TextFieldPanel(id, (IModel<String>) property.getModel(itemModel));
                        } else {
                            final DropDownPanel ddp = new DropDownPanel(id, 
                                    (IModel<String>) property.getModel(itemModel),
                                    new PropertyModel<List<String>>(domains, itemModel.getObject().getName()));
                            
                            final GeoServerTablePanel<Attribute> tablePanel = this;
                            
                            ddp.getDropDownChoice().add(new AjaxFormSubmitBehavior("change") {
                                
                                private static final long serialVersionUID = -7698014209707408962L;
                    
                                @Override
                                protected void onSubmit(AjaxRequestTarget target) {
                                    attributesModel.save(false);
                                    TaskManagerBeans.get().getTaskUtil().updateDependentDomains(
                                            itemModel.getObject(), domains);
                                    
                                    target.add(tablePanel);
                                }
            
                            });
                            
                            return ddp;
                        }
                    } else if (property.equals(AttributesModel.ACTIONS)) {
                        
                    }
                    return null;
                }
            };
    }
    
    protected AjaxSubmitLink saveOrApplyButton(final String id, final boolean doReturn) {
        return new AjaxSubmitLink(id) {
            private static final long serialVersionUID = 3735176778941168701L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                attributesModel.save(true);
                List<ValidationError> errors = TaskManagerBeans.get().getTaskUtil().validate(
                        configurationModel.getObject());
                if (!errors.isEmpty()) {
                    for (ValidationError error : errors) {
                        //TODO: use localized resource based on error type instead of toString
                        form.error(error.toString());
                    }
                    target.add(feedbackPanel);
                    return;
                }

                try {
                    configurationModel.setObject(TaskManagerBeans.get().getDataUtil().saveAndRemove(
                            configurationModel.getObject(), removedTasks, batchesPanel.getRemovedBatches()));
                    removedTasks.clear();
                    batchesPanel.getRemovedBatches().clear();
                    if (doReturn) {
                        doReturn();
                    } else {
                        form.success(new ParamResourceModel("success", getPage()).getString());
                        target.add(batchesPanel);
                        ((MarkupContainer) batchesPanel.get("form:batchesPanel:listContainer:items")).removeAll();
                        target.add(feedbackPanel);
                    }
                } catch (Exception e) { 
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                    Throwable rootCause = ExceptionUtils.getRootCause(e);
                    form.error(rootCause == null ? e.getLocalizedMessage() : 
                        rootCause.getLocalizedMessage());
                    target.add(feedbackPanel);
                }
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedbackPanel);
            }
        };
    }
    
    

}

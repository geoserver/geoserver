/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.entity.ContentType;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.util.InitConfigUtil;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.util.ValidationError;
import org.geoserver.taskmanager.util.XStreamUtil;
import org.geoserver.taskmanager.web.action.Action;
import org.geoserver.taskmanager.web.model.AttributesModel;
import org.geoserver.taskmanager.web.model.TasksModel;
import org.geoserver.taskmanager.web.panel.BatchesPanel;
import org.geoserver.taskmanager.web.panel.ButtonPanel;
import org.geoserver.taskmanager.web.panel.DropDownPanel;
import org.geoserver.taskmanager.web.panel.MultiLabelCheckBoxPanel;
import org.geoserver.taskmanager.web.panel.NamePanel;
import org.geoserver.taskmanager.web.panel.NewTaskPanel;
import org.geoserver.taskmanager.web.panel.PanelListPanel;
import org.geoserver.taskmanager.web.panel.SimpleAjaxSubmitLink;
import org.geoserver.taskmanager.web.panel.TaskParameterPanel;
import org.geoserver.taskmanager.web.panel.TextAreaPanel;
import org.geoserver.taskmanager.web.panel.TextFieldPanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.UnauthorizedPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;
import org.hibernate.exception.ConstraintViolationException;

// TODO WICKET8 - Verify this page works OK
public class ConfigurationPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger(ConfigurationPage.class);

    @Serial
    private static final long serialVersionUID = 3902645494421966388L;

    private IModel<Configuration> originalConfigurationModel;

    private IModel<Configuration> configurationModel;

    private Map<String, Batch> oldBatches;

    private Map<String, Task> oldTasks;

    private List<Task> removedTasks = new ArrayList<Task>();

    private GeoServerDialog dialog;

    private AjaxSubmitLink remove;

    private ResourceLink<Object> export;

    private AttributesModel attributesModel;

    private GeoServerTablePanel<Attribute> attributesPanel;

    private GeoServerTablePanel<Task> tasksPanel;

    private Map<String, List<String>> domains;

    private BatchesPanel batchesPanel;

    private TasksModel tasksModel;

    private boolean initMode;

    public ConfigurationPage(IModel<Configuration> configurationModel) {
        if (configurationModel.getObject().getId() != null
                && !TaskManagerBeans.get()
                        .getSecUtil()
                        .isReadable(getSession().getAuthentication(), configurationModel.getObject())) {
            throw new RestartResponseException(UnauthorizedPage.class);
        }
        for (Task task : configurationModel.getObject().getTasks().values()) {
            TaskManagerBeans.get().getTaskUtil().fixTask(task);
        }
        initMode = TaskManagerBeans.get().getInitConfigUtil().isInitConfig(configurationModel.getObject());
        originalConfigurationModel = configurationModel;
        this.configurationModel = new Model<Configuration>(
                initMode
                        ? TaskManagerBeans.get().getInitConfigUtil().wrap(configurationModel.getObject())
                        : configurationModel.getObject());
        oldTasks = new HashMap<>(configurationModel.getObject().getTasks());
        oldBatches = new HashMap<>(configurationModel.getObject().getBatches());
        if (configurationModel.getObject().isTemplate()) {
            setReturnPage(TemplatesPage.class);
        } else {
            setReturnPage(ConfigurationsPage.class);
        }
    }

    public ConfigurationPage(Configuration configuration) {
        this(new Model<Configuration>(configuration));
    }

    public IModel<Configuration> getConfigurationModel() {
        return configurationModel;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        add(dialog = new GeoServerDialog("dialog"));

        add(new WebMarkupContainer("init").setVisible(initMode));

        add(new WebMarkupContainer("notvalidated")
                .setVisible(!initMode
                        && configurationModel.getObject().getId() != null
                        && !configurationModel.getObject().isTemplate()
                        && !configurationModel.getObject().isValidated()));

        Form<Configuration> form = new Form<Configuration>("configurationForm", configurationModel);
        add(form);

        AjaxSubmitLink saveButton = saveOrApplyButton("save", true);
        saveButton.setOutputMarkupId(true);
        form.add(saveButton);
        AjaxSubmitLink applyButton = saveOrApplyButton("apply", false);
        form.add(applyButton);

        form.add((export = exportButton())
                .setOutputMarkupId(true)
                .setOutputMarkupPlaceholderTag(true)
                .setVisible(!initMode && configurationModel.getObject().getId() != null));

        form.add(new TextField<String>("name", new PropertyModel<String>(configurationModel, "name")) {
            @Serial
            private static final long serialVersionUID = -3736209422699508894L;

            @Override
            public boolean isRequired() {
                return form.findSubmitter() == saveButton || form.findSubmitter() == applyButton;
            }
        });

        SortedSet<String> workspaces = new TreeSet<String>();
        for (WorkspaceInfo wi : GeoServerApplication.get().getCatalog().getWorkspaces()) {
            if (TaskManagerBeans.get().getSecUtil().isAdminable(getSession().getAuthentication(), wi)) {
                workspaces.add(wi.getName());
            }
        }
        if (configurationModel.getObject().getWorkspace() != null) {
            workspaces.add(configurationModel.getObject().getWorkspace());
        }
        boolean canBeNull = GeoServerApplication.get().getCatalog().getDefaultWorkspace() != null
                && TaskManagerBeans.get()
                        .getSecUtil()
                        .isAdminable(
                                getSession().getAuthentication(),
                                GeoServerApplication.get().getCatalog().getDefaultWorkspace());
        form.add(
                new DropDownChoice<String>(
                        "workspace",
                        new PropertyModel<String>(configurationModel, "workspace"),
                        new ArrayList<String>(workspaces)) {
                    @Serial
                    private static final long serialVersionUID = -6665795544099616226L;

                    @Override
                    public boolean isRequired() {
                        return !canBeNull
                                && (form.findSubmitter() == saveButton || form.findSubmitter() == applyButton);
                    }
                }.setNullValid(canBeNull));

        TextField<String> name =
                new TextField<String>("description", new PropertyModel<String>(configurationModel, "description"));
        form.add(name);

        // the attributes panel
        attributesModel = new AttributesModel(configurationModel);
        form.add(attributesPanel = attributesPanel());
        attributesPanel.setFilterVisible(false);
        attributesPanel.setSelectable(false);
        attributesPanel.setPageable(false);
        attributesPanel.setSortable(false);
        attributesPanel.setOutputMarkupId(true);

        form.add(addButton().setOutputMarkupId(true));

        // the removal button
        form.add(remove = removeButton());
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);

        // the tasks panel
        tasksModel = new TasksModel(configurationModel);
        form.add(tasksPanel = tasksPanel());
        tasksPanel.setFilterVisible(false);
        tasksPanel.setPageable(false);
        tasksPanel.setSortable(false);
        tasksPanel.setOutputMarkupId(true);

        // the batches panel
        form.add(batchesPanel = new BatchesPanel("batchesPanel", configurationModel));
        batchesPanel.setOutputMarkupId(true);

        form.add(new AjaxLink<Object>("cancel") {
            @Serial
            private static final long serialVersionUID = -6892944747517089296L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (!initMode) {
                    // restore tasks
                    configurationModel.getObject().getTasks().clear();
                    configurationModel.getObject().getTasks().putAll(oldTasks);
                    // restore batches
                    configurationModel.getObject().getBatches().clear();
                    configurationModel.getObject().getBatches().putAll(oldBatches);
                }
                doReturn();
            }
        });

        if (initMode) {
            form.get("addNew").setEnabled(false);
            form.get("removeSelected").setEnabled(false);
            batchesPanel.get("addNew").setEnabled(false);
            batchesPanel.get("removeSelected").setEnabled(false);
            saveButton.setVisible(false);
        }

        if (configurationModel.getObject().getId() != null
                && !TaskManagerBeans.get()
                        .getSecUtil()
                        .isAdminable(getSession().getAuthentication(), configurationModel.getObject())) {
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

    @Override
    protected String getTitle() {
        return new ParamResourceModel(configurationModel.getObject().isTemplate() ? "temp.title" : "title", this)
                .getString();
    }

    @Override
    protected String getDescription() {
        return new ParamResourceModel(
                        configurationModel.getObject().isTemplate() ? "temp.description" : "description", this)
                .getString();
    }

    protected AjaxSubmitLink addButton() {
        return new AjaxSubmitLink("addNew") {

            @Serial
            private static final long serialVersionUID = 7320342263365531859L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                dialog.setTitle(new ParamResourceModel("newTaskDialog.title", getPage()));
                dialog.setInitialWidth(600);
                dialog.setInitialHeight(225);
                dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                    @Serial
                    private static final long serialVersionUID = 7410393012930249966L;

                    private NewTaskPanel panel;

                    @Override
                    protected Component getContents(String id) {
                        return panel = new NewTaskPanel(id, configurationModel.getObject());
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        if (getExistingTask(panel.getNameField().getModelObject()) != null) {
                            error(new ParamResourceModel("duplicateTaskName", getPage()).getString());
                            target.add(panel.getFeedbackPanel());
                            return false;
                        } else {
                            Task task;
                            String copyTask = panel.getCopyField().getModel().getObject();
                            if (copyTask != null) {
                                task = TaskManagerBeans.get()
                                        .getTaskUtil()
                                        .copyTask(
                                                configurationModel
                                                        .getObject()
                                                        .getTasks()
                                                        .get(copyTask),
                                                panel.getNameField().getModelObject());
                            } else {
                                task = TaskManagerBeans.get()
                                        .getTaskUtil()
                                        .initTask(
                                                panel.getTypeField().getModelObject(),
                                                panel.getNameField().getModelObject());
                            }
                            TaskManagerBeans.get()
                                    .getDataUtil()
                                    .addTaskToConfiguration(configurationModel.getObject(), task);

                            attributesModel.save(false);
                            TaskManagerBeans.get()
                                    .getTaskUtil()
                                    .updateDomains(
                                            configurationModel.getObject(),
                                            domains,
                                            TaskManagerBeans.get().getDataUtil().getAssociatedAttributeNames(task));
                            ((MarkupContainer)
                                            attributesPanel.get("listContainer").get("items"))
                                    .removeAll();

                            // bit of a hack - updates the selected array inside the panel
                            // with the new count
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
            @Serial
            private static final long serialVersionUID = 3581476968062788921L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                dialog.setTitle(new ParamResourceModel("confirmDeleteDialog.title", getPage()));
                dialog.setInitialWidth(600);
                dialog.setInitialHeight(175);
                dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                    @Serial
                    private static final long serialVersionUID = -5552087037163833563L;

                    private IModel<Boolean> shouldCleanupModel = new Model<Boolean>(false);

                    @Override
                    protected Component getContents(String id) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(new ParamResourceModel("confirmDeleteDialog.content", getPage()).getString());
                        for (Task task : tasksPanel.getSelection()) {
                            sb.append("\n&nbsp;&nbsp;");
                            sb.append(StringEscapeUtils.escapeHtml4(task.getName()));
                        }
                        return new MultiLabelCheckBoxPanel(
                                id,
                                sb.toString(),
                                new ParamResourceModel("cleanUp", getPage()).getString(),
                                shouldCleanupModel,
                                !configurationModel.getObject().isTemplate());
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        Set<String> attNames = new HashSet<String>();
                        for (Task task : tasksPanel.getSelection()) {
                            BatchElement element =
                                    configurationModel.getObject().getId() == null
                                            ? null
                                            : TaskManagerBeans.get()
                                                    .getDataUtil()
                                                    .taskInUse(task, configurationModel.getObject());
                            if (element == null) {
                                if (shouldCleanupModel.getObject()) {
                                    // clean-up
                                    if (TaskManagerBeans.get().getTaskUtil().canCleanup(task)) {
                                        if (TaskManagerBeans.get().getTaskUtil().cleanup(task)) {
                                            info(new ParamResourceModel("cleanUp.success", getPage(), task.getName())
                                                    .getString());
                                        } else {
                                            error(new ParamResourceModel("cleanUp.failed", getPage(), task.getName())
                                                    .getString());
                                        }
                                    } else {
                                        info(new ParamResourceModel("cleanUp.ignore", getPage(), task.getName())
                                                .getString());
                                    }
                                }

                                // remember which attribute names to update
                                attNames.addAll(
                                        TaskManagerBeans.get().getDataUtil().getAssociatedAttributeNames(task));

                                // actually remove
                                configurationModel.getObject().getTasks().remove(task.getName());
                                if (task.getId() != null) {
                                    removedTasks.add(task);
                                }
                            } else {
                                error(new ParamResourceModel(
                                                "taskInUse",
                                                getPage(),
                                                task.getName(),
                                                element.getBatch().getFullName())
                                        .getString());
                            }
                        }
                        tasksPanel.clearSelection();
                        attributesModel.save(false);
                        TaskManagerBeans.get()
                                .getTaskUtil()
                                .updateDomains(configurationModel.getObject(), domains, attNames);
                        ((MarkupContainer) attributesPanel.get("listContainer").get("items")).removeAll();
                        remove.setEnabled(false);
                        target.add(tasksPanel);
                        target.add(attributesPanel);
                        target.add(remove);
                        ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                        return true;
                    }
                });
            }
        };
    }

    private ResourceLink<Object> exportButton() {
        return new ResourceLink<Object>("export", new AbstractResource() {
            @Serial
            private static final long serialVersionUID = 184195260939749675L;

            @Override
            protected ResourceResponse newResourceResponse(Attributes attributes) {
                // reload from database
                Configuration lastSaved = TaskManagerBeans.get()
                        .getDao()
                        .init(TaskManagerBeans.get()
                                .getDao()
                                .getConfiguration(configurationModel.getObject().getId()));
                ResourceResponse response = new ResourceResponse();
                response.setContentType(ContentType.APPLICATION_XML.getMimeType());
                response.setContentDisposition(ContentDisposition.ATTACHMENT);
                response.setFileName(lastSaved.getName() + ".xml");
                response.setWriteCallback(new WriteCallback() {

                    @Override
                    public void writeData(Attributes attributes) throws IOException {
                        OutputStream outputStream = attributes.getResponse().getOutputStream();
                        outputStream.write(XStreamUtil.xs().toXML(lastSaved).getBytes());
                    }
                });
                return response;
            }
        });
    }

    protected GeoServerTablePanel<Task> tasksPanel() {
        return new GeoServerTablePanel<Task>("tasksPanel", tasksModel, true) {

            @Serial
            private static final long serialVersionUID = -8943273843044917552L;

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                remove.setEnabled(tasksPanel.getSelection().size() > 0);
                target.add(remove);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected Component getComponentForProperty(String id, IModel<Task> itemModel, Property<Task> property) {
                final GeoServerTablePanel<Task> thisPanel = this;
                if (property.equals(TasksModel.NAME)) {
                    IModel<String> nameModel = (IModel<String>) property.getModel(itemModel);
                    return new SimpleAjaxSubmitLink(id, nameModel) {

                        @Serial
                        private static final long serialVersionUID = 2023797271780630795L;

                        @Override
                        protected void onSubmit(AjaxRequestTarget target) {

                            dialog.setInitialWidth(400);
                            dialog.setInitialHeight(100);
                            dialog.setTitle(new ParamResourceModel("changeTaskName", getPage()));
                            dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                                @Serial
                                private static final long serialVersionUID = 7410393012930249966L;

                                private NamePanel panel;

                                @Override
                                protected Component getContents(String id) {
                                    panel = new NamePanel(id, nameModel);
                                    panel.getTextField().add(new IValidator<String>() {
                                        @Serial
                                        private static final long serialVersionUID = 1L;

                                        @Override
                                        public void validate(IValidatable<String> validatable) {
                                            Task existing = getExistingTask(validatable.getValue());
                                            if (existing != null && !existing.equals(itemModel.getObject())) {
                                                validatable.error(new org.apache.wicket.validation.ValidationError(
                                                        new ParamResourceModel("duplicateTaskName", getPage())
                                                                .getString()));
                                            }
                                        }
                                    });
                                    return panel;
                                }

                                @Override
                                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                                    // rebuild map so that the key is changed by order
                                    // remains the same
                                    ArrayList<Task> tasks = new ArrayList<>(configurationModel
                                            .getObject()
                                            .getTasks()
                                            .values());
                                    configurationModel.getObject().getTasks().clear();
                                    for (Task task : tasks) {
                                        configurationModel
                                                .getObject()
                                                .getTasks()
                                                .put(task.getName(), task);
                                    }
                                    target.add(thisPanel);
                                    return true;
                                }

                                @Override
                                public void onError(final AjaxRequestTarget target, Form<?> form) {
                                    target.add(panel.getFeedbackPanel());
                                }
                            });
                        }
                    };
                } else if (property.equals(TasksModel.PARAMETERS)) {
                    return new SimpleAjaxSubmitLink(id, new IModel<String>() {
                        @Serial
                        private static final long serialVersionUID = 519359570729184717L;

                        @Override
                        public void detach() {}

                        @Override
                        public void setObject(String object) {}

                        @Override
                        public String getObject() {
                            StringBuilder sb = new StringBuilder();
                            for (Parameter pam :
                                    itemModel.getObject().getParameters().values()) {
                                if (pam.getValue() != null) {
                                    sb.append(pam.getName())
                                            .append(" = ")
                                            .append(pam.getValue())
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
                        @Serial
                        private static final long serialVersionUID = 3950104089264630053L;

                        @Override
                        public void onSubmit(AjaxRequestTarget target) {
                            Set<String> attributeNames = new HashSet<String>();
                            // add attributes before change
                            attributeNames.addAll(TaskManagerBeans.get()
                                    .getDataUtil()
                                    .getAssociatedAttributeNames(itemModel.getObject()));

                            dialog.setInitialWidth(800);
                            dialog.setInitialHeight(400);
                            dialog.setTitle(
                                    new Model<String>(itemModel.getObject().getFullName()
                                            + " - "
                                            + itemModel.getObject().getType()));
                            dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                                @Serial
                                private static final long serialVersionUID = 7410393012930249966L;

                                @Override
                                protected Component getContents(String id) {
                                    return new TaskParameterPanel(id, itemModel, attributesModel);
                                }

                                @Override
                                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                                    attributesModel.save(false);

                                    // add attributes after change
                                    attributeNames.addAll(TaskManagerBeans.get()
                                            .getDataUtil()
                                            .getAssociatedAttributeNames(itemModel.getObject()));
                                    TaskManagerBeans.get()
                                            .getTaskUtil()
                                            .updateDomains(configurationModel.getObject(), domains, attributeNames);
                                    ((MarkupContainer) attributesPanel
                                                    .get("listContainer")
                                                    .get("items"))
                                            .removeAll();

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

            @Serial
            private static final long serialVersionUID = -8943273843044917552L;

            @SuppressWarnings("unchecked")
            @Override
            protected Component getComponentForProperty(
                    String id, IModel<Attribute> itemModel, Property<Attribute> property) {

                final GeoServerTablePanel<Attribute> tablePanel = this;

                if (property.equals(AttributesModel.VALUE)) {
                    List<String> domain = domains.get(itemModel.getObject().getName());
                    if (domain == null) {
                        Set<ParameterType> typesForAttribute = TaskManagerBeans.get()
                                .getTaskUtil()
                                .getTypesForAttribute(itemModel.getObject(), configurationModel.getObject());
                        if (typesForAttribute.contains(ParameterType.SQL)) {
                            return new TextAreaPanel(id, (IModel<String>) property.getModel(itemModel));
                        } else {
                            return new TextFieldPanel(id, (IModel<String>) property.getModel(itemModel));
                        }
                    } else {
                        final DropDownPanel ddp = new DropDownPanel(
                                id,
                                (IModel<String>) property.getModel(itemModel),
                                new PropertyModel<List<String>>(
                                        domains, itemModel.getObject().getName()),
                                configurationModel.getObject().isTemplate()
                                        || !TaskManagerBeans.get()
                                                .getTaskUtil()
                                                .isAttributeRequired(
                                                        itemModel.getObject(), configurationModel.getObject()));
                        ddp.getDropDownChoice().add(new AjaxFormSubmitBehavior("change") {

                            @Serial
                            private static final long serialVersionUID = -7698014209707408962L;

                            @Override
                            protected void onSubmit(AjaxRequestTarget target) {
                                attributesModel.save(false);
                                TaskManagerBeans.get()
                                        .getTaskUtil()
                                        .updateDependentDomains(
                                                itemModel.getObject(), configurationModel.getObject(), domains);

                                target.add(tablePanel);
                            }
                        });

                        return ddp;
                    }
                } else if (property.equals(AttributesModel.ACTIONS)) {
                    List<String> actions = TaskManagerBeans.get()
                            .getTaskUtil()
                            .getActionsForAttribute(itemModel.getObject(), configurationModel.getObject());
                    if (!actions.isEmpty()) {
                        return new PanelListPanel<String>(id, actions) {
                            @Serial
                            private static final long serialVersionUID = -4770841274788269473L;

                            @Override
                            protected Panel populateItem(String id, IModel<String> actionModel) {
                                return new ButtonPanel(
                                        id, new StringResourceModel("Actions." + actionModel.getObject())) {
                                    @Serial
                                    private static final long serialVersionUID = -2791644626218648013L;

                                    @Override
                                    public void onSubmit(AjaxRequestTarget target) {
                                        Action action = TaskManagerBeans.get()
                                                .getActions()
                                                .get(actionModel.getObject());
                                        IModel<String> valueModel =
                                                new PropertyModel<>(itemModel, AttributesModel.VALUE.getName());
                                        List<String> dependentValues = TaskManagerBeans.get()
                                                .getTaskUtil()
                                                .getDependentRawValues(
                                                        action.getName(),
                                                        itemModel.getObject(),
                                                        configurationModel.getObject());
                                        if (action.accept(valueModel.getObject(), dependentValues)) {
                                            action.execute(ConfigurationPage.this, target, valueModel, dependentValues);
                                            target.add(tablePanel);
                                        } else {
                                            error(new ParamResourceModel("invalidValue", getPage()).getString());
                                            ConfigurationPage.this.addFeedbackPanels(target);
                                        }
                                    }
                                };
                            }
                        };
                    }
                }
                return null;
            }
        };
    }

    protected AjaxSubmitLink saveOrApplyButton(final String id, final boolean doReturn) {
        return new AjaxSubmitLink(id) {
            @Serial
            private static final long serialVersionUID = 3735176778941168701L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                attributesModel.save(true);
                List<ValidationError> errors =
                        TaskManagerBeans.get().getTaskUtil().validate(configurationModel.getObject());
                if (!errors.isEmpty()) {
                    for (ValidationError error : errors) {
                        // TODO: use localized resource based on error type instead of toString
                        getForm().error(error.toString());
                    }
                    addFeedbackPanels(target);
                    return;
                } else if (!configurationModel.getObject().isTemplate() && !initMode) {
                    configurationModel.getObject().setValidated(true);
                }

                try {
                    originalConfigurationModel.setObject(TaskManagerBeans.get()
                            .getDataUtil()
                            .saveScheduleAndRemove(
                                    InitConfigUtil.unwrap(configurationModel.getObject()),
                                    removedTasks,
                                    batchesPanel.getRemovedBatches()));
                    configurationModel.setObject(
                            initMode
                                    ? TaskManagerBeans.get()
                                            .getInitConfigUtil()
                                            .wrap(originalConfigurationModel.getObject())
                                    : originalConfigurationModel.getObject());
                    removedTasks.clear();
                    batchesPanel.getRemovedBatches().clear();
                    if (doReturn) {
                        doReturn();
                    } else {
                        oldTasks = new HashMap<>(configurationModel.getObject().getTasks());
                        oldBatches =
                                new HashMap<>(configurationModel.getObject().getBatches());
                        getForm().success(new ParamResourceModel("success", getPage()).getString());
                        export.setVisible(true);
                        target.add(export);
                        target.add(batchesPanel);
                        attributesModel.refresh();
                        target.add(attributesPanel);

                        ((MarkupContainer) batchesPanel.get("form:batchesPanel:listContainer:items")).removeAll();
                        addFeedbackPanels(target);
                        if (initMode) {
                            setResponsePage(new InitConfigurationPage(configurationModel));
                        }
                    }
                } catch (Exception e) {
                    if (e.getCause() instanceof ConstraintViolationException) {
                        getForm().error(new ParamResourceModel("duplicate", getPage()).getString());
                    } else {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                        Throwable rootCause = ExceptionUtils.getRootCause(e);
                        getForm().error(rootCause == null ? e.getLocalizedMessage() : rootCause.getLocalizedMessage());
                    }
                    addFeedbackPanels(target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                addFeedbackPanels(target);
            }
        };
    }

    private Task getExistingTask(String name) {
        Task existing = configurationModel.getObject().getTasks().get(name);
        if (existing == null) {
            // if we have deleted an old task with that name,
            // but not yet clicked apply we cannot use name either.
            existing = oldTasks.get(name);
        }
        return existing;
    }

    public GeoServerDialog getDialog() {
        return dialog;
    }

    public void addAttributesPanel(AjaxRequestTarget target) {
        target.add(attributesPanel);
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}

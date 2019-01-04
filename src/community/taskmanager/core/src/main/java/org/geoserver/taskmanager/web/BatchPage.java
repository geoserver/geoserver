/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.web.model.BatchElementsModel;
import org.geoserver.taskmanager.web.panel.DropDownPanel;
import org.geoserver.taskmanager.web.panel.FrequencyPanel;
import org.geoserver.taskmanager.web.panel.PositionPanel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.UnauthorizedPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;
import org.hibernate.exception.ConstraintViolationException;

public class BatchPage extends GeoServerSecuredPage {
    private static final long serialVersionUID = -5111795911981486778L;

    private static final Logger LOGGER = Logging.getLogger(BatchPage.class);

    private IModel<Batch> batchModel;

    private List<BatchElement> oldElements;

    private Set<BatchElement> removedElements = new HashSet<BatchElement>();

    private Set<Task> addedTasks = new HashSet<Task>();

    private GeoServerDialog dialog;

    private AjaxSubmitLink remove;

    private GeoServerTablePanel<BatchElement> elementsPanel;

    public BatchPage(IModel<Batch> batchModel, Page parentPage) {
        if (batchModel.getObject().getId() != null
                && !TaskManagerBeans.get()
                        .getSecUtil()
                        .isReadable(getSession().getAuthentication(), batchModel.getObject())) {
            throw new RestartResponseException(UnauthorizedPage.class);
        }
        this.batchModel = batchModel;
        oldElements = new ArrayList<>(batchModel.getObject().getElements());
        setReturnPage(parentPage);
    }

    public BatchPage(Batch batch, Page parentPage) {
        this(new Model<Batch>(batch), parentPage);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        add(dialog = new GeoServerDialog("dialog"));

        add(
                new WebMarkupContainer("notvalidated")
                        .setVisible(
                                batchModel.getObject().getConfiguration() != null
                                        && !batchModel.getObject().getConfiguration().isTemplate()
                                        && !batchModel
                                                .getObject()
                                                .getConfiguration()
                                                .isValidated()));

        Form<Batch> form = new Form<Batch>("batchForm", batchModel);
        add(form);

        AjaxSubmitLink saveButton = saveOrApplyButton("save", true);
        form.add(saveButton);
        AjaxSubmitLink applyButton = saveOrApplyButton("apply", false);
        form.add(applyButton);

        form.add(
                new TextField<String>("name", new PropertyModel<String>(batchModel, "name")) {
                    private static final long serialVersionUID = -3736209422699508894L;

                    @Override
                    public boolean isRequired() {
                        return form.findSubmittingButton() == saveButton
                                || form.findSubmittingButton() == applyButton;
                    }
                });

        SortedSet<String> workspaces = new TreeSet<String>();
        for (WorkspaceInfo wi : GeoServerApplication.get().getCatalog().getWorkspaces()) {
            if (wi.getName().equals(batchModel.getObject().getWorkspace())
                    || TaskManagerBeans.get()
                            .getSecUtil()
                            .isAdminable(getSession().getAuthentication(), wi)) {
                workspaces.add(wi.getName());
            }
        }
        boolean canBeNull =
                (GeoServerApplication.get().getCatalog().getDefaultWorkspace() != null
                        && TaskManagerBeans.get()
                                .getSecUtil()
                                .isAdminable(
                                        getSession().getAuthentication(),
                                        GeoServerApplication.get()
                                                .getCatalog()
                                                .getDefaultWorkspace()));
        form.add(
                new DropDownChoice<String>(
                        "workspace",
                        new PropertyModel<String>(batchModel, "workspace"),
                        new ArrayList<String>(workspaces)) {

                    private static final long serialVersionUID = -9058423608027219299L;

                    @Override
                    public boolean isRequired() {
                        return !canBeNull
                                && (form.findSubmittingButton() == saveButton
                                        || form.findSubmittingButton() == applyButton);
                    }
                }.setNullValid(canBeNull)
                        // theoretically a batch can have a separate workspace from config, but it
                        // is
                        // confusing to users so turning this off by default.
                        .setEnabled(
                                batchModel.getObject().getConfiguration() == null
                                        || batchModel.getObject().getWorkspace() != null));

        form.add(
                new TextField<String>(
                        "description", new PropertyModel<String>(batchModel, "description")));

        form.add(
                new TextField<String>(
                                "configuration",
                                new Model<String>(
                                        batchModel.getObject().getConfiguration() == null
                                                ? ""
                                                : batchModel
                                                        .getObject()
                                                        .getConfiguration()
                                                        .getName()))
                        .setEnabled(false));

        form.add(
                new FrequencyPanel(
                        "frequency", new PropertyModel<String>(batchModel, "frequency")));

        form.add(new CheckBox("enabled", new PropertyModel<Boolean>(batchModel, "enabled")));

        form.add(addButton());

        // the removal button
        form.add(remove = removeButton());
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);

        // the tasks panel
        form.add(elementsPanel = elementsPanel());
        elementsPanel.setFilterVisible(false);
        elementsPanel.setPageable(false);
        elementsPanel.setSortable(false);
        elementsPanel.setOutputMarkupId(true);

        form.add(
                new AjaxLink<Object>("cancel") {
                    private static final long serialVersionUID = -6892944747517089296L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // restore elements
                        batchModel.getObject().getElements().clear();
                        batchModel.getObject().getElements().addAll(oldElements);
                        doReturn();
                    }
                });

        if (batchModel.getObject().getId() != null
                && !TaskManagerBeans.get()
                        .getSecUtil()
                        .isAdminable(getSession().getAuthentication(), batchModel.getObject())) {
            form.get("name").setEnabled(false);
            form.get("workspace").setEnabled(false);
            form.get("description").setEnabled(false);
            form.get("configuration").setEnabled(false);
            form.get("frequency").setEnabled(false);
            form.get("enabled").setEnabled(false);
            form.get("addNew").setEnabled(false);
            remove.setEnabled(false);
            saveButton.setEnabled(false);
            elementsPanel.setEnabled(false);
        }
    }

    protected AjaxSubmitLink saveOrApplyButton(final String id, final boolean doReturn) {
        return new AjaxSubmitLink(id) {
            private static final long serialVersionUID = 3735176778941168701L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    Configuration config = batchModel.getObject().getConfiguration();
                    batchModel.setObject(
                            TaskManagerBeans.get()
                                    .getDataUtil()
                                    .saveScheduleAndRemove(
                                            batchModel.getObject(), removedElements));
                    // update the old config (still used on configuration page)
                    if (config != null) {
                        batchModel.getObject().setConfiguration(config);
                        config.getBatches()
                                .put(batchModel.getObject().getName(), batchModel.getObject());
                    }
                    if (doReturn) {
                        doReturn();
                    } else {
                        form.success(new ParamResourceModel("success", getPage()).getString());
                    }
                } catch (ConstraintViolationException e) {
                    form.error(new ParamResourceModel("duplicate", getPage()).getString());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                    Throwable rootCause = ExceptionUtils.getRootCause(e);
                    form.error(
                            rootCause == null
                                    ? e.getLocalizedMessage()
                                    : rootCause.getLocalizedMessage());
                }
                addFeedbackPanels(target);
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                addFeedbackPanels(target);
            }
        };
    }

    protected AjaxSubmitLink addButton() {
        return new AjaxSubmitLink("addNew") {

            private static final long serialVersionUID = 7320342263365531859L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                dialog.setTitle(new ParamResourceModel("newTaskDialog.title", getPage()));
                dialog.setInitialWidth(600);
                dialog.setInitialHeight(100);
                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {

                            private static final long serialVersionUID = 7410393012930249966L;

                            private DropDownPanel panel;
                            private Map<String, Task> tasks;

                            @Override
                            protected Component getContents(String id) {
                                tasks = new TreeMap<String, Task>();
                                for (Task task :
                                        TaskManagerBeans.get()
                                                .getDao()
                                                .getTasksAvailableForBatch(
                                                        batchModel.getObject())) {
                                    if (batchModel.getObject().getConfiguration() != null
                                            && !batchModel
                                                    .getObject()
                                                    .getConfiguration()
                                                    .getTasks()
                                                    .containsKey(task.getName())) {
                                        // deleted in config
                                        continue;
                                    }
                                    if (!addedTasks.contains(task)
                                            && TaskManagerBeans.get()
                                                    .getSecUtil()
                                                    .isWriteable(
                                                            BatchPage.this
                                                                    .getSession()
                                                                    .getAuthentication(),
                                                            task.getConfiguration())) {
                                        tasks.put(task.getFullName(), task);
                                    }
                                }
                                for (BatchElement be : removedElements) {
                                    tasks.put(be.getTask().getFullName(), be.getTask());
                                }
                                panel =
                                        new DropDownPanel(
                                                id,
                                                new Model<String>(),
                                                new Model<ArrayList<String>>(
                                                        new ArrayList<String>(tasks.keySet())),
                                                new ParamResourceModel(
                                                        "newTaskDialog.content", getPage()));
                                panel.getDropDownChoice().setNullValid(false).setRequired(true);
                                return panel;
                            }

                            @Override
                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                Task task = tasks.get(panel.getDropDownChoice().getModelObject());
                                BatchElement be =
                                        TaskManagerBeans.get()
                                                .getDataUtil()
                                                .addBatchElement(batchModel.getObject(), task);
                                if (!removedElements.remove(be)) {
                                    addedTasks.add(task);
                                }

                                // bit of a hack - updates the selected array inside the panel
                                // with the new count
                                elementsPanel.setPageable(false);

                                target.add(elementsPanel);
                                return true;
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
                dialog.setInitialHeight(100);
                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {

                            private static final long serialVersionUID = -5552087037163833563L;

                            @Override
                            protected Component getContents(String id) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(
                                        new ParamResourceModel(
                                                        "confirmDeleteDialog.content", getPage())
                                                .getString());
                                for (BatchElement be : elementsPanel.getSelection()) {
                                    sb.append("\n&nbsp;&nbsp;");
                                    sb.append(
                                            StringEscapeUtils.escapeHtml4(
                                                    be.getTask().getFullName()));
                                }
                                return new MultiLineLabel(id, sb.toString())
                                        .setEscapeModelStrings(false);
                            }

                            @Override
                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                batchModel
                                        .getObject()
                                        .getElements()
                                        .removeAll(elementsPanel.getSelection());
                                for (BatchElement be : elementsPanel.getSelection()) {
                                    if (!addedTasks.remove(be.getTask())) {
                                        removedElements.add(be);
                                    }
                                }
                                remove.setEnabled(false);
                                target.add(elementsPanel);
                                target.add(remove);
                                return true;
                            }
                        });
            }
        };
    }

    protected GeoServerTablePanel<BatchElement> elementsPanel() {
        return new GeoServerTablePanel<BatchElement>(
                "tasksPanel", new BatchElementsModel(batchModel), true) {

            private static final long serialVersionUID = -8943273843044917552L;

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                remove.setEnabled(elementsPanel.getSelection().size() > 0);
                target.add(remove);
            }

            @Override
            protected Component getComponentForProperty(
                    String id, IModel<BatchElement> itemModel, Property<BatchElement> property) {
                if (property.equals(BatchElementsModel.INDEX)) {
                    return new PositionPanel(id, itemModel, this);
                }
                return null;
            }
        };
    }
}

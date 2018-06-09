/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
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
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;
import org.geoserver.backuprestore.AbstractExecutionAdapter;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.GeoServerUnlockablePage;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspaceDetachableModel;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.factory.Hints;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;

/**
 * First page of the backup wizard.
 *
 * @author Andrea Aime - OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class BackupRestoreDataPage extends GeoServerSecuredPage implements GeoServerUnlockablePage {

    static Logger LOGGER = Logging.getLogger(BackupRestoreDataPage.class);

    WorkspaceDetachableModel workspace;
    DropDownChoice workspaceChoice;
    TextField workspaceNameTextField;

    Component statusLabel;

    BackupRestoreExecutionsTable backupRestoreExecutionsTable;
    BackupRestoreExecutionsTable restoreExecutionsTable;

    WebMarkupContainer newBackupRestorePanel;

    GeoServerDialog dialog;

    public BackupRestoreDataPage(PageParameters params) {

        newBackupRestorePanel = new WebMarkupContainer("newBackupRestorePanel");
        newBackupRestorePanel.setOutputMarkupId(true);

        resetResourcePanel();

        add(newBackupRestorePanel);

        Catalog catalog = GeoServerApplication.get().getCatalog();

        // workspace chooser
        workspace = new WorkspaceDetachableModel(null);
        workspaceChoice =
                new DropDownChoice(
                        "workspace",
                        workspace,
                        new WorkspacesModel(),
                        new WorkspaceChoiceRenderer());
        workspaceChoice.setOutputMarkupId(true);
        workspaceChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        updateTargetWorkspace(target);
                    }
                });
        workspaceChoice.setNullValid(true);
        add(workspaceChoice);

        WebMarkupContainer workspaceNameContainer =
                new WebMarkupContainer("workspaceNameContainer");
        workspaceNameContainer.setOutputMarkupId(true);
        add(workspaceNameContainer);

        workspaceNameTextField = new TextField("workspaceName", new Model());
        workspaceNameTextField.setOutputMarkupId(true);
        boolean defaultWorkspace = catalog.getDefaultWorkspace() != null;
        workspaceNameTextField.setVisible(!defaultWorkspace);
        workspaceNameTextField.setRequired(!defaultWorkspace);
        workspaceNameContainer.add(workspaceNameTextField);

        /** Backup Panel */
        Form backupForm = new Form("backupForm");
        add(backupForm);

        populateBackupForm(backupForm);

        /** Restore Panel */
        Form restoreForm = new Form("restoreForm");
        add(restoreForm);

        populateRestoreForm(restoreForm);

        /** */
        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialWidth(600);
        dialog.setInitialHeight(400);
        dialog.setMinimalHeight(150);
    }

    /** @param target */
    protected void updateTargetWorkspace(AjaxRequestTarget target) {
        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();

        // workspaceNameTextField.setVisible(ws == null);
        // workspaceNameTextField.setRequired(ws == null);

        /*if (target != null) {
            target.add(workspaceNameTextField.getParent());
        }*/
    }

    /** */
    private void resetResourcePanel() {
        if (newBackupRestorePanel.size() > 0) {
            newBackupRestorePanel.remove("backupResource");
        }

        Panel p = new ResourceFilePanel("backupResource");
        newBackupRestorePanel.add(p);
    }

    /** @param form */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void populateBackupForm(Form form) {
        form.add(new CheckBox("backupOptOverwirte", new Model<Boolean>(false)));
        form.add(new CheckBox("backupOptBestEffort", new Model<Boolean>(false)));
        form.add(new CheckBox("backupOptCleanTemp", new Model<Boolean>(true)));
        form.add(statusLabel = new Label("status", new Model()).setOutputMarkupId(true));
        form.add(
                new AjaxSubmitLink("newBackupStart", form) {
                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    }

                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        addFeedbackPanels(target);
                    }

                    protected void onSubmit(AjaxRequestTarget target, final Form<?> form) {

                        // update status to indicate we are working
                        statusLabel.add(AttributeModifier.replace("class", "working-link"));
                        statusLabel.setDefaultModelObject("Working");
                        target.add(statusLabel);

                        // enable cancel and disable this
                        Component cancel = form.get("cancel");
                        cancel.setEnabled(true);
                        target.add(cancel);

                        setEnabled(false);
                        target.add(this);

                        final AjaxSubmitLink self = this;

                        final Long jobid;
                        try {
                            jobid = launchBackupExecution(form);
                        } catch (Exception e) {
                            error(e);
                            LOGGER.log(Level.WARNING, "Error starting a new Backup", e);
                            return;
                        } finally {
                            // update the button back to original state
                            resetButtons(form, target, "newBackupStart");

                            addFeedbackPanels(target);
                        }

                        cancel.setDefaultModelObject(jobid);
                        this.add(
                                new AbstractAjaxTimerBehavior(Duration.milliseconds(100)) {
                                    @Override
                                    protected void onTimer(AjaxRequestTarget target) {
                                        Backup backupFacade = BackupRestoreWebUtils.backupFacade();
                                        BackupExecutionAdapter exec =
                                                backupFacade.getBackupExecutions().get(jobid);

                                        if (!exec.isRunning()) {
                                            try {
                                                if (exec.getAllFailureExceptions() != null
                                                        && !exec.getAllFailureExceptions()
                                                                .isEmpty()) {
                                                    getSession()
                                                            .error(
                                                                    exec.getAllFailureExceptions()
                                                                            .get(0));
                                                    setResponsePage(BackupRestoreDataPage.class);
                                                } else if (exec.isStopping()) {
                                                    // do nothing
                                                } else {
                                                    PageParameters pp = new PageParameters();
                                                    pp.add("id", exec.getId());
                                                    pp.add(
                                                            "clazz",
                                                            BackupExecutionAdapter.class
                                                                    .getSimpleName());

                                                    setResponsePage(BackupRestorePage.class, pp);
                                                }
                                            } catch (Exception e) {
                                                error(e);
                                                LOGGER.log(Level.WARNING, "", e);
                                            } finally {
                                                stop(null);

                                                // update the button back to original state
                                                resetButtons(form, target, "newBackupStart");

                                                addFeedbackPanels(target);
                                            }
                                            return;
                                        }

                                        String msg =
                                                exec != null
                                                        ? exec.getStatus().toString()
                                                        : "Working";

                                        statusLabel.setDefaultModelObject(msg);
                                        target.add(statusLabel);
                                    };

                                    @Override
                                    public boolean canCallListenerInterface(
                                            Component component, Method method) {
                                        if (self.equals(component)
                                                && method.getDeclaringClass()
                                                        .equals(
                                                                org.apache.wicket.behavior
                                                                        .IBehaviorListener.class)
                                                && method.getName().equals("onRequest")) {
                                            return true;
                                        }
                                        return super.canCallListenerInterface(component, method);
                                    }
                                });
                    }

                    private Long launchBackupExecution(Form<?> form) throws Exception {
                        ResourceFilePanel panel =
                                (ResourceFilePanel) newBackupRestorePanel.get("backupResource");
                        Resource archiveFile = null;
                        try {
                            archiveFile = panel.getResource();
                        } catch (NullPointerException e) {
                            throw new Exception("Backup Archive File is Mandatory!");
                        }

                        if (archiveFile == null
                                || archiveFile.getType() == Type.DIRECTORY
                                || FilenameUtils.getExtension(archiveFile.name()).isEmpty()) {
                            throw new Exception(
                                    "Backup Archive File is Mandatory and should not be a Directory or URI.");
                        }

                        Filter filter = null;
                        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();
                        if (ws != null) {
                            filter = ECQL.toFilter("name = '" + ws.getName() + "'");
                        }

                        Hints hints = new Hints(new HashMap(2));

                        Boolean backupOptOverwirte =
                                ((CheckBox) form.get("backupOptOverwirte")).getModelObject();
                        Boolean backupOptBestEffort =
                                ((CheckBox) form.get("backupOptBestEffort")).getModelObject();
                        Boolean backupOptCleanTemp =
                                ((CheckBox) form.get("backupOptCleanTemp")).getModelObject();

                        if (backupOptBestEffort) {
                            hints.add(
                                    new Hints(
                                            new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                                            Backup.PARAM_BEST_EFFORT_MODE));
                        }

                        if (backupOptCleanTemp) {
                            hints.add(
                                    new Hints(
                                            new Hints.OptionKey(Backup.PARAM_CLEANUP_TEMP),
                                            Backup.PARAM_CLEANUP_TEMP));
                        }

                        Backup backupFacade = BackupRestoreWebUtils.backupFacade();

                        return backupFacade
                                .runBackupAsync(archiveFile, backupOptOverwirte, filter, hints)
                                .getId();
                    }
                });

        form.add(
                new AjaxLink<Long>("cancel", new Model<Long>()) {
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    };

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Long jobid = getModelObject();

                        if (jobid != null) {
                            try {
                                BackupRestoreWebUtils.backupFacade().stopExecution(jobid);
                                setResponsePage(BackupRestoreDataPage.class);
                            } catch (NoSuchJobExecutionException
                                    | JobExecutionNotRunningException e) {
                                LOGGER.log(Level.WARNING, "", e);
                            }
                        }

                        setEnabled(false);

                        target.add(this);
                    }
                }.setOutputMarkupId(true).setEnabled(false));

        backupRestoreExecutionsTable =
                new BackupRestoreExecutionsTable(
                        "backups",
                        new BackupRestoreExecutionsProvider(true, BackupExecutionAdapter.class) {
                            @Override
                            protected List<
                                            org.geoserver.web.wicket.GeoServerDataProvider.Property<
                                                    AbstractExecutionAdapter>>
                                    getProperties() {
                                return Arrays.asList(
                                        ID, STATE, STARTED, PROGRESS, ARCHIVEFILE, OPTIONS);
                            }
                        },
                        true,
                        BackupExecutionAdapter.class) {
                    protected void onSelectionUpdate(AjaxRequestTarget target) {};
                };
        backupRestoreExecutionsTable.setOutputMarkupId(true);
        backupRestoreExecutionsTable.setFilterable(false);
        backupRestoreExecutionsTable.setSortable(false);
        form.add(backupRestoreExecutionsTable);
    }

    /** @param form */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void populateRestoreForm(Form form) {
        form.add(new CheckBox("restoreOptDryRun", new Model<Boolean>(false)));
        form.add(new CheckBox("restoreOptBestEffort", new Model<Boolean>(false)));
        form.add(new CheckBox("restoreOptCleanTemp", new Model<Boolean>(true)));
        form.add(statusLabel = new Label("status", new Model()).setOutputMarkupId(true));
        form.add(
                new AjaxSubmitLink("newRestoreStart", form) {
                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    }

                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        addFeedbackPanels(target);
                    }

                    protected void onSubmit(AjaxRequestTarget target, final Form<?> form) {

                        // update status to indicate we are working
                        statusLabel.add(AttributeModifier.replace("class", "working-link"));
                        statusLabel.setDefaultModelObject("Working");
                        target.add(statusLabel);

                        // enable cancel and disable this
                        Component cancel = form.get("cancel");
                        cancel.setEnabled(true);
                        target.add(cancel);

                        setEnabled(false);
                        target.add(this);

                        final AjaxSubmitLink self = this;

                        final Long jobid;
                        try {
                            jobid = launchRestoreExecution(form);
                        } catch (Exception e) {
                            error(e);
                            LOGGER.log(Level.WARNING, "Error starting a new Restore", e);
                            return;
                        } finally {
                            // update the button back to original state
                            resetButtons(form, target, "newRestoreStart");

                            addFeedbackPanels(target);
                        }

                        cancel.setDefaultModelObject(jobid);
                        this.add(
                                new AbstractAjaxTimerBehavior(Duration.milliseconds(100)) {
                                    @Override
                                    protected void onTimer(AjaxRequestTarget target) {
                                        Backup backupFacade = BackupRestoreWebUtils.backupFacade();
                                        RestoreExecutionAdapter exec =
                                                backupFacade.getRestoreExecutions().get(jobid);

                                        if (!exec.isRunning()) {
                                            try {
                                                if (exec.getAllFailureExceptions() != null
                                                        && !exec.getAllFailureExceptions()
                                                                .isEmpty()) {
                                                    getSession()
                                                            .error(
                                                                    exec.getAllFailureExceptions()
                                                                            .get(0));
                                                    setResponsePage(BackupRestoreDataPage.class);
                                                } else if (exec.isStopping()) {
                                                    // do nothing
                                                } else {
                                                    PageParameters pp = new PageParameters();
                                                    pp.add("id", exec.getId());
                                                    pp.add(
                                                            "clazz",
                                                            RestoreExecutionAdapter.class
                                                                    .getSimpleName());

                                                    setResponsePage(BackupRestorePage.class, pp);
                                                }
                                            } catch (Exception e) {
                                                error(e);
                                                LOGGER.log(Level.WARNING, "", e);
                                            } finally {
                                                stop(null);

                                                // update the button back to original state
                                                resetButtons(form, target, "newRestoreStart");

                                                addFeedbackPanels(target);
                                            }
                                            return;
                                        }

                                        String msg =
                                                exec != null
                                                        ? exec.getStatus().toString()
                                                        : "Working";

                                        statusLabel.setDefaultModelObject(msg);
                                        target.add(statusLabel);
                                    };

                                    @Override
                                    public boolean canCallListenerInterface(
                                            Component component, Method method) {
                                        if (self.equals(component)
                                                && method.getDeclaringClass()
                                                        .equals(
                                                                org.apache.wicket.behavior
                                                                        .IBehaviorListener.class)
                                                && method.getName().equals("onRequest")) {
                                            return true;
                                        }
                                        return super.canCallListenerInterface(component, method);
                                    }
                                });
                    }

                    private Long launchRestoreExecution(Form<?> form) throws Exception {
                        ResourceFilePanel panel =
                                (ResourceFilePanel) newBackupRestorePanel.get("backupResource");
                        Resource archiveFile = null;
                        try {
                            archiveFile = panel.getResource();
                        } catch (NullPointerException e) {
                            throw new Exception("Restore Archive File is Mandatory!");
                        }

                        if (archiveFile == null
                                || !Resources.exists(archiveFile)
                                || archiveFile.getType() == Type.DIRECTORY
                                || FilenameUtils.getExtension(archiveFile.name()).isEmpty()) {
                            throw new Exception(
                                    "Restore Archive File is Mandatory, must exists and should not be a Directory or URI.");
                        }

                        Filter filter = null;
                        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();
                        if (ws != null) {
                            filter = ECQL.toFilter("name = '" + ws.getName() + "'");
                        }

                        Hints hints = new Hints(new HashMap(2));

                        Boolean restoreOptDryRun =
                                ((CheckBox) form.get("restoreOptDryRun")).getModelObject();

                        if (restoreOptDryRun) {
                            hints.add(
                                    new Hints(
                                            new Hints.OptionKey(Backup.PARAM_DRY_RUN_MODE),
                                            Backup.PARAM_DRY_RUN_MODE));
                        }

                        Boolean restoreOptBestEffort =
                                ((CheckBox) form.get("restoreOptBestEffort")).getModelObject();

                        if (restoreOptBestEffort) {
                            hints.add(
                                    new Hints(
                                            new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                                            Backup.PARAM_BEST_EFFORT_MODE));
                        }

                        Boolean restoreOptCleanTemp =
                                ((CheckBox) form.get("restoreOptCleanTemp")).getModelObject();

                        if (restoreOptCleanTemp) {
                            hints.add(
                                    new Hints(
                                            new Hints.OptionKey(Backup.PARAM_CLEANUP_TEMP),
                                            Backup.PARAM_CLEANUP_TEMP));
                        }

                        Backup backupFacade = BackupRestoreWebUtils.backupFacade();

                        return backupFacade.runRestoreAsync(archiveFile, filter, hints).getId();
                    }
                });

        form.add(
                new AjaxLink<Long>("cancel", new Model<Long>()) {
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    };

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Long jobid = getModelObject();

                        if (jobid != null) {
                            try {
                                BackupRestoreWebUtils.backupFacade().stopExecution(jobid);
                                setResponsePage(BackupRestoreDataPage.class);
                            } catch (NoSuchJobExecutionException
                                    | JobExecutionNotRunningException e) {
                                LOGGER.log(Level.WARNING, "", e);
                            }
                        }

                        setEnabled(false);

                        target.add(this);
                    }
                }.setOutputMarkupId(true).setEnabled(false));

        restoreExecutionsTable =
                new BackupRestoreExecutionsTable(
                        "restores",
                        new BackupRestoreExecutionsProvider(true, RestoreExecutionAdapter.class) {
                            @Override
                            protected List<
                                            org.geoserver.web.wicket.GeoServerDataProvider.Property<
                                                    AbstractExecutionAdapter>>
                                    getProperties() {
                                return Arrays.asList(
                                        ID, STATE, STARTED, PROGRESS, ARCHIVEFILE, OPTIONS);
                            }
                        },
                        true,
                        RestoreExecutionAdapter.class) {
                    protected void onSelectionUpdate(AjaxRequestTarget target) {};
                };
        restoreExecutionsTable.setOutputMarkupId(true);
        restoreExecutionsTable.setFilterable(false);
        restoreExecutionsTable.setSortable(false);
        form.add(restoreExecutionsTable);
    }

    protected void resetButtons(Form<?> form, AjaxRequestTarget target, String buttonId) {
        form.get(buttonId).setEnabled(true);
        statusLabel.setDefaultModelObject("");
        statusLabel.add(AttributeModifier.replace("class", ""));

        target.add(form.get(buttonId));
        target.add(form.get("status"));
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.or;

import java.time.Duration;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.backuprestore.AbstractExecutionAdapter;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.GeoServerUnlockablePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.api.filter.Filter;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.step.StepExecution;

/**
 * First page of the backup wizard.
 *
 * @author Andrea Aime - OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
// TODO WICKET8 - Verify this page works OK
@SuppressWarnings("serial")
public class BackupRestoreDataPage extends GeoServerSecuredPage implements GeoServerUnlockablePage {

    static Logger LOGGER = Logging.getLogger(BackupRestoreDataPage.class);

    /**
     * Poll interval for the in-page job-progress refresh. The previous value (100&nbsp;ms) issued roughly ten AJAX
     * requests per second against the running server for the whole duration of a backup / restore; two seconds is still
     * responsive while being two orders of magnitude lighter.
     */
    private static final Duration PROGRESS_POLL_INTERVAL = Duration.ofSeconds(2);

    /**
     * Builds a human-readable progress line for the status label, e.g. {@code "STARTED — step 3/9
     * (restoreLayerInfos)"}, from the coarse {@link org.springframework.batch.core.BatchStatus} plus the engine's
     * executed/total step counts and the currently running step name. Falls back to {@code "Working"} when no execution
     * is available yet.
     */
    private static String formatProgress(AbstractExecutionAdapter exec) {
        if (exec == null) {
            return "Working";
        }
        StringBuilder sb = new StringBuilder(String.valueOf(exec.getStatus()));
        sb.append(" — step ").append(exec.getProgress());
        String current = currentStepName(exec);
        if (current != null) {
            sb.append(" (").append(current).append(')');
        }
        return sb.toString();
    }

    /** Name of the step that is currently running, or {@code null} when none is in a running state. */
    private static String currentStepName(AbstractExecutionAdapter exec) {
        if (exec.getStepExecutions() != null) {
            for (StepExecution se : exec.getStepExecutions()) {
                if (se.getStatus() != null && se.getStatus().isRunning()) {
                    return se.getStepName();
                }
            }
        }
        return null;
    }

    WorkspaceModel workspace;
    DropDownChoice workspaceChoice;
    TextField workspaceNameTextField;

    StoreModel<StoreInfo> store;
    DropDownChoice<StoreInfo> storeChoice;

    LayerModel<LayerInfo> layer;
    DropDownChoice<LayerInfo> layerChoice;

    Component statusLabel;

    BackupRestoreExecutionsTable backupRestoreExecutionsTable;
    BackupRestoreExecutionsTable restoreExecutionsTable;

    ResourceFilePanel backupRestoreFileResource;

    GeoServerDialog dialog;

    public BackupRestoreDataPage(PageParameters params) {

        backupRestoreFileResource = new ResourceFilePanel("backupResource", this);

        add(backupRestoreFileResource);

        Catalog catalog = GeoServerApplication.get().getCatalog();

        // workspace chooser
        workspace = new WorkspaceModel<WorkspaceInfo>(backupRestoreFileResource, null);
        workspaceChoice = new DropDownChoice<WorkspaceInfo>(
                "workspace",
                workspace,
                new BackupRestoreWorkspacesIndexModel(backupRestoreFileResource),
                new WorkspaceChoiceRenderer());
        workspaceChoice.setOutputMarkupId(true);
        workspaceChoice.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateTargetWorkspace(target);
            }
        });
        workspaceChoice.setNullValid(true);
        add(workspaceChoice);

        WebMarkupContainer workspaceNameContainer = new WebMarkupContainer("workspaceNameContainer");
        workspaceNameContainer.setOutputMarkupId(true);

        add(workspaceNameContainer);

        workspaceNameTextField = new TextField("workspaceName", new Model());
        workspaceNameTextField.setOutputMarkupId(true);
        boolean defaultWorkspace = catalog.getDefaultWorkspace() != null;
        workspaceNameTextField.setVisible(!defaultWorkspace);
        workspaceNameTextField.setRequired(!defaultWorkspace);

        workspaceNameContainer.add(workspaceNameTextField);

        // store chooser
        store = new StoreModel<StoreInfo>(backupRestoreFileResource, null);
        storeChoice =
                new DropDownChoice<StoreInfo>(
                        "store",
                        store,
                        new BackupRestoreStoresIndexModel(workspace, backupRestoreFileResource),
                        new StoreChoiceRenderer()) {

                    @Override
                    protected String getNullValidKey() {
                        return BackupRestoreDataPage.class.getSimpleName() + "." + super.getNullValidKey();
                    }
                    ;
                };
        storeChoice.setOutputMarkupId(true);
        storeChoice.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateTargetStore(target);
            }
        });
        storeChoice.setNullValid(true);
        add(storeChoice);

        // layer chooser
        layer = new LayerModel<LayerInfo>(backupRestoreFileResource, null);
        layerChoice =
                new DropDownChoice<LayerInfo>(
                        "layer",
                        layer,
                        new BackupRestoreLayersIndexModel(store, backupRestoreFileResource),
                        new LayerChoiceRenderer()) {

                    @Override
                    protected String getNullValidKey() {
                        return BackupRestoreDataPage.class.getSimpleName() + "." + super.getNullValidKey();
                    }
                    ;
                };
        layerChoice.setOutputMarkupId(true);
        layerChoice.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateTargetLayer(target);
            }
        });
        layerChoice.setNullValid(true);
        add(layerChoice);

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
    }

    /** @param target */
    protected void updateTargetWorkspace(AjaxRequestTarget target) {
        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();

        workspaceNameTextField.setVisible(ws == null);
        workspaceNameTextField.setRequired(ws == null);

        if (target != null) {
            target.add(storeChoice);
            target.add(layerChoice);
            target.add(workspaceNameTextField.getParent());
        }
    }

    /** @param target */
    protected void updateTargetStore(AjaxRequestTarget target) {
        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();

        workspaceNameTextField.setVisible(ws == null);
        workspaceNameTextField.setRequired(ws == null);

        if (target != null) {
            target.add(layerChoice);
            target.add(workspaceNameTextField.getParent());
        }
    }

    /** @param target */
    protected void updateTargetLayer(AjaxRequestTarget target) {
        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();

        workspaceNameTextField.setVisible(ws == null);
        workspaceNameTextField.setRequired(ws == null);

        if (target != null) {
            target.add(layerChoice);
            target.add(workspaceNameTextField.getParent());
        }
    }

    /** @param form */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void populateBackupForm(Form form) {
        form.add(new CheckBox("backupOptOverwirte", new Model<Boolean>(false)));
        form.add(new CheckBox("backupOptBestEffort", new Model<Boolean>(true)));
        form.add(new CheckBox("backupOptCleanTemp", new Model<Boolean>(true)));
        form.add(new CheckBox("backupOptSkipGWC", new Model<Boolean>(false)));
        form.add(new CheckBox("backupOptParamPasswords", new Model<Boolean>(false)));
        form.add(new CheckBox("backupOptPreserveIds", new Model<Boolean>(false)));
        // Default-true options: pre-checked to match the documented default (security / global settings excluded).
        form.add(new CheckBox("backupOptSkipSecurity", new Model<Boolean>(true)));
        form.add(new CheckBox("backupOptSkipSettings", new Model<Boolean>(true)));
        form.add(statusLabel = new Label("status", new Model()).setOutputMarkupId(true));
        form.add(new AjaxSubmitLink("newBackupStart", form) {
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
                this.add(new AbstractAjaxTimerBehavior(PROGRESS_POLL_INTERVAL) {
                    @Override
                    protected void onTimer(AjaxRequestTarget target) {
                        Backup backupFacade = BackupRestoreWebUtils.backupFacade();
                        BackupExecutionAdapter exec =
                                backupFacade.getBackupExecutions().get(jobid);

                        if (!exec.isRunning()) {
                            try {
                                if (exec.getAllFailureExceptions() != null
                                        && !exec.getAllFailureExceptions().isEmpty()) {
                                    getSession()
                                            .error(exec.getAllFailureExceptions()
                                                    .get(0));
                                    setResponsePage(BackupRestoreDataPage.class);
                                } else if (exec.isStopping()) {
                                    // do nothing
                                } else {
                                    PageParameters pp = new PageParameters();
                                    pp.add("id", exec.getId());
                                    pp.add("clazz", BackupExecutionAdapter.class.getSimpleName());

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

                        statusLabel.setDefaultModelObject(formatProgress(exec));
                        target.add(statusLabel);
                    }
                    ;

                    @Override
                    public boolean canCallListener(Component component) {
                        if (self.equals(component)) {
                            return true;
                        }
                        return super.canCallListener(component);
                    }
                });
            }

            private Long launchBackupExecution(Form<?> form) throws Exception {
                Resource archiveFile = getBackupRestoreArchiveResource(true);
                Filter wsFilter = null;
                Filter siFilter = null;
                Filter liFilter = null;
                WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();
                StoreInfo si = (StoreInfo) store.getObject();
                LayerInfo li = (LayerInfo) layer.getObject();
                if (ws != null) {
                    wsFilter = or(
                            equal("name", ws.getName()),
                            equal("workspace.name", ws.getName()),
                            equal("resource.store.workspace.name", ws.getName()));
                }
                if (si != null) {
                    siFilter = or(equal("name", si.getName()), equal("resource.store.name", si.getName()));
                }
                if (li != null) {
                    liFilter = or(
                            equal("name", li.getName()),
                            equal("resource.name", li.getResource().getName()));
                }

                Hints hints = new Hints(new HashMap(2));

                Boolean backupOptOverwirte = ((CheckBox) form.get("backupOptOverwirte")).getModelObject();
                Boolean backupOptBestEffort = ((CheckBox) form.get("backupOptBestEffort")).getModelObject();
                Boolean backupOptCleanTemp = ((CheckBox) form.get("backupOptCleanTemp")).getModelObject();

                if (backupOptBestEffort) {
                    hints.add(new Hints(
                            new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
                }

                if (backupOptCleanTemp) {
                    hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_CLEANUP_TEMP), Backup.PARAM_CLEANUP_TEMP));
                }

                Boolean backupOptSkipGWC = ((CheckBox) form.get("backupOptSkipGWC")).getModelObject();
                if (backupOptSkipGWC) {
                    hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_SKIP_GWC), Backup.PARAM_SKIP_GWC));
                }

                Boolean backupOptParamPasswords = ((CheckBox) form.get("backupOptParamPasswords")).getModelObject();
                if (backupOptParamPasswords) {
                    hints.add(new Hints(
                            new Hints.OptionKey(Backup.PARAM_PARAMETERIZE_PASSWDS), Backup.PARAM_PARAMETERIZE_PASSWDS));
                }

                Boolean backupOptPreserveIds = ((CheckBox) form.get("backupOptPreserveIds")).getModelObject();
                if (backupOptPreserveIds) {
                    hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_PRESERVE_IDS), Backup.PARAM_PRESERVE_IDS));
                }

                // Default-true options: always send an explicit true/false so that un-checking actually disables the
                // skip (these are excluded by default, so a missing parameter would otherwise keep skipping them).
                Boolean backupOptSkipSecurity = ((CheckBox) form.get("backupOptSkipSecurity")).getModelObject();
                hints.add(new Hints(
                        new Hints.OptionKey(Backup.PARAM_SKIP_SECURITY_SETTINGS, "*"),
                        Boolean.toString(backupOptSkipSecurity)));

                Boolean backupOptSkipSettings = ((CheckBox) form.get("backupOptSkipSettings")).getModelObject();
                hints.add(new Hints(
                        new Hints.OptionKey(Backup.PARAM_SKIP_SETTINGS, "*"), Boolean.toString(backupOptSkipSettings)));

                Backup backupFacade = BackupRestoreWebUtils.backupFacade();

                return backupFacade
                        .runBackupAsync(archiveFile, backupOptOverwirte, wsFilter, siFilter, liFilter, hints)
                        .getId();
            }
        });

        form.add(
                new AjaxLink<Long>("cancel", new Model<Long>()) {
                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    }
                    ;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Long jobid = getModelObject();

                        if (jobid != null) {
                            try {
                                BackupRestoreWebUtils.backupFacade().stopExecution(jobid);
                                setResponsePage(BackupRestoreDataPage.class);
                            } catch (NoSuchJobExecutionException | JobExecutionNotRunningException e) {
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
                                return Arrays.asList(ID, STATE, STARTED, PROGRESS, ARCHIVEFILE, OPTIONS);
                            }
                        },
                        true,
                        BackupExecutionAdapter.class) {
                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {}
                    ;
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
        form.add(new CheckBox("restoreOptSkipGWC", new Model<Boolean>(false)));
        // Default-true options: pre-checked to match the documented defaults.
        form.add(new CheckBox("restoreOptSkipSecurity", new Model<Boolean>(true)));
        form.add(new CheckBox("restoreOptSkipSettings", new Model<Boolean>(true)));
        form.add(new CheckBox("restoreOptPurgeResources", new Model<Boolean>(true)));
        form.add(statusLabel = new Label("status", new Model()).setOutputMarkupId(true));
        form.add(new AjaxSubmitLink("newRestoreStart", form) {
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
                this.add(new AbstractAjaxTimerBehavior(PROGRESS_POLL_INTERVAL) {
                    @Override
                    protected void onTimer(AjaxRequestTarget target) {
                        Backup backupFacade = BackupRestoreWebUtils.backupFacade();
                        RestoreExecutionAdapter exec =
                                backupFacade.getRestoreExecutions().get(jobid);

                        if (!exec.isRunning()) {
                            try {
                                if (exec.getAllFailureExceptions() != null
                                        && !exec.getAllFailureExceptions().isEmpty()) {
                                    getSession()
                                            .error(exec.getAllFailureExceptions()
                                                    .get(0));
                                    setResponsePage(BackupRestoreDataPage.class);
                                } else if (exec.isStopping()) {
                                    // do nothing
                                } else {
                                    PageParameters pp = new PageParameters();
                                    pp.add("id", exec.getId());
                                    pp.add("clazz", RestoreExecutionAdapter.class.getSimpleName());

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

                        statusLabel.setDefaultModelObject(formatProgress(exec));
                        target.add(statusLabel);
                    }
                    ;

                    @Override
                    public boolean canCallListener(Component component) {
                        if (self.equals(component)) {
                            return true;
                        }
                        return super.canCallListener(component);
                    }
                });
            }

            private Long launchRestoreExecution(Form<?> form) throws Exception {
                Resource archiveFile = getBackupRestoreArchiveResource(false);

                Filter wsFilter = null;
                Filter siFilter = null;
                Filter liFilter = null;
                WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();
                StoreInfo si = (StoreInfo) store.getObject();
                LayerInfo li = (LayerInfo) layer.getObject();
                if (ws != null) {
                    wsFilter = or(
                            equal("name", ws.getName()),
                            equal("workspace.name", ws.getName()),
                            equal("resource.store.workspace.name", ws.getName()));
                }
                if (si != null) {
                    siFilter = or(equal("name", si.getName()), equal("resource.store.name", si.getName()));
                }
                if (li != null) {
                    liFilter = or(
                            equal("name", li.getName()),
                            equal("resource.name", li.getResource().getName()));
                }

                if (wsFilter == null && backupRestoreFileResource.wsFilter != null) {
                    wsFilter = backupRestoreFileResource.wsFilter;
                }

                if (siFilter == null && backupRestoreFileResource.siFilter != null) {
                    siFilter = backupRestoreFileResource.siFilter;
                }

                if (liFilter == null && backupRestoreFileResource.liFilter != null) {
                    liFilter = backupRestoreFileResource.liFilter;
                }

                Hints hints = new Hints(new HashMap(2));

                Boolean restoreOptDryRun = ((CheckBox) form.get("restoreOptDryRun")).getModelObject();

                if (restoreOptDryRun) {
                    hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_DRY_RUN_MODE), Backup.PARAM_DRY_RUN_MODE));
                }

                Boolean restoreOptBestEffort = ((CheckBox) form.get("restoreOptBestEffort")).getModelObject();

                if (restoreOptBestEffort) {
                    hints.add(new Hints(
                            new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
                }

                Boolean restoreOptCleanTemp = ((CheckBox) form.get("restoreOptCleanTemp")).getModelObject();

                if (restoreOptCleanTemp) {
                    hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_CLEANUP_TEMP), Backup.PARAM_CLEANUP_TEMP));
                }

                Boolean restoreOptSkipGWC = ((CheckBox) form.get("restoreOptSkipGWC")).getModelObject();
                if (restoreOptSkipGWC) {
                    hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_SKIP_GWC), Backup.PARAM_SKIP_GWC));
                }

                // Default-true options: always send an explicit true/false so that un-checking takes effect.
                Boolean restoreOptSkipSecurity = ((CheckBox) form.get("restoreOptSkipSecurity")).getModelObject();
                hints.add(new Hints(
                        new Hints.OptionKey(Backup.PARAM_SKIP_SECURITY_SETTINGS, "*"),
                        Boolean.toString(restoreOptSkipSecurity)));

                Boolean restoreOptSkipSettings = ((CheckBox) form.get("restoreOptSkipSettings")).getModelObject();
                hints.add(new Hints(
                        new Hints.OptionKey(Backup.PARAM_SKIP_SETTINGS, "*"),
                        Boolean.toString(restoreOptSkipSettings)));

                Boolean restoreOptPurgeResources = ((CheckBox) form.get("restoreOptPurgeResources")).getModelObject();
                hints.add(new Hints(
                        new Hints.OptionKey(Backup.PARAM_PURGE_RESOURCES, "*"),
                        Boolean.toString(restoreOptPurgeResources)));

                Backup backupFacade = BackupRestoreWebUtils.backupFacade();

                return backupFacade
                        .runRestoreAsync(archiveFile, wsFilter, siFilter, liFilter, hints)
                        .getId();
            }
        });

        form.add(
                new AjaxLink<Long>("cancel", new Model<Long>()) {
                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    }
                    ;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Long jobid = getModelObject();

                        if (jobid != null) {
                            try {
                                BackupRestoreWebUtils.backupFacade().stopExecution(jobid);
                                setResponsePage(BackupRestoreDataPage.class);
                            } catch (NoSuchJobExecutionException | JobExecutionNotRunningException e) {
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
                                return Arrays.asList(ID, STATE, STARTED, PROGRESS, ARCHIVEFILE, OPTIONS);
                            }
                        },
                        true,
                        RestoreExecutionAdapter.class) {
                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {}
                    ;
                };
        restoreExecutionsTable.setOutputMarkupId(true);
        restoreExecutionsTable.setFilterable(false);
        restoreExecutionsTable.setSortable(false);
        form.add(restoreExecutionsTable);
    }

    /** */
    protected Resource getBackupRestoreArchiveResource(boolean isBackup) throws Exception {
        Resource archiveFile = null;
        try {
            archiveFile = backupRestoreFileResource.getResource();
        } catch (NullPointerException e) {
            throw new Exception("Restore Archive File is Mandatory!");
        }

        if (archiveFile == null
                || (!isBackup && !Resources.exists(archiveFile))
                || archiveFile.getType() == Type.DIRECTORY
                || FilenameUtils.getExtension(archiveFile.name()).isEmpty()) {
            throw new Exception("Archive File is Mandatory, must exist and should not be a Directory or URI.");
        }
        return archiveFile;
    }

    protected void resetButtons(Form<?> form, AjaxRequestTarget target, String buttonId) {
        form.get(buttonId).setEnabled(true);
        statusLabel.setDefaultModelObject("");
        statusLabel.add(AttributeModifier.replace("class", ""));

        target.add(form.get(buttonId));
        target.add(form.get("status"));
    }
}

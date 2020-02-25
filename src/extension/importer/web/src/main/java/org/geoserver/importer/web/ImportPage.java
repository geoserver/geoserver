/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import static org.geoserver.importer.web.ImporterWebUtils.importer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.time.Duration;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.importer.BasicImportFilter;
import org.geoserver.importer.Database;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.RasterFormat;
import org.geoserver.importer.VectorFormat;
import org.geoserver.importer.job.Task;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.Icon;

public class ImportPage extends GeoServerSecuredPage {

    GeoServerDialog dialog;

    AtomicBoolean running = new AtomicBoolean(false);

    public ImportPage(PageParameters pp) {
        this(new ImportContextModel(pp.get("id").toLong()));
    }

    public ImportPage(ImportContext imp) {
        this(new ImportContextModel(imp));
    }

    public ImportPage(IModel<ImportContext> model) {
        initComponents(model);
    }

    void initComponents(final IModel<ImportContext> model) {
        add(new Label("id", new PropertyModel(model, "id")));

        ImportContextProvider provider =
                new ImportContextProvider() {
                    @Override
                    protected List<Property<ImportContext>> getProperties() {
                        return Arrays.asList(STATE, CREATED, UPDATED);
                    }

                    @Override
                    protected List<ImportContext> getItems() {
                        return Collections.singletonList(model.getObject());
                    }
                };

        add(
                new AjaxLink("raw") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.setInitialHeight(500);
                        dialog.setInitialWidth(700);
                        dialog.showOkCancel(
                                target,
                                new DialogDelegate() {
                                    @Override
                                    protected Component getContents(String id) {
                                        XStreamPersister xp =
                                                importer().createXStreamPersisterXML();
                                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                                        try {
                                            xp.save(model.getObject(), bout);
                                        } catch (IOException e) {
                                            bout = new ByteArrayOutputStream();
                                            LOGGER.log(Level.FINER, e.getMessage(), e);
                                            e.printStackTrace(new PrintWriter(bout));
                                        }

                                        return new TextAreaPanel(
                                                id, new Model(new String(bout.toByteArray())));
                                    }

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        return true;
                                    }
                                });
                    }
                }.setVisible(ImporterWebUtils.isDevMode()));

        final ImportContextTable headerTable = new ImportContextTable("header", provider);

        headerTable.setOutputMarkupId(true);
        headerTable.setFilterable(false);
        headerTable.setPageable(false);
        add(headerTable);

        final ImportContext imp = model.getObject();
        boolean selectable = imp.getState() != ImportContext.State.COMPLETE;
        final ImportTaskTable taskTable =
                new ImportTaskTable("tasks", new ImportTaskProvider(model), selectable) {
                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        updateImportLink((AjaxLink) ImportPage.this.get("import"), this, target);
                    }
                }.setFeedbackPanel(topFeedbackPanel);
        taskTable.setOutputMarkupId(true);
        taskTable.setFilterable(false);
        add(taskTable);

        final AjaxLink<Long> importLink =
                new AjaxLink<Long>("import", new Model<Long>()) {
                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        ImporterWebUtils.disableLink(tag);
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ImportContext imp = model.getObject();

                        BasicImportFilter filter = new BasicImportFilter();
                        for (ImportTask t : taskTable.getSelection()) {
                            filter.add(t);
                        }

                        // set running flag and update cancel link
                        running.set(true);
                        target.add(cancelLink(this));

                        final Long jobid = importer().runAsync(imp, filter, false);
                        setDefaultModelObject(jobid);

                        final AjaxLink self = this;

                        // create a timer to update the table and reload the page when
                        // necessary
                        taskTable.add(
                                new AbstractAjaxTimerBehavior(Duration.milliseconds(500)) {
                                    @Override
                                    protected void onTimer(AjaxRequestTarget target) {
                                        Task<ImportContext> job = importer().getTask(jobid);
                                        if (job == null || job.isDone()) {
                                            // remove the timer
                                            stop(null);

                                            self.setEnabled(true);
                                            target.add(self);

                                            running.set(false);
                                            target.add(cancelLink(self));

                                            /*ImportContext imp = model.getObject();
                                            if (imp.getState() == ImportContext.State.COMPLETE) {
                                                // enable cancel, which will not be "done"
                                                setLinkEnabled(cancelLink(self), true, target);
                                            } else {
                                                // disable cancel, import is not longer running, but
                                                // also
                                                // not complete
                                                setLinkEnabled(cancelLink(self), false, target);
                                            }*/
                                        }

                                        // update the table
                                        target.add(taskTable);
                                        target.add(headerTable);
                                    }
                                });
                        target.add(taskTable);

                        // disable import button
                        setLinkEnabled(this, false, target);
                        // enable cancel button
                        // setLinkEnabled(cancelLink(this), true, target);
                    }
                };
        importLink.setOutputMarkupId(true);
        importLink.setEnabled(doSelectReady(imp, taskTable, null));
        add(importLink);

        final AjaxLink cancelLink =
                new AjaxLink("cancel") {
                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        ImporterWebUtils.disableLink(tag);
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (!running.get()) {
                            // if (imp.getState() == ImportContext.State.COMPLETE) {
                            setResponsePage(ImportDataPage.class);
                            return;
                        }

                        Long jobid = importLink.getModelObject();
                        if (jobid == null) {
                            return;
                        }

                        Task<ImportContext> task = importer().getTask(jobid);
                        if (task == null || task.isDone()) {
                            return;
                        }

                        task.getMonitor().setCanceled(true);
                        task.cancel(false);
                        try {
                            task.get();
                        } catch (Exception e) {
                        }

                        // enable import button
                        setLinkEnabled(importLink, true, target);
                        // disable cancel button
                        // setLinkEnabled(cancelLink(importLink), false, target);
                    }
                };
        // cancelLink.setEnabled(imp.getState() == ImportContext.State.COMPLETE);
        cancelLink.add(new Label("text", new CancelTitleModel()));
        add(cancelLink);

        WebMarkupContainer selectPanel = new WebMarkupContainer("select");
        selectPanel.add(
                new AjaxLink<ImportContext>("select-all", model) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        taskTable.selectAll();
                        target.add(taskTable);
                        updateImportLink(importLink, taskTable, target);
                    }
                });
        selectPanel.add(
                new AjaxLink<ImportContext>("select-none", model) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        taskTable.clearSelection();
                        target.add(taskTable);
                        updateImportLink(importLink, taskTable, target);
                    }
                });
        selectPanel.add(
                new AjaxLink<ImportContext>("select-ready", model) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doSelectReady(getModelObject(), taskTable, target);
                        target.add(taskTable);
                        updateImportLink(importLink, taskTable, target);
                    }
                });
        add(selectPanel);

        add(new Icon("icon", new DataIconModel(imp.getData())));
        add(
                new Label("title", new DataTitleModel(imp))
                        .add(new AttributeModifier("title", new DataTitleModel(imp, false))));

        add(dialog = new GeoServerDialog("dialog"));
    }

    void updateImportLink(AjaxLink link, ImportTaskTable table, AjaxRequestTarget target) {
        boolean enable = !table.getSelection().isEmpty();
        if (enable) {
            boolean allComplete = true;
            for (ImportTask task : table.getSelection()) {
                allComplete = task.getState() == ImportTask.State.COMPLETE;
            }
            enable = !allComplete;
        }

        setLinkEnabled(link, enable, target);
    }

    void setLinkEnabled(AjaxLink link, boolean enabled, AjaxRequestTarget target) {
        link.setEnabled(enabled);
        target.add(link);
    }

    AjaxLink cancelLink(AjaxLink importLink) {
        return (AjaxLink) importLink.getParent().get("cancel");
    }

    boolean doSelectReady(ImportContext imp, ImportTaskTable table, AjaxRequestTarget target) {
        boolean empty = true;
        for (ImportTask t : imp.getTasks()) {
            if (t.getState() == ImportTask.State.READY) {
                table.selectObject(t);
                empty = false;
            }
        }
        return empty;
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return null;
    }

    static class DataIconModel extends LoadableDetachableModel<PackageResourceReference> {

        ImportData data;

        public DataIconModel(ImportData data) {
            this.data = data;
        }

        @Override
        protected PackageResourceReference load() {
            DataIcon icon = null;
            if (data instanceof FileData) {
                FileData df = (FileData) data;
                if (data instanceof Directory) {
                    icon = DataIcon.FOLDER;
                } else {
                    icon =
                            df.getFormat() instanceof VectorFormat
                                    ? DataIcon.FILE_VECTOR
                                    : df.getFormat() instanceof RasterFormat
                                            ? DataIcon.FILE_RASTER
                                            : DataIcon.FILE;
                }
            } else if (data instanceof Database) {
                icon = DataIcon.DATABASE;
            } else {
                icon = DataIcon.VECTOR; // TODO: better default
            }
            return icon.getIcon();
        }
    }

    static class DataTitleModel extends LoadableDetachableModel<String> {

        long contextId;
        boolean abbrev;

        DataTitleModel(ImportContext imp) {
            this(imp, true);
        }

        DataTitleModel(ImportContext imp, boolean abbrev) {
            this.contextId = imp.getId();
            this.abbrev = abbrev;
        }

        @Override
        protected String load() {
            ImportContext ctx = importer().getContext(contextId);
            ImportData data = ctx.getData();
            String title = data != null ? data.toString() : ctx.toString();

            if (abbrev && title.length() > 70) {
                // shorten it
                title = title.substring(0, 20) + "[...]" + title.substring(title.length() - 50);
            }
            return title;
        }
    }

    class CancelTitleModel implements IModel<String> {

        @Override
        public String getObject() {
            StringResourceModel m =
                    running.get()
                            ? new StringResourceModel("cancel", new Model("Cancel"))
                            : new StringResourceModel("done", new Model("Done"));
            return m.getString();
        }

        @Override
        public void setObject(String object) {}

        @Override
        public void detach() {}
    }

    static class TextAreaPanel extends Panel {

        public TextAreaPanel(String id, IModel textAreaModel) {
            super(id);

            add(new TextArea("textArea", textAreaModel));
        }
    }

    static class FilteredImportTasksModel extends ListModel<ImportTask> {

        IModel<List<ImportTask>> taskModel;
        boolean empty;

        FilteredImportTasksModel(IModel<List<ImportTask>> taskModel, boolean empty) {
            this.taskModel = taskModel;
            this.empty = empty;
        }

        @Override
        public List<ImportTask> getObject() {
            List<ImportTask> tasks = new ArrayList();
            for (ImportTask task : taskModel.getObject()) {
                tasks.add(task);
            }
            return tasks;
        }

        @Override
        public void detach() {
            super.detach();
            taskModel.detach();
        }
    }
}

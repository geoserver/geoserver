/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import static org.geoserver.backuprestore.web.BackupRestoreWebUtils.backupFacade;
import static org.geoserver.backuprestore.web.BackupRestoreWebUtils.humanReadableByteCount;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.backuprestore.AbstractExecutionAdapter;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Paths;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.GeoServerUnlockablePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.Icon;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class BackupRestorePage<T extends AbstractExecutionAdapter> extends GeoServerSecuredPage
        implements GeoServerUnlockablePage {

    public static final PackageResourceReference COMPRESS_ICON =
            new PackageResourceReference(BackupRestorePage.class, "compress.png");

    static final String DETAILS_LEVEL = "expand";

    int expand = 0;

    File backupFile;

    GeoServerDialog dialog;

    private PageParameters params;

    private Class<T> clazz;

    public BackupRestorePage(PageParameters pp) {
        this(
                new BackupRestoreExecutionModel(
                        pp.get("id").toLong(), getType(pp.get("clazz").toString())),
                pp,
                getType(pp.get("clazz").toString()));
    }

    public BackupRestorePage(T bkp, PageParameters pp) {
        this(
                new BackupRestoreExecutionModel(bkp, getType(pp.get("clazz").toString())),
                pp,
                getType(pp.get("clazz").toString()));
    }

    /** */
    private static Class getType(String simpleName) {
        if (BackupExecutionAdapter.class.getSimpleName().equals(simpleName)) {
            return BackupExecutionAdapter.class;
        } else if (RestoreExecutionAdapter.class.getSimpleName().equals(simpleName)) {
            return RestoreExecutionAdapter.class;
        }
        return null;
    }

    public BackupRestorePage(IModel<T> model, PageParameters pp, Class<T> clazz) {
        this.params = pp;
        this.clazz = clazz;

        initComponents(model);
    }

    public Class<T> getType() {
        return this.clazz;
    }

    void initComponents(final IModel<T> model) {
        add(new Label("id", new PropertyModel(model, "id")));
        add(
                new Label(
                        "clazz",
                        new Model(
                                this.clazz
                                        .getSimpleName()
                                        .substring(
                                                0,
                                                this.clazz.getSimpleName().indexOf("Execution")))));

        BackupRestoreExecutionsProvider provider =
                new BackupRestoreExecutionsProvider(getType()) {
                    @Override
                    protected List<Property<AbstractExecutionAdapter>> getProperties() {
                        return Arrays.asList(ID, STATE, STARTED, PROGRESS, ARCHIVEFILE, OPTIONS);
                    }

                    @Override
                    protected List<T> getItems() {
                        return Collections.singletonList(model.getObject());
                    }
                };

        final BackupRestoreExecutionsTable headerTable =
                new BackupRestoreExecutionsTable("header", provider, getType());

        headerTable.setOutputMarkupId(true);
        headerTable.setFilterable(false);
        headerTable.setPageable(false);
        add(headerTable);

        final T bkp = model.getObject();
        boolean selectable = bkp.getStatus() != BatchStatus.COMPLETED;

        add(new Icon("icon", COMPRESS_ICON));
        add(
                new Label("title", new DataTitleModel(bkp))
                        .add(new AttributeModifier("title", new DataTitleModel(bkp, false))));

        @SuppressWarnings("rawtypes")
        Form<?> form = new Form("form");
        add(form);

        try {
            if (params != null && params.getNamedKeys().contains(DETAILS_LEVEL)) {
                if (params.get(DETAILS_LEVEL).toInt() > 0) {
                    expand = params.get(DETAILS_LEVEL).toInt();
                }
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error parsing the 'details level' parameter: ",
                    params.get(DETAILS_LEVEL).toString());
        }

        form.add(
                new SubmitLink("refresh") {
                    @Override
                    public void onSubmit() {
                        setResponsePage(
                                BackupRestorePage.class,
                                new PageParameters()
                                        .add("id", params.get("id").toLong())
                                        .add("clazz", getType().getSimpleName())
                                        .add(DETAILS_LEVEL, expand));
                    }
                });

        NumberTextField<Integer> expand =
                new NumberTextField<Integer>("expand", new PropertyModel<Integer>(this, "expand"));
        expand.add(RangeValidator.minimum(0));
        form.add(expand);

        TextArea<String> details = new TextArea<String>("details", new BKErrorDetailsModel(bkp));
        details.setOutputMarkupId(true);
        details.setMarkupId("details");
        add(details);

        String location = bkp.getArchiveFile().path();
        if (location == null) {
            location = getGeoServerApplication().getGeoServer().getLogging().getLocation();
        }
        backupFile = new File(location);
        if (!backupFile.isAbsolute()) {
            // locate the geoserver.log file
            GeoServerDataDirectory dd =
                    getGeoServerApplication().getBeanOfType(GeoServerDataDirectory.class);
            backupFile = dd.get(Paths.convert(backupFile.getPath())).file();
        }

        if (!backupFile.exists()) {
            error("Could not find the Backup Archive file: " + backupFile.getAbsolutePath());
        }

        /** * DOWNLOAD LINK */
        final Link<Object> downLoadLink =
                new Link<Object>("download") {

                    @Override
                    public void onClick() {
                        IResourceStream stream =
                                new FileResourceStream(backupFile) {
                                    public String getContentType() {
                                        return "application/zip";
                                    }
                                };
                        ResourceStreamRequestHandler handler =
                                new ResourceStreamRequestHandler(stream, backupFile.getName());
                        handler.setContentDisposition(ContentDisposition.ATTACHMENT);

                        RequestCycle.get().scheduleRequestHandlerAfterCurrent(handler);
                    }
                };
        add(downLoadLink);

        /** * PAUSE LINK */
        final AjaxLink pauseLink =
                new AjaxLink("pause") {

                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        AbstractExecutionAdapter bkp = model.getObject();
                        if (bkp.getStatus() == BatchStatus.STOPPED) {
                            setLinkEnabled(
                                    (AjaxLink) downLoadLink.getParent().get("pause"),
                                    false,
                                    target);
                        } else {
                            try {
                                backupFacade().stopExecution(bkp.getId());

                                setResponsePage(BackupRestoreDataPage.class);
                            } catch (NoSuchJobExecutionException
                                    | JobExecutionNotRunningException e) {
                                LOGGER.log(Level.WARNING, "", e);
                                getSession().error(e);
                                setResponsePage(BackupRestoreDataPage.class);
                            }
                        }
                    }
                };
        pauseLink.setEnabled(doSelectReady(bkp) && bkp.getStatus() != BatchStatus.STOPPED);
        add(pauseLink);

        /** * RESUME LINK */
        final AjaxLink resumeLink =
                new AjaxLink("resume") {

                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        AbstractExecutionAdapter bkp = model.getObject();
                        if (bkp.getStatus() != BatchStatus.STOPPED) {
                            setLinkEnabled(
                                    (AjaxLink) downLoadLink.getParent().get("pause"),
                                    false,
                                    target);
                        } else {
                            try {
                                Long id = backupFacade().restartExecution(bkp.getId());

                                PageParameters pp = new PageParameters();
                                pp.add("id", id);
                                if (bkp instanceof BackupExecutionAdapter) {
                                    pp.add("clazz", BackupExecutionAdapter.class.getSimpleName());
                                } else if (bkp instanceof RestoreExecutionAdapter) {
                                    pp.add("clazz", RestoreExecutionAdapter.class.getSimpleName());
                                }

                                setResponsePage(BackupRestorePage.class, pp);
                            } catch (NoSuchJobExecutionException
                                    | JobInstanceAlreadyCompleteException
                                    | NoSuchJobException
                                    | JobRestartException
                                    | JobParametersInvalidException e) {
                                LOGGER.log(Level.WARNING, "", e);
                                getSession().error(e);
                                setResponsePage(BackupRestoreDataPage.class);
                            }
                        }
                    }
                };
        resumeLink.setEnabled(bkp.getStatus() == BatchStatus.STOPPED);
        add(resumeLink);

        /** * ABANDON LINK */
        final AjaxLink cancelLink =
                new AjaxLink("cancel") {

                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        AbstractExecutionAdapter bkp = model.getObject();
                        if (!doSelectReady(bkp)) {
                            setLinkEnabled(
                                    (AjaxLink) downLoadLink.getParent().get("cancel"),
                                    false,
                                    target);
                        } else {
                            try {
                                backupFacade().abandonExecution(bkp.getId());

                                PageParameters pp = new PageParameters();
                                pp.add("id", bkp.getId());
                                if (bkp instanceof BackupExecutionAdapter) {
                                    pp.add("clazz", BackupExecutionAdapter.class.getSimpleName());
                                } else if (bkp instanceof RestoreExecutionAdapter) {
                                    pp.add("clazz", RestoreExecutionAdapter.class.getSimpleName());
                                }

                                setResponsePage(BackupRestorePage.class, pp);
                            } catch (NoSuchJobExecutionException
                                    | JobExecutionAlreadyRunningException e) {
                                error(e);
                                LOGGER.log(Level.WARNING, "", e);
                            }
                        }
                    }
                };
        cancelLink.setEnabled(doSelectReady(bkp));
        add(cancelLink);

        /** * DONE LINK */
        final AjaxLink doneLink =
                new AjaxLink("done") {

                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        tag.setName("a");
                        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(BackupRestoreDataPage.class);
                        return;
                    }
                };
        add(doneLink);

        /** FINALIZE */
        add(dialog = new GeoServerDialog("dialog"));
    }

    /** */
    private boolean doSelectReady(AbstractExecutionAdapter bkp) {
        if (bkp.getStatus() == BatchStatus.COMPLETED
                || bkp.getStatus() == BatchStatus.FAILED
                || bkp.getStatus() == BatchStatus.ABANDONED) {
            return false;
        }

        return true;
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return null;
    }

    class DataTitleModel<T extends AbstractExecutionAdapter>
            extends LoadableDetachableModel<String> {

        long contextId;

        boolean abbrev;

        DataTitleModel(T bkp) {
            this(bkp, true);
        }

        DataTitleModel(T bkp, boolean abbrev) {
            this.contextId = bkp.getId();
            this.abbrev = abbrev;
        }

        @Override
        protected String load() {
            AbstractExecutionAdapter ctx = null;

            if (getType() == BackupExecutionAdapter.class) {
                ctx = backupFacade().getBackupExecutions().get(contextId);
            } else if (getType() == RestoreExecutionAdapter.class) {
                ctx = backupFacade().getRestoreExecutions().get(contextId);
            }
            String title =
                    ctx.getArchiveFile() != null ? ctx.getArchiveFile().path() : ctx.toString();

            if (abbrev && title.length() > 70) {
                // shorten it
                title = title.substring(0, 20) + "[...]" + title.substring(title.length() - 50);
            }

            title =
                    title
                            + " ["
                            + humanReadableByteCount(
                                    FileUtils.sizeOf(ctx.getArchiveFile().file()), false)
                            + "]";

            return title;
        }
    }

    void setLinkEnabled(AjaxLink link, boolean enabled, AjaxRequestTarget target) {
        link.setEnabled(enabled);
        target.add(link);
    }

    class BKErrorDetailsModel<T extends AbstractExecutionAdapter>
            extends LoadableDetachableModel<String> {

        long contextId;

        public BKErrorDetailsModel(AbstractExecutionAdapter bkp) {
            this.contextId = bkp.getId();
        }

        @Override
        protected String load() {
            AbstractExecutionAdapter ctx = null;

            if (getType() == BackupExecutionAdapter.class) {
                ctx = backupFacade().getBackupExecutions().get(contextId);
            } else if (getType() == RestoreExecutionAdapter.class) {
                ctx = backupFacade().getRestoreExecutions().get(contextId);
            }

            StringBuilder buf = new StringBuilder();
            if (!ctx.getAllFailureExceptions().isEmpty()) {
                for (Throwable ex : ctx.getAllFailureExceptions()) {
                    ex = writeException(buf, ex, Level.SEVERE);
                }
            } else {
                buf.append("\nNO Exceptions Detected.\n");
            }

            if (!ctx.getAllWarningExceptions().isEmpty()) {
                for (Throwable ex : ctx.getAllWarningExceptions()) {
                    ex = writeException(buf, ex, Level.WARNING);
                }
            } else {
                buf.append("\nNO Warnings Detected.\n");
            }

            return buf.toString();
        }

        /** */
        private Throwable writeException(StringBuilder buf, Throwable ex, Level level) {
            int cnt = 0;
            while (ex != null) {
                if (buf.length() > 0) {
                    buf.append('\n');
                }
                if (ex.getMessage() != null) {
                    buf.append(level).append(":");
                    buf.append(ex.getMessage());
                    cnt++;
                }
                if (BackupRestorePage.this.expand > 0 && BackupRestorePage.this.expand >= cnt) {
                    StringWriter errors = new StringWriter();
                    ex.printStackTrace(new PrintWriter(errors));
                    buf.append('\n').append(errors.toString());
                }

                ex = ex.getCause();
            }
            return ex;
        }
    }
}

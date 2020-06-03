/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupRestoreItem;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.platform.resource.Resources.AnyFilter;
import org.geoserver.util.Filter;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.StoppableTasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Base Class for Backup and Restore custom Tasklets. <br>
 * Exposes some utility methods to correctly marshall/unmarshall items to/from backup folder.
 *
 * <p>The logic is executed asynchronously using injected {@link #setTaskExecutor(TaskExecutor)} -
 * timeout value is required to be set, so that the batch job does not hang forever if the external
 * process hangs.
 *
 * <p>Tasklet periodically checks for termination status (i.e. {@link
 * #doExecute(StepContribution,ChunkContext,JobExecution)} finished its execution or {@link
 * #setTimeout(long)} expired or job was interrupted). The check interval is given by {@link
 * #setTerminationCheckInterval(long)}.
 *
 * <p>When job interrupt is detected tasklet's execution is terminated immediately by throwing
 * {@link JobInterruptedException}.
 *
 * <p>{@link #setInterruptOnCancel(boolean)} specifies whether the tasklet should attempt to
 * interrupt the thread that executes the system command if it is still running when tasklet exits
 * (abnormally).
 *
 * @author Robert Kasanicky
 * @author Will Schipp
 * @author Alessio Fabiani, GeoSolutions
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractCatalogBackupRestoreTasklet<T> extends BackupRestoreItem
        implements StoppableTasklet, InitializingBean {

    protected static Logger LOGGER = Logging.getLogger(AbstractCatalogBackupRestoreTasklet.class);

    /*
     *
     */
    protected static Map<String, Filter<Resource>> resources =
            new HashMap<String, Filter<Resource>>();

    /*
     *
     */
    static {
        resources.put(
                "/",
                new Filter<Resource>() {

                    @Override
                    public boolean accept(Resource res) {
                        if (res.getType() == Type.DIRECTORY
                                        && !res.name().equalsIgnoreCase("temp")
                                        && !res.name().equalsIgnoreCase("tmp")
                                        && !res.name().equalsIgnoreCase("demo")
                                        && !res.name().equalsIgnoreCase("logs")
                                        && !res.name().equalsIgnoreCase("images")
                                        && !res.name().equalsIgnoreCase("gwc")
                                        && !res.name().equalsIgnoreCase("gwc-layers")
                                        && !res.name().equalsIgnoreCase("layergroups")
                                        && !res.name().equalsIgnoreCase("palettes")
                                        && !res.name().equalsIgnoreCase("plugIns")
                                        && !res.name().equalsIgnoreCase("styles")
                                        && !res.name().equalsIgnoreCase("security")
                                        && !res.name().equalsIgnoreCase("workspaces")
                                        && !res.name().equalsIgnoreCase("user_projections")
                                        && !res.name().equalsIgnoreCase("validation")
                                        && !res.name().equalsIgnoreCase("www")
                                        && !res.name().equalsIgnoreCase("csw")
                                || (res.getType() == Type.RESOURCE
                                        && (res.name().endsWith(".properties")
                                                || res.name().endsWith(".ini")
                                                || res.name().endsWith(".conf")))) {
                            return true;
                        }
                        return false;
                    }
                });
        resources.put("demo", AnyFilter.INSTANCE);
        resources.put("images", AnyFilter.INSTANCE);
        resources.put(
                "logs",
                new Filter<Resource>() {

                    @Override
                    public boolean accept(Resource res) {
                        if (!res.name().endsWith(".xml")) {
                            return true;
                        }
                        return false;
                    }
                });
        resources.put("gwc-layers", AnyFilter.INSTANCE);
        resources.put("layergroups", AnyFilter.INSTANCE);
        resources.put("palettes", AnyFilter.INSTANCE);
        resources.put("plugIns", AnyFilter.INSTANCE);

        // NOTE: it would be better to use ad-hoc Visitors in order to scan the
        // Style Resources and download only the ones needed.
        // This maybe an improvement for a future release/refactoring.
        resources.put(
                "styles",
                new Filter<Resource>() {

                    @Override
                    public boolean accept(Resource res) {
                        if (res.name().toLowerCase().endsWith("sld")
                                || // exclude everything ends with SLD ext (SLD, YSLD, ...)
                                res.name().toLowerCase().endsWith(".xml")
                                || res.name().toLowerCase().endsWith(".css")) // exclude CSS also
                        {
                            return false;
                        }
                        return true;
                    }
                });
        resources.put("user_projections", AnyFilter.INSTANCE);
        resources.put("validation", AnyFilter.INSTANCE);
        resources.put("www", AnyFilter.INSTANCE);
        resources.put("csw", AnyFilter.INSTANCE);
    }

    private long timeout = 0;

    private long checkInterval = 1000;

    private StepExecution execution = null;

    private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

    private boolean interruptOnCancel = false;

    private volatile boolean stopped = false;

    public static final String BR_INDEX_XML = "br_index.xml";

    public AbstractCatalogBackupRestoreTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {
        super.retrieveInterstepData(chunkContext.getStepContext().getStepExecution());
        JobExecution jobExecution =
                chunkContext.getStepContext().getStepExecution().getJobExecution();

        FutureTask<RepeatStatus> theTask =
                new FutureTask<RepeatStatus>(
                        new Callable<RepeatStatus>() {

                            @Override
                            public RepeatStatus call() throws Exception {
                                return doExecute(contribution, chunkContext, jobExecution);
                            }
                        });

        long t0 = System.currentTimeMillis();

        taskExecutor.execute(theTask);

        while (true) {
            Thread.sleep(checkInterval); // moved to the end of the logic

            JobExecution currentExecution =
                    chunkContext.getStepContext().getStepExecution().getJobExecution();

            if (currentExecution.isStopping()) {
                stopped = true;
            }

            if (theTask.isDone()) {
                return theTask.get();
            } else if (System.currentTimeMillis() - t0 > timeout) {
                theTask.cancel(interruptOnCancel);

                JobInterruptedException exception =
                        new JobInterruptedException(
                                "Job " + currentExecution + " did not finish within the timeout.");
                logValidationExceptions((T) null, exception);

                return RepeatStatus.FINISHED;
            } else if (execution != null && execution.isTerminateOnly()) {
                theTask.cancel(interruptOnCancel);

                JobInterruptedException exception =
                        new JobInterruptedException(
                                "Job " + currentExecution + " interrupted while executing.");
                logValidationExceptions((T) null, exception);

                return RepeatStatus.FINISHED;
            } else if (stopped) {
                theTask.cancel(interruptOnCancel);
                contribution.setExitStatus(ExitStatus.STOPPED);
                return RepeatStatus.FINISHED;
            }
        }
    }

    /** */
    abstract RepeatStatus doExecute(
            StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception;

    /** */
    public void backupRestoreAdditionalResources(ResourceStore resourceStore, Resource baseDir)
            throws Exception {
        try {
            String[] excludeFilePaths = null;
            if (getCurrentJobExecution() != null) {
                JobParameters jobParameters = getCurrentJobExecution().getJobParameters();
                if (jobParameters.getString(Backup.PARAM_EXCLUDE_FILE_PATH) != null) {
                    excludeFilePaths =
                            jobParameters.getString(Backup.PARAM_EXCLUDE_FILE_PATH).split(";");
                }
            }
            for (Entry<String, Filter<Resource>> entry : resources.entrySet()) {
                Resource resource = resourceStore.get(entry.getKey());

                List<Resource> resourcesToExclude =
                        checkReosourcesToExclude(resourceStore, resource, excludeFilePaths);

                if (resource != null
                        && Resources.exists(resource)
                        && !resourcesToExclude.contains(resource)) {

                    List<Resource> resources = Resources.list(resource, entry.getValue(), false);

                    Resource targetDir = BackupUtils.dir(baseDir, resource.name());
                    copyResources(
                            resourceStore,
                            excludeFilePaths,
                            resources,
                            entry.getValue(),
                            targetDir);
                }
            }
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }
    }

    /**
     * @param resourceStore
     * @param excludeFilePaths
     * @param resources
     * @param filter
     * @param targetDir
     */
    private void copyResources(
            ResourceStore resourceStore,
            String[] excludeFilePaths,
            List<Resource> resources,
            Filter<Resource> filter,
            Resource targetDir) {
        for (Resource res : resources) {
            try {
                if (!checkReosourcesToExclude(resourceStore, res, excludeFilePaths).contains(res)) {
                    if (res.getType() != Type.DIRECTORY) {
                        Resources.copy(res.file(), targetDir);
                    } else {
                        List<Resource> sub_resources = Resources.list(res, filter, false);
                        if (sub_resources.size() == 0) {
                            Resources.copy(res, BackupUtils.dir(targetDir, res.path()));
                        } else {
                            copyResources(
                                    resourceStore,
                                    excludeFilePaths,
                                    sub_resources,
                                    filter,
                                    targetDir);
                        }
                    }
                } else {
                    LOGGER.log(Level.INFO, "Excluded Resource " + res.path());
                    if (getCurrentJobExecution() != null) {
                        getCurrentJobExecution()
                                .addWarningExceptions(
                                        Arrays.asList(
                                                new Exception("Excluded Resource " + res.path())));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error occurred while trying to move a Resource!", e);
                if (getCurrentJobExecution() != null) {
                    getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                }
            }
        }
    }

    private List<Resource> checkReosourcesToExclude(
            ResourceStore resourceStore, Resource resource, String[] excludeFilePaths)
            throws IOException {
        final String basePath =
                Paths.convert(resourceStore.get(Paths.BASE).dir().getCanonicalPath());
        List<Resource> resourcesToExclude = new ArrayList<Resource>();
        if (excludeFilePaths != null) {
            for (String exclusionPath : excludeFilePaths) {
                if (resourceStore.get(exclusionPath) != null) {
                    String canonicalPath =
                            resource.getType() == Type.DIRECTORY
                                    ? resource.dir().getCanonicalPath()
                                    : resource.file().getCanonicalPath();
                    canonicalPath = Paths.convert(canonicalPath);
                    canonicalPath = canonicalPath.replace(basePath, "");
                    if (canonicalPath.startsWith(exclusionPath)) {
                        resourcesToExclude.add(resource);
                    }
                }
            }
        }
        return resourcesToExclude;
    }

    //
    @SuppressWarnings({"unchecked", "static-access"})
    public void doWrite(Object item, Resource directory, String fileName) throws Exception {
        try {
            if (item instanceof ServiceInfo) {
                ServiceInfo service = (ServiceInfo) item;
                XStreamServiceLoader loader = findServiceLoader(service);

                try {
                    loader.save(
                            service,
                            backupFacade.getGeoServer(),
                            BackupUtils.dir(directory, fileName));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                    // LOGGER.log(Level.SEVERE, "Error occurred while saving configuration", t);
                }
            } else {
                // unwrap dynamic proxies
                OutputStream out = Resources.fromPath(fileName, directory).out();
                try {
                    if (getXp() == null) {
                        xstream = getxStreamPersisterFactory().createXMLPersister();
                        setXp(xstream.getXStream());
                    }
                    item = xstream.unwrapProxies(item);
                    getXp().toXML(item, out);
                } finally {
                    out.close();
                }
            }
        } catch (Exception e) {
            logValidationExceptions((T) item, e);
        }
    }

    //
    @SuppressWarnings({"unchecked"})
    public Object doRead(Resource directory, String fileName) throws Exception {
        Object item = null;
        try {
            InputStream in = Resources.fromPath(fileName, directory).in();

            // Try first using the Services Loaders
            final List<XStreamServiceLoader> loaders =
                    GeoServerExtensions.extensions(XStreamServiceLoader.class);
            for (XStreamServiceLoader<ServiceInfo> l : loaders) {
                try {
                    if (l.getFilename().equals(fileName)) {
                        item =
                                l.load(
                                        backupFacade.getGeoServer(),
                                        Resources.fromPath(fileName, directory));

                        if (item != null && item instanceof ServiceInfo) {
                            return item;
                        }
                    }
                } catch (Exception e) {
                    // Just skip and try with another loader
                    item = null;
                }
            }

            try {
                if (item == null) {
                    try {
                        if (getXp() == null) {
                            xstream = getxStreamPersisterFactory().createXMLPersister();
                            setXp(xstream.getXStream());
                        }
                        item = getXp().fromXML(in);
                    } finally {
                        in.close();
                    }
                }
            } catch (Exception e) {
                // Collect warnings
                item = null;
                if (getCurrentJobExecution() != null) {
                    getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                }
            }
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }

        return item;
    }

    /**
     * This method dumps the current Backup index: - List of Workspaces - List of Stores - List of
     * Layers
     */
    protected void dumpBackupIndex(Resource sourceFolder) throws IOException {
        Element root = new Element("Index");
        Document doc = new Document();

        for (WorkspaceInfo ws : getCatalog().getWorkspaces()) {
            if (!filteredResource(ws, false)) {
                Element workspace = new Element("Workspace");
                workspace.addContent(new Element("Name").addContent(ws.getName()));
                root.addContent(workspace);

                for (DataStoreInfo ds :
                        getCatalog().getStoresByWorkspace(ws.getName(), DataStoreInfo.class)) {
                    if (!filteredResource(ds, ws, true, StoreInfo.class)) {
                        Element store = new Element("Store");
                        store.setAttribute("type", "DataStoreInfo");
                        store.addContent(new Element("Name").addContent(ds.getName()));
                        workspace.addContent(store);

                        for (FeatureTypeInfo ft : getCatalog().getFeatureTypesByDataStore(ds)) {
                            if (!filteredResource(ft, ws, true, ResourceInfo.class)) {
                                for (LayerInfo ly : getCatalog().getLayers(ft)) {
                                    if (!filteredResource(ly, ws, true, LayerInfo.class)) {
                                        Element layer = new Element("Layer");
                                        layer.setAttribute("type", "VECTOR");
                                        layer.addContent(
                                                new Element("Name").addContent(ly.getName()));
                                        store.addContent(layer);
                                    }
                                }
                            }
                        }
                    }
                }

                for (CoverageStoreInfo cs :
                        getCatalog().getStoresByWorkspace(ws.getName(), CoverageStoreInfo.class)) {
                    if (!filteredResource(cs, ws, true, StoreInfo.class)) {
                        Element store = new Element("Store");
                        store.setAttribute("type", "CoverageStoreInfo");
                        store.addContent(new Element("Name").addContent(cs.getName()));
                        workspace.addContent(store);

                        for (CoverageInfo ci : getCatalog().getCoveragesByCoverageStore(cs)) {
                            if (!filteredResource(ci, ws, true, ResourceInfo.class)) {
                                for (LayerInfo ly : getCatalog().getLayers(ci)) {
                                    if (!filteredResource(ly, ws, true, LayerInfo.class)) {
                                        Element layer = new Element("Layer");
                                        layer.setAttribute("type", "RASTER");
                                        layer.addContent(
                                                new Element("Name").addContent(ly.getName()));
                                        store.addContent(layer);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filterIsValid()) {
            Element filter = new Element("Filters");
            if (getFilters().length > 0 && getFilters()[0] != null) {
                Element wsFilter = new Element("Filter");
                wsFilter.setAttribute("type", "WorkspaceInfo");
                wsFilter.addContent(new Element("ECQL").addContent(ECQL.toCQL(getFilters()[0])));
                filter.addContent(wsFilter);
            }

            if (getFilters().length > 1 && getFilters()[1] != null) {
                Element siFilter = new Element("Filter");
                siFilter.setAttribute("type", "StoreInfo");
                siFilter.addContent(new Element("ECQL").addContent(ECQL.toCQL(getFilters()[1])));
                filter.addContent(siFilter);
            }

            if (getFilters().length > 2 && getFilters()[2] != null) {
                Element liFilter = new Element("Filter");
                liFilter.setAttribute("type", "LayerInfo");
                liFilter.addContent(new Element("ECQL").addContent(ECQL.toCQL(getFilters()[2])));
                filter.addContent(liFilter);
            }

            root.addContent(filter);
        }

        doc.setRootElement(root);

        XMLOutputter outter = new XMLOutputter();
        outter.setFormat(Format.getPrettyFormat());
        outter.output(doc, new FileWriter(sourceFolder.get(BR_INDEX_XML).file()));
    }

    @SuppressWarnings({"unchecked"})
    protected XStreamServiceLoader findServiceLoader(ServiceInfo service) {
        XStreamServiceLoader loader = null;

        final List<XStreamServiceLoader> loaders =
                GeoServerExtensions.extensions(XStreamServiceLoader.class);
        for (XStreamServiceLoader<ServiceInfo> l : loaders) {
            if (l.getServiceClass().isInstance(service)) {
                loader = l;
                break;
            }
        }

        if (loader == null) {
            throw new IllegalArgumentException("No loader for " + service.getName());
        }
        return loader;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(backupFacade, "backupFacade must be set");
        Assert.notNull(getxStreamPersisterFactory(), "xstream must be set");
        Assert.isTrue(timeout > 0, "timeout value must be greater than zero");
        Assert.notNull(taskExecutor, "taskExecutor is required");
    }

    /**
     * Timeout in milliseconds.
     *
     * @param timeout upper limit for how long the execution of the external program is allowed to
     *     last.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * The time interval how often the tasklet will check for termination status.
     *
     * @param checkInterval time interval in milliseconds (1 second by default).
     */
    public void setTerminationCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }

    /**
     * Sets the task executor that will be used to execute the system command NB! Avoid using a
     * synchronous task executor
     */
    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * If <code>true</code> tasklet will attempt to interrupt the thread executing the system
     * command if {@link #setTimeout(long)} has been exceeded or user interrupts the job. <code>
     * false</code> by default
     */
    public void setInterruptOnCancel(boolean interruptOnCancel) {
        this.interruptOnCancel = interruptOnCancel;
    }

    /**
     * Will interrupt the thread executing the system command only if {@link
     * #setInterruptOnCancel(boolean)} has been set to true. Otherwise the underlying command will
     * be allowed to finish before the tasklet ends.
     *
     * @since 3.0
     * @see StoppableTasklet#stop()
     */
    @Override
    public void stop() {
        stopped = true;
    }
}

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import com.google.common.collect.Iterators;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleGenerator;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersister.CRSConverter;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.importer.ImportTask.State;
import org.geoserver.importer.job.Job;
import org.geoserver.importer.job.JobQueue;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.job.Task;
import org.geoserver.importer.mosaic.Mosaic;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.RasterTransformChain;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.importer.transform.VectorTransformChain;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.data.CloseableIterator;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.FileGroupProvider;
import org.geotools.api.data.FileServiceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.HarvestedSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Primary controller/facade of the import subsystem.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class Importer implements DisposableBean, ApplicationListener {

    public static final String IMPORTER_STORE_KEY = "org.geoserver.importer.store";
    // Metadata field that triggers calculation of feature native and latlong bounds
    public static final String CALCULATE_BOUNDS = "calculate-bounds";
    static Logger LOGGER = Logging.getLogger(Importer.class);

    public static final String PROPERTYFILENAME = "importer.properties";
    private final ImporterInfoDAO configDAO;
    private Resource configFile;

    /** catalog */
    Catalog catalog;

    /** import context storage */
    ImportStore contextStore;

    /** style generator */
    StyleGenerator styleGen;

    /** style handler */
    StyleHandler styleHandler = new SLDHandler();

    /** job queue */
    JobQueue asynchronousJobs = new JobQueue();

    JobQueue synchronousJobs = new JobQueue();

    ConcurrentHashMap<Long, ImportTask> currentlyProcessing = new ConcurrentHashMap<>();

    ImporterInfo configuration;

    public Importer(Catalog catalog, ImporterInfoDAO dao) {
        this.catalog = catalog;
        this.styleGen = new StyleGenerator(catalog);
        this.configDAO = dao;

        try {
            GeoServerResourceLoader loader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            this.configFile = loader.get("importer/" + PROPERTYFILENAME);
            this.configuration = new ImporterInfoImpl();

            // first load
            LOGGER.log(Level.CONFIG, "Initial importer configuration");
            this.configuration = configDAO.read(configFile);
            asynchronousJobs.setMaximumPoolSize(configuration.getMaxAsynchronousImports());
            synchronousJobs.setMaximumPoolSize(configuration.getMaxSynchronousImports());
            // register to reload (resource events are too slow, can trigger up to 10 seconds later)
            configFile.addListener(c -> reloadConfiguration());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Issues found while loading the importer configuration", e);
            try {
                this.configuration = dao.read(null);
            } catch (IOException ex) {
                // not expected, but still...
                throw new RuntimeException(ex);
            }
        }
    }

    public void reloadConfiguration() {
        try {
            LOGGER.log(Level.CONFIG, "Reload importer configuration");
            configDAO.read(configFile, configuration);
            asynchronousJobs.setMaximumPoolSize(configuration.getMaxAsynchronousImports());
            synchronousJobs.setMaximumPoolSize(configuration.getMaxSynchronousImports());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to update importer configuration");
        }
    }

    /**
     * Style generator.
     *
     * @return style generator for creation of new styles
     */
    public StyleGenerator getStyleGenerator() {
        return styleGen;
    }

    /**
     * Preferred style language for generation.
     *
     * @return preferred style language
     */
    public StyleHandler getStyleHandler() {
        return styleHandler;
    }

    /** Configure importer with preferred style format. */
    public void setStyleHandler(StyleHandler handler) {
        styleHandler = handler;
    }

    /**
     * Importer state persistence, define via {@link #IMPORTER_STORE_KEY}.
     *
     * @return importer state persistence.
     */
    ImportStore createContextStore() {
        // check the spring context for an import store
        ImportStore store = null;

        String name = GeoServerExtensions.getProperty(IMPORTER_STORE_KEY);
        if (name == null) {
            // backward compatability check
            name = GeoServerExtensions.getProperty("org.opengeo.importer.store");
        }

        List<ImportStore> extensions = GeoServerExtensions.extensions(ImportStore.class);
        if (name != null) {
            for (ImportStore bean : extensions) {
                if (name.equals(bean.getName())) {
                    store = bean;
                    break;
                }
            }

            if (store == null) {
                LOGGER.warning("Invalid value for import store, no such store " + name);
            }
        } else if (!extensions.isEmpty()) {
            if (extensions.size() > 1) {
                LOGGER.warning("Found multiple extensions");
            }
            // pick the first found
            store = extensions.get(0);
        }

        if (store == null) {
            store = new MemoryImportStore();
        }

        LOGGER.info("Enabling import store: " + store.getName());
        return store;
    }

    public ImportStore getStore() {
        return contextStore;
    }

    public ImportTask getCurrentlyProcessingTask(long contextId) {
        return currentlyProcessing.get(Long.valueOf(contextId));
    }

    /**
     * Used to setup importer with {@link #createContextStore()} on startup.
     *
     * <p>Also sets up {@code IMPORTER_LOGGING} logging profile, updating if required.
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // load the context store here to avoid circular dependency on creation of the importer
        if (event instanceof ContextLoadedEvent) {
            // provide a helpful logging config for this extension
            GeoServerResourceLoader loader = getCatalog().getResourceLoader();
            LoggingUtils.checkBuiltInLoggingConfiguration(loader, "IMPORTER_LOGGING.xml");

            contextStore = createContextStore();
            contextStore.init();
        }
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public GeoServer getGeoServer() {
        return GeoServerExtensions.bean(GeoServer.class);
    }

    public ImportContext getContext(long id) {
        ImportContext context = contextStore.get(id);
        return context != null ? reattach(context) : null;
    }

    public ImportContext reattach(ImportContext context) {
        // reload store and workspace objects from catalog so they are "attached" with
        // the proper references to the catalog initialized
        context.reattach(catalog);
        for (ImportTask task : context.getTasks()) {
            StoreInfo store = task.getStore();
            if (store != null && store.getId() != null) {
                task.setStore(catalog.getStore(store.getId(), StoreInfo.class));
                // ((StoreInfoImpl) task.getStore()).setCatalog(catalog); // @todo remove if the
                // above sets catalog
            }
            if (task.getLayer() != null) {
                LayerInfo l = task.getLayer();
                if (l.getDefaultStyle() != null && l.getDefaultStyle().getId() != null) {
                    l.setDefaultStyle(catalog.getStyle(l.getDefaultStyle().getId()));
                }
                if (l.getResource() != null) {
                    ResourceInfo r = l.getResource();
                    r.setCatalog(catalog);

                    if (r.getStore() == null && resourceMatchesStore(r, store)) {
                        r.setStore(store);
                    }
                }
            }
        }
        return context;
    }

    public Iterator<ImportContext> getContexts() {
        return contextStore.allNonCompleteImports();
    }

    public Iterator<ImportContext> getContextsByUser(String user) {
        return contextStore.importsByUser(user);
    }

    public Iterator<ImportContext> getAllContexts() {
        return contextStore.iterator();
    }

    public Iterator<ImportContext> getAllContextsByUpdated() {
        try {
            return contextStore.iterator("updated");
        } catch (UnsupportedOperationException e) {
            // fallback
            TreeSet<ImportContext> sorted =
                    new TreeSet<>(
                            (o1, o2) -> {
                                Date d1 = o1.getUpdated();
                                Date d2 = o2.getUpdated();
                                return -1 * d1.compareTo(d2);
                            });
            Iterators.addAll(sorted, contextStore.iterator());
            return sorted.iterator();
        }
    }

    public ImportContext createContext(ImportData data, WorkspaceInfo targetWorkspace)
            throws IOException {
        return createContext(data, targetWorkspace, null);
    }

    public ImportContext createContext(ImportData data, StoreInfo targetStore) throws IOException {
        return createContext(data, null, targetStore);
    }

    public ImportContext createContext(ImportData data) throws IOException {
        return createContext(data, null, null);
    }

    public ImportContext registerContext(Long id) throws IOException, IllegalArgumentException {
        ImportContext context = createContext(id);
        context.setState(org.geoserver.importer.ImportContext.State.INIT);
        return context;
    }

    /**
     * Create a context with the provided optional id. The provided id must be higher than the
     * current mark.
     *
     * @param id optional id to use
     * @return Created ImportContext
     * @throws IllegalArgumentException if the provided id is invalid
     */
    public ImportContext createContext(Long id) throws IOException, IllegalArgumentException {
        ImportContext context = new ImportContext();
        if (id != null) {
            Long retval = contextStore.advanceId(id);
            assert retval == null || retval >= id;
            context.setId(retval);
            contextStore.save(context);
        } else {
            contextStore.add(context);
        }
        LOGGER.log(Level.FINE, "Created new context with id: {0}", context.getId());
        return context;
    }

    public ImportContext createContext(
            ImportData data, WorkspaceInfo targetWorkspace, StoreInfo targetStore)
            throws IOException {
        return createContext(data, targetWorkspace, targetStore, null);
    }

    public ImportContext createContext(
            ImportData data,
            WorkspaceInfo targetWorkspace,
            StoreInfo targetStore,
            ProgressMonitor monitor)
            throws IOException {

        ImportContext context = new ImportContext();
        context.setProgress(monitor);
        context.setData(data);

        if (targetWorkspace == null && targetStore != null) {
            targetWorkspace = targetStore.getWorkspace();
        }
        if (targetWorkspace == null) {
            targetWorkspace = catalog.getDefaultWorkspace();
        }
        context.setTargetWorkspace(targetWorkspace);
        context.setTargetStore(targetStore);

        init(context);
        if (!context.progress().isCanceled()) {
            contextStore.add(context);
        }
        LOGGER.log(Level.FINE, "Context created: {0}", context);
        return context;
    }

    public Long createContextAsync(
            final ImportData data, final WorkspaceInfo targetWorkspace, final StoreInfo targetStore)
            throws IOException {
        LOGGER.log(Level.FINE, "Asynchronous context creation scheduled");
        return asynchronousJobs.submit(
                new SecurityContextCopyingJob<ImportContext>() {
                    @Override
                    protected ImportContext callInternal(ProgressMonitor monitor) throws Exception {
                        return createContext(data, targetWorkspace, targetStore, monitor);
                    }

                    @Override
                    public String toString() {
                        return "Processing data " + data.toString();
                    }
                });
    }

    /**
     * Performs an asynchronous initialization of tasks in the specified context, and eventually
     * saves the result in the {@link ImportStore}
     */
    public Long initAsync(final ImportContext context, final boolean prepData) {
        LOGGER.log(Level.FINE, "Asynchronous context initialization scheduled");
        return asynchronousJobs.submit(
                new SecurityContextCopyingJob<ImportContext>() {
                    @Override
                    protected ImportContext callInternal(ProgressMonitor monitor) throws Exception {
                        try {
                            init(context, prepData);
                        } finally {
                            changed(context);
                        }
                        return context;
                    }

                    @Override
                    public String toString() {
                        return "Initializing context " + context.getId();
                    }
                });
    }

    public void init(ImportContext context) throws IOException {
        init(context, true);
    }

    public void init(ImportContext context, boolean prepData) throws IOException {
        LOGGER.log(Level.FINE, "Initializing context {0}", context);
        context.reattach(catalog);

        try {
            ImportData data = context.getData();
            if (data != null) {
                if (data instanceof RemoteData) {

                    data = ((RemoteData) data).resolve(this);
                    context.setData(data);
                }

                addTasks(context, data, prepData);
            }

            // switch from init to pending as needed
            context.setState(ImportContext.State.PENDING);
            LOGGER.log(Level.FINE, "Context initialized and set to pending: {0}", context);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to init the context ", e);

            // switch to complete to make the error evident, since we
            // cannot attach it to a task
            context.setState(ImportContext.State.INIT_ERROR);
            context.setMessage(e.getMessage());
            return;
        }
    }

    public List<ImportTask> update(ImportContext context, ImportData data) throws IOException {
        List<ImportTask> tasks = addTasks(context, data, true);

        // prep(context);
        changed(context);

        return tasks;
    }

    List<ImportTask> addTasks(ImportContext context, ImportData data, boolean prepData)
            throws IOException {
        if (data == null) {
            return Collections.emptyList();
        }

        LOGGER.log(
                Level.FINE,
                "Adding tasks for context {0} using data {1}",
                new Object[] {context.getId(), data});

        if (prepData) {
            data.prepare(context.progress());
        }

        if (data instanceof FileData && ((FileData) data).getFile() != null) {
            if (data instanceof Mosaic) {
                return initForMosaic(context, (Mosaic) data);
            } else if (data instanceof Directory) {
                return initForDirectory(context, (Directory) data);
            } else {
                return initForFile(context, (FileData) data);
            }
        } else if (data instanceof Database) {
            return initForDatabase(context, (Database) data);
        }

        throw new IllegalStateException();
        /*for (ImportTask t : tasks) {
            context.addTask(t);
        }
        prep(context, prepData);
        return tasks;*/
    }

    /**
     * Initializes the import for a mosaic.
     *
     * <p>Mosaics only support direct import (context.targetStore must be null) and
     */
    List<ImportTask> initForMosaic(ImportContext context, Mosaic mosaic) throws IOException {

        if (context.getTargetStore() != null) {
            throw new IllegalArgumentException("ingest not supported for mosaics");
        }

        return createTasks(mosaic, context);
        // tasks.add(createTask(mosaic, context, context.getTargetStore()));
    }

    List<ImportTask> initForDirectory(ImportContext context, Directory data) throws IOException {
        List<ImportTask> tasks = new ArrayList<>();

        // flatten out the directory into itself and all sub directories and process in order
        for (Directory dir : data.flatten()) {
            LOGGER.log(Level.FINE, "Looking for data to import in {0}", dir);

            // ignore empty directories
            if (dir.getFiles().isEmpty()) continue;

            // group the contents of the directory by format
            Map<DataFormat, List<FileData>> map = new HashMap<>();
            for (FileData f : dir.getFiles()) {
                DataFormat format = f.getFormat();
                List<FileData> files = map.get(format);
                if (files == null) {
                    files = new ArrayList<>();
                    map.put(format, files);
                }
                files.add(f);
            }

            // handle case of importing a single file that we don't know the format of, in this
            // case rather than ignore it we want to process it and sets its state to "NO_FORMAT"
            boolean skipNoFormat = !(map.size() == 1 && map.containsKey(null));

            // if no target store specified group the directory into pieces that can be
            // processed as a single task
            StoreInfo targetStore = context.getTargetStore();
            if (targetStore == null) {

                // create a task for each "format" if that format can handle a directory
                for (DataFormat format : new ArrayList<>(map.keySet())) {
                    if (format != null && format.canRead(dir)) {
                        List<FileData> files = map.get(format);
                        if (files.size() == 1) {
                            // use the file directly
                            // createTasks(files.get(0), format, context, null));
                            tasks.addAll(createTasks(files.get(0), format, context));
                        } else {
                            tasks.addAll(createTasks(dir.filter(files), format, context));
                            // tasks.addAll(createTasks(dir.filter(files), format, context, null));
                        }

                        map.remove(format);
                    }
                }

                // handle the left overs, each file gets its own task
                for (List<FileData> files : map.values()) {
                    for (FileData file : files) {
                        // tasks.add(createTask(file, context, null));
                        tasks.addAll(createTasks(file, file.getFormat(), context, skipNoFormat));
                    }
                }

            } else {
                for (FileData file : dir.getFiles()) {
                    tasks.addAll(createTasks(file, file.getFormat(), context, skipNoFormat));
                }
            }
        }

        return tasks;
    }

    List<ImportTask> initForFile(ImportContext context, FileData file) throws IOException {
        return createTasks(file, context);
    }

    List<ImportTask> initForDatabase(ImportContext context, Database db) throws IOException {
        // JD: we use check for direct vs non-direct in order to determine if there should be
        // one task with many items, or one task per table... can;t think of the use case for
        // many tasks

        // tasks.add(createTask(db, context, targetStore));
        return createTasks(db, context);
    }

    List<ImportTask> createTasks(ImportData data, ImportContext context) throws IOException {
        return createTasks(data, data.getFormat(), context);
    }

    List<ImportTask> createTasks(ImportData data, DataFormat format, ImportContext context)
            throws IOException {
        return createTasks(data, format, context, true);
    }

    List<ImportTask> createTasks(
            ImportData data, DataFormat format, ImportContext context, boolean skipNoFormat)
            throws IOException {

        LOGGER.log(
                Level.FINE,
                "Creating tasks for context {0}, based on data {1}, using format {2}",
                new Object[] {context.getId(), data, format});

        List<ImportTask> tasks = new ArrayList<>();

        boolean direct = false;

        StoreInfo targetStore = context.getTargetStore();
        if (targetStore == null) {
            // direct import, use the format to create a store
            direct = true;

            if (format != null) {
                targetStore = format.createStore(data, context.getTargetWorkspace(), catalog);
                LOGGER.log(Level.FINE, "Created target store {0}", targetStore);
            }

            if (targetStore == null) {
                // format unable to create store, switch to indirect import and use
                // default store from catalog
                targetStore = lookupDefaultStore();
                LOGGER.log(Level.FINE, "Falling back on the default store {0}", targetStore);

                direct = targetStore == null;
            }
        }

        // are we setting up an harvest against an existing store, and the input is also
        // multi-coverage?
        if (targetStore instanceof CoverageStoreInfo
                && targetStore.getId() != null
                && isMultiCoverageInput(format, data)) {
            LOGGER.log(Level.FINE, "Preparing to harvest images into {0}", targetStore);
            CoverageStoreInfo cs = (CoverageStoreInfo) targetStore;
            GridCoverageReader reader = cs.getGridCoverageReader(null, null);

            if (!(reader instanceof StructuredGridCoverage2DReader)) {
                throw new IllegalArgumentException(
                        "Harversting a file into a target raster store can only be done if "
                                + "the store is a structured one (e.g., a mosaic)");
            }
            StructuredGridCoverage2DReader structured = (StructuredGridCoverage2DReader) reader;
            if (structured.isReadOnly()) {
                throw new IllegalArgumentException(
                        "The target structured raster store is read only, cannot harvest into it");
            }

            ImportTask task = new ImportTask(data);
            task.setDirect(false);
            task.setStore(targetStore);
            prep(task);
            task.setState(State.READY);
            task.setError(null);
            task.setTransform(new RasterTransformChain());
            context.addTask(task);
            LOGGER.log(Level.FINE, "Import task created {0}", task);
            return Arrays.asList(task);
        }

        if (format != null) {
            // add tasks by having the format list the available items from the input data
            LOGGER.log(
                    Level.INFO,
                    "Looping through all of the items/files for the input data for "
                                            + data.getName()
                                    != null
                            ? data.getName()
                            : "null name");
            for (ImportTask t : format.list(data, catalog, context.progress())) {
                // initialize transform chain based on vector vs raster
                if (t.getTransform() == null) {
                    t.setTransform(
                            format instanceof VectorFormat
                                    ? new VectorTransformChain()
                                    : new RasterTransformChain());
                }
                t.setDirect(direct);
                t.setStore(targetStore);

                // in case of indirect import against a coverage store with no published
                // layers, do not use the granule name, but the store name
                if (!direct && targetStore instanceof CoverageStoreInfo) {
                    t.getLayer().setName(targetStore.getName());
                    t.getLayer().getResource().setName(targetStore.getName());

                    if (!catalog.getCoveragesByStore((CoverageStoreInfo) targetStore).isEmpty()) {
                        t.setUpdateMode(UpdateMode.APPEND);
                    }
                }

                prep(t);
                LOGGER.log(Level.FINE, "Import task created {0}", t);
                tasks.add(t);
            }
        } else if (!skipNoFormat) {
            ImportTask t = new ImportTask(data);
            t.setDirect(direct);
            t.setStore(targetStore);
            prep(t);
            LOGGER.log(Level.FINE, "Import task created {0}", t);
            tasks.add(t);
        }

        for (ImportTask t : tasks) {
            context.addTask(t);
        }
        return tasks;
    }

    private boolean isMultiCoverageInput(DataFormat format, ImportData data) throws IOException {
        if (!(format instanceof GridFormat)) {
            return false;
        }

        GridFormat gf = (GridFormat) format;
        AbstractGridCoverage2DReader reader = gf.gridReader(data);
        try {
            if (reader instanceof StructuredGridCoverage2DReader) {
                StructuredGridCoverage2DReader structured = (StructuredGridCoverage2DReader) reader;
                // clean up eventual ancillary files (NetCDF case) as the image mosaic might want
                // them created in some other way
                structured.delete(false);
                return true;
            } else {
                return false;
            }
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    boolean prep(ImportTask task) {
        if (task.getState() == ImportTask.State.COMPLETE) {
            return true;
        }

        // check the format
        DataFormat format = task.getData().getFormat();
        if (format == null) {
            task.setState(State.NO_FORMAT);
            return false;
        }

        // check the target
        if (task.getStore() == null) {
            task.setError(new Exception("No target store for task"));
            task.setState(State.ERROR);
            return false;
        }

        // check for a mismatch between store and format
        if (!formatMatchesStore(format, task.getStore())) {
            String msg =
                    task.getStore() instanceof DataStoreInfo
                            ? "Unable to import raster data into vector store"
                            : "Unable to import vector data into raster store";

            task.setError(new Exception(msg));
            task.setState(State.BAD_FORMAT);
            return false;
        }

        if (task.getLayer() == null || task.getLayer().getResource() == null) {
            task.setError(new Exception("Task has no layer configuration"));
            task.setState(State.ERROR);
            return false;
        }

        LayerInfo l = task.getLayer();
        ResourceInfo r = l.getResource();

        // initialize resource references
        r.setStore(task.getStore());
        r.setNamespace(catalog.getNamespaceByPrefix(task.getStore().getWorkspace().getName()));

        // style
        // assign a default style to the layer if not already done
        if (l.getDefaultStyle() == null) {
            try {
                StyleInfo style = null;

                // check the case of a style file being uploaded via zip along with rest of files
                if (task.getData() instanceof SpatialFile) {
                    SpatialFile file = (SpatialFile) task.getData();
                    if (file.getStyleFile() != null) {
                        style = createStyleFromFile(file.getStyleFile(), task);
                        LOGGER.log(
                                Level.FINE,
                                "Found a style {0} for file {1}",
                                new Object[] {style, file.getFile()});
                    }
                }

                if (style == null) {
                    if (r instanceof FeatureTypeInfo) {
                        // since this resource is still detached from the catalog we can't call
                        // through to get its underlying resource, so we depend on the "native"
                        // type provided from the format
                        FeatureType featureType =
                                (FeatureType) task.getMetadata().get(FeatureType.class);
                        if (featureType != null) {
                            style =
                                    styleGen.createStyle(
                                            styleHandler, (FeatureTypeInfo) r, featureType);
                            LOGGER.log(
                                    Level.FINE,
                                    "Generated a style {0} for feature type {1}",
                                    new Object[] {style, featureType});
                        } else {
                            throw new RuntimeException("Unable to compute style");
                        }
                    } else if (r instanceof CoverageInfo) {
                        style = styleGen.createStyle(styleHandler, (CoverageInfo) r);
                        LOGGER.log(
                                Level.FINE,
                                "Generated a style {0} for coverage {1}",
                                new Object[] {style, r});
                    } else {
                        throw new RuntimeException("Unknown resource type :" + r.getClass());
                    }
                }
                l.setDefaultStyle(style);
            } catch (Exception e) {
                task.setError(e);
                task.setState(ImportTask.State.ERROR);
                return false;
            }
        }

        // srs
        if (r.getSRS() == null) {
            LOGGER.log(Level.FINE, "Resource lacks SRS, stopping task preparation: {0}", r);
            task.setState(ImportTask.State.NO_CRS);
            return false;
        } else if (task.getState() == ImportTask.State.NO_CRS) {
            // changed after setting srs manually, compute the lat long bounding box
            try {
                computeLatLonBoundingBox(task, false);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error computing lat long bounding box", e);
                task.setState(ImportTask.State.ERROR);
                task.setError(e);
                return false;
            }

            // also since this resource has no native crs set the project policy to force declared
            task.getLayer().getResource().setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        } else {
            task.getLayer().getResource().setProjectionPolicy(ProjectionPolicy.NONE);
        }

        // bounds
        if (r.getNativeBoundingBox() == null) {
            LOGGER.log(
                    Level.FINE, "Resource lacks bounding box, stopping task preparation: {0}", r);
            task.setState(ImportTask.State.NO_BOUNDS);
            return false;
        }

        task.setState(ImportTask.State.READY);
        LOGGER.log(Level.FINE, "Task preparation complete, marked as READY: {0}", task);
        return true;
    }

    boolean formatMatchesStore(DataFormat format, StoreInfo store) {
        if (format instanceof VectorFormat) {
            return store instanceof DataStoreInfo;
        }
        if (format instanceof GridFormat) {
            return store instanceof CoverageStoreInfo;
        }
        return false;
    }

    boolean resourceMatchesStore(ResourceInfo resource, StoreInfo store) {
        if (resource instanceof FeatureTypeInfo) {
            return store instanceof DataStoreInfo;
        }
        if (resource instanceof CoverageInfo) {
            return store instanceof CoverageStoreInfo;
        }
        return false;
    }

    public void run(ImportContext context) throws IOException {
        run(context, ImportFilter.ALL);
    }

    public void run(ImportContext context, ImportFilter filter) throws IOException {
        run(context, filter, null);
    }

    public void run(ImportContext context, ImportFilter filter, ProgressMonitor monitor)
            throws IOException {
        Long taskId = runOnPool(synchronousJobs, context, filter, false);
        Task<?> task = synchronousJobs.getTask(taskId);
        // wait for it to complete
        try {
            task.get();
        } catch (InterruptedException e) {
            throw new IOException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new IOException(cause);
            }
        }
    }

    public Long runAsync(
            final ImportContext context, final ImportFilter filter, final boolean init) {
        return runOnPool(asynchronousJobs, context, filter, init);
    }

    protected void runInternal(ImportContext context, ImportFilter filter, ProgressMonitor monitor)
            throws IOException {
        if (context.getState() == ImportContext.State.INIT) {
            throw new IllegalStateException("Importer is still initializing, cannot run it");
        }

        context.setProgress(monitor);
        context.setState(ImportContext.State.RUNNING);
        contextStore.save(context);

        LOGGER.log(Level.FINE, "Running import {0}", context.getId());

        for (ImportTask task : context.getTasks()) {
            if (!filter.include(task)) {
                LOGGER.log(Level.FINE, "Filtering out task {0}", task);
                continue;
            }
            if (!task.readyForImport()) {
                LOGGER.log(Level.FINE, "Skipping task not ready for import: {0}", task);
                continue;
            }

            if (context.progress().isCanceled()) {
                break;
            }
            run(task);
        }

        LOGGER.log(Level.FINE, "All tasks run for {0}", context);
        context.updated();
        contextStore.save(context);

        if (context.isArchive() && context.getState() == ImportContext.State.COMPLETE) {
            if (!context.isDirect()) {
                final Directory directory = context.getUploadDirectory();

                if (directory != null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Archiving directory " + directory.getFile().getAbsolutePath());
                    }
                    try {
                        directory.archive(getArchiveFile(context));
                    } catch (Exception ioe) {
                        // this is not a critical operation, so don't make the whole thing fail
                        LOGGER.log(Level.WARNING, "Error archiving", ioe);
                    }
                }
            }
        }
    }

    void run(ImportTask task) throws IOException {
        if (task.getState() == ImportTask.State.COMPLETE) {
            return;
        }
        LOGGER.log(Level.FINE, "Running task {0}", task);
        task.setState(ImportTask.State.RUNNING);
        contextStore.save(task.getContext());

        try {
            if (task.isDirect()) {
                // direct import, simply add configured store and layers to catalog
                doDirectImport(task);
            } else {
                // indirect import, read data from the source and into the target store
                doIndirectImport(task);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Task failed during import: " + task, e);
            task.setState(ImportTask.State.ERROR);
            task.setError(e);
        }
    }

    public File getArchiveFile(ImportContext context) throws IOException {
        // String archiveName = "import-" + task.getContext().getId() + "-" + task.getId() + "-" +
        // task.getData().getName() + ".zip";
        String archiveName = "import-" + context.getId() + ".zip";
        File dir =
                getCatalog().getResourceLoader().findOrCreateDirectory(getUploadRoot(), "archives");
        return new File(dir, archiveName);
    }

    public void changed(ImportContext context) throws IOException {
        context.updated();
        contextStore.save(context);
    }

    public void changed(ImportTask task) throws IOException {
        prep(task);
        changed(task.getContext());
    }

    private Long runOnPool(
            JobQueue asynchronousJobs, ImportContext context, ImportFilter filter, boolean init) {
        // creating an asynchronous importer job
        return asynchronousJobs.submit(
                new SecurityContextCopyingJob<ImportContext>() {

                    @Override
                    protected ImportContext callInternal(ProgressMonitor monitor) throws Exception {
                        if (init) {
                            init(context, true);
                        }
                        try {
                            runInternal(context, filter, monitor);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Import " + context.getId() + " failed", e);
                            context.setState(ImportContext.State.COMPLETE_ERROR);
                            context.setMessage(e.getMessage());
                        }
                        return context;
                    }

                    @Override
                    public String toString() {
                        return "Processing import " + context.getId();
                    }
                });
    }

    protected abstract class SecurityContextCopyingJob<T> extends Job<T> {
        final RequestAttributes parentRequestAttributes;
        final Authentication auth;
        final Thread parentThread;

        protected SecurityContextCopyingJob() {
            // we store the current request spring context
            parentRequestAttributes = RequestContextHolder.getRequestAttributes();
            auth = SecurityContextHolder.getContext().getAuthentication();
            parentThread = Thread.currentThread();
        }

        @Override
        protected final T call(ProgressMonitor monitor) throws Exception {
            final Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();
            try {
                // set the parent request spring context, some interceptors like the security ones
                // for example may need to have access to the original request attributes
                RequestContextHolder.setRequestAttributes(parentRequestAttributes);
                SecurityContextHolder.getContext().setAuthentication(auth);
                return callInternal(monitor);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to run job in background", e);
                throw e;
            } finally {
                if (Thread.currentThread() != parentThread) {
                    // cleaning request spring context for the current thread
                    RequestContextHolder.resetRequestAttributes();
                    SecurityContextHolder.getContext().setAuthentication(oldAuth);
                }
            }
        }

        protected abstract T callInternal(ProgressMonitor monitor) throws Exception;
    }

    @SuppressWarnings("unchecked")
    public Task<ImportContext> getTask(Long job) {
        return (Task<ImportContext>) asynchronousJobs.getTask(job);
    }

    @SuppressWarnings("unchecked")
    public List<Task<ImportContext>> getTasks() {
        return (List) asynchronousJobs.getTasks();
    }

    /*
     * an import that involves consuming a data source directly
     */
    void doDirectImport(ImportTask task) throws IOException {
        LOGGER.log(Level.FINE, "Running direct import for task {0}", task.getId());
        // TODO: this needs to be transactional in case of errors along the way

        // add the store, may have been added in a previous iteration of this task
        if (task.getStore().getId() == null) {
            StoreInfo store = task.getStore();

            // ensure a unique name
            store.setName(findUniqueStoreName(task.getStore()));

            // ensure a namespace connection parameter set matching workspace/namespace
            if (!store.getConnectionParameters().containsKey("namespace")) {
                WorkspaceInfo ws = task.getContext().getTargetWorkspace();
                if (ws == null && task.getContext().getTargetStore() != null) {
                    ws = task.getContext().getTargetStore().getWorkspace();
                }
                if (ws != null) {
                    NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
                    if (ns != null) {
                        store.getConnectionParameters().put("namespace", ns.getURI());
                    }
                }
            }
            LOGGER.log(
                    Level.FINE,
                    "Creating target store {0} for task {1}",
                    new Object[] {task.getStore(), task.getId()});
            catalog.add(task.getStore());
        }

        task.setState(ImportTask.State.RUNNING);

        // set up transform chain
        TransformChain tx = task.getTransform();

        // apply pre transform
        if (!doPreTransform(task, task.getData(), tx)) {
            return;
        }

        addToCatalog(task);

        if (task.getLayer().getResource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo featureType = (FeatureTypeInfo) task.getLayer().getResource();
            FeatureTypeInfo resource =
                    getCatalog()
                            .getResourceByName(
                                    featureType.getQualifiedName(), FeatureTypeInfo.class);
            if (resource != null) {
                calculateBounds(resource);
            }
        }

        // apply post transform
        if (!doPostTransform(task, task.getData(), tx)) {
            return;
        }

        task.setState(ImportTask.State.COMPLETE);
    }

    /*
     * an import that involves reading from the store and writing into a specified target store
     */
    void doIndirectImport(ImportTask task) throws IOException {
        if (!task.getStore().isEnabled()) {
            task.getStore().setEnabled(true);
        }

        if (task.progress().isCanceled()) {
            return;
        }

        task.setState(ImportTask.State.RUNNING);

        // setup transform chain
        TransformChain tx = task.getTransform();

        // pre transform
        if (!doPreTransform(task, task.getData(), tx)) {
            return;
        }

        boolean canceled = false;
        DataFormat format = task.getData().getFormat();
        if (format instanceof VectorFormat) {
            try {
                currentlyProcessing.put(task.getContext().getId(), task);
                loadIntoDataStore(
                        task,
                        (DataStoreInfo) task.getStore(),
                        (VectorFormat) format,
                        (VectorTransformChain) tx);
                canceled = task.progress().isCanceled();

                FeatureTypeInfo featureType = (FeatureTypeInfo) task.getLayer().getResource();
                featureType.getAttributes().clear();

                if (!canceled) {
                    // check if resource is already present
                    FeatureTypeInfo resource =
                            getCatalog()
                                    .getResourceByName(
                                            featureType.getQualifiedName(), FeatureTypeInfo.class);

                    if (resource == null
                            && (task.getUpdateMode() == UpdateMode.CREATE
                                    || task.getUpdateMode() == UpdateMode.REPLACE)) {
                        // Create if needed (for create or replace mode)
                        // Replace mode can be used to update table contents in place
                        // and publish the results as a new layer
                        addToCatalog(task);
                        resource =
                                getCatalog()
                                        .getResourceByName(
                                                featureType.getQualifiedName(),
                                                FeatureTypeInfo.class);
                    }

                    if (resource != null) {
                        calculateBounds(resource);
                    }
                }
            } catch (Throwable th) {
                LOGGER.log(Level.SEVERE, "Error occured during import", th);
                Exception e = (th instanceof Exception) ? (Exception) th : new Exception(th);
                task.setError(e);
                task.setState(ImportTask.State.ERROR);
                return;
            } finally {
                currentlyProcessing.remove(task.getContext().getId());
            }
        } else {
            StoreInfo store = task.getStore();
            if (!(store instanceof CoverageStoreInfo)) {
                throw new IllegalArgumentException(
                        "Indirect raster import can only work against "
                                + " CoverageStores, this one is not: "
                                + store);
            }

            loadIntoCoverageStore(
                    task,
                    (CoverageStoreInfo) store,
                    (GridFormat) format,
                    (RasterTransformChain) tx);
        }

        if (!canceled && !doPostTransform(task, task.getData(), tx)) {
            return;
        }

        task.setState(canceled ? ImportTask.State.CANCELED : ImportTask.State.COMPLETE);
    }

    /**
     * (Re)calculates the bounds for a FeatureTypeInfo. Bounds will be calculated if:
     * <li>The native bounds of the resource are null or empty
     * <li>The resource has a metadata entry "recalculate-bounds"="true"<br>
     *     <br>
     *     Otherwise, this method has no effect.<br>
     *     <br>
     *     If the metadata entry "recalculate-bounds"="true" exists, it will be removed after bounds
     *     are calculated.<br>
     *     <br>
     *     This is currently used by csv / kml uploads that have a geometry that may be the result
     *     of a transform, and by JDBC imports which wait to calculate bounds until after the layers
     *     that will be imported have been chosen.
     *
     * @param resource The resource to calculate the bounds for
     */
    protected void calculateBounds(FeatureTypeInfo resource) throws IOException {
        if (resource != null
                && (resource.getNativeBoundingBox() == null
                        || resource.getNativeBoundingBox().isEmpty()
                        || Boolean.TRUE.equals(resource.getMetadata().get(CALCULATE_BOUNDS))
                        || "true".equals(resource.getMetadata().get(CALCULATE_BOUNDS)))) {
            String boundsStatus = "Bounds are previously populated, calculating for again.";
            if (resource.getNativeBoundingBox() == null
                    || resource.getNativeBoundingBox().isEmpty()) {
                boundsStatus = "Bounds are null/empty, calculating for the first time.";
            }
            LOGGER.log(
                    Level.INFO,
                    "Calculating bounds for "
                            + (resource.getName() != null ? resource.getName() : "null")
                            + " "
                            + boundsStatus);
            // force computation
            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            ReferencedEnvelope nativeBounds = cb.getNativeBounds(resource);
            resource.setNativeBoundingBox(nativeBounds);
            resource.setLatLonBoundingBox(cb.getLatLonBounds(nativeBounds, resource.getCRS()));
            getCatalog().save(resource);

            // Do not re-calculate on subsequent imports
            if (resource.getMetadata().get(CALCULATE_BOUNDS) != null) {
                resource.getMetadata().remove(CALCULATE_BOUNDS);
            }
        }
    }

    private void checkSingleHarvest(List<HarvestedSource> harvests) throws IOException {
        for (HarvestedSource harvested : harvests) {
            if (!harvested.success()) {
                throw new IOException(
                        "Failed to harvest "
                                + harvested.getSource()
                                + ": "
                                + harvested.getMessage());
            }
        }
    }

    private void harvestDirectory(StructuredGridCoverage2DReader sr, Directory data)
            throws UnsupportedOperationException, IOException {
        for (FileData fd : data.getFiles()) {
            harvestImportData(sr, fd);
        }
    }

    private void harvestImportData(StructuredGridCoverage2DReader sr, ImportData data)
            throws IOException {
        if (data instanceof SpatialFile) {
            SpatialFile sf = (SpatialFile) data;
            List<HarvestedSource> harvests = sr.harvest(null, sf.getFile(), null);
            checkSingleHarvest(harvests);
        } else if (data instanceof Directory) {
            harvestDirectory(sr, (Directory) data);
        } else {
            unsupportedHarvestFileData(data);
        }
    }

    private void unsupportedHarvestFileData(ImportData fd) {
        throw new IllegalArgumentException(
                "Unsupported data type for raster harvesting (use SpatialFile or Directory): "
                        + fd);
    }

    boolean doPreTransform(ImportTask task, ImportData data, TransformChain tx) {
        try {
            tx.pre(task, data);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occured during pre transform", e);
            task.setError(e);
            task.setState(ImportTask.State.ERROR);
            return false;
        }
        return true;
    }

    boolean doPostTransform(ImportTask task, ImportData data, TransformChain tx) {
        try {
            tx.post(task, data);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occured during post transform", e);
            task.setError(e);
            task.setState(ImportTask.State.ERROR);
            return false;
        }
        return true;
    }

    void loadIntoDataStore(
            ImportTask task, DataStoreInfo store, VectorFormat format, VectorTransformChain tx)
            throws Throwable {
        ImportData data = task.getData();
        @SuppressWarnings("PMD.CloseResource") // conditionally created, then closed
        FeatureReader reader = null;

        // using this exception to throw at the end
        Throwable error = null;
        Transaction transaction = new DefaultTransaction();
        try { // NOPMD - Want to catch error handling transaction closing

            SimpleFeatureType featureType = task.getFeatureType();
            task.setOriginalLayerName(featureType.getTypeName());
            String nativeName = task.getLayer().getResource().getNativeName();

            if (!featureType.getTypeName().equals(nativeName)) {
                LOGGER.log(
                        Level.INFO,
                        "Feature Type name has been changed from "
                                + (featureType.getTypeName() != null
                                        ? "'" + featureType.getTypeName() + "'"
                                        : "null name")
                                + " to '"
                                + nativeName
                                + "'");
                SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
                tb.init(featureType);
                tb.setName(nativeName);
                featureType = tb.buildFeatureType();
            }

            DataStore dataStore = (DataStore) store.getDataStore(null);

            FeatureDataConverter featureDataConverter = FeatureDataConverter.DEFAULT;
            if (isShapefileDataStore(dataStore)) {
                featureDataConverter = FeatureDataConverter.TO_SHAPEFILE;
            } else if (isOracleDataStore(dataStore)) {
                featureDataConverter = FeatureDataConverter.TO_ORACLE;
            } else if (isPostGISDataStore(dataStore)) {
                featureDataConverter = FeatureDataConverter.TO_POSTGIS;
            }
            // conversion may adjust feature type name and attribute names and data types
            // to match the abilities of the data store format being used
            featureType = featureDataConverter.convertType(featureType, format, data, task);
            UpdateMode updateMode = task.getUpdateMode();

            // created native type name in target datastore, will be dropped if import fails
            String createdNativeTypeName = null;

            if (updateMode == UpdateMode.CREATE) {
                // find a unique native name in the target store (to avoid replacing existing
                // content)
                nativeName = findUniqueNativeFeatureTypeName(featureType, store);

                if (!nativeName.equals(featureType.getName().getLocalPart())) {
                    // update the layer name to be unique within target workspace
                    task.getLayer().getResource().setName(nativeName);
                    // update the metadata native name to reflect the data storage
                    task.getLayer().getResource().setNativeName(nativeName);

                    // retype
                    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                    // use of init to preserve as many details as possible
                    typeBuilder.init(featureType);
                    typeBuilder.setName(nativeName);

                    featureType = typeBuilder.buildFeatureType();
                }
            } else {
                // @todo what to do if featureType transform is present?

                // @todo implement me - need to specify attribute used for id
                if (updateMode == UpdateMode.UPDATE) {
                    FeatureStore fs =
                            (FeatureStore) dataStore.getFeatureSource(featureType.getTypeName());
                    fs.setTransaction(transaction);

                    throw new UnsupportedOperationException(
                            "updateMode UPDATE is not supported yet");
                }
            }

            if (updateMode == UpdateMode.CREATE || updateMode == UpdateMode.REPLACE) {
                // @todo HACK remove this at some point when timezone issues are fixed
                // this will force postgis to create timezone w/ timestamp fields
                if (dataStore instanceof JDBCDataStore) {
                    JDBCDataStore ds = (JDBCDataStore) dataStore;
                    // sniff for postgis (h2 is used in tests and will cause failure if this occurs)
                    if (ds.getSqlTypeNameToClassMappings().containsKey("timestamptz")) {
                        ds.getSqlTypeToSqlTypeNameOverrides()
                                .put(java.sql.Types.TIMESTAMP, "timestamptz");
                    }
                }
            }
            // apply the feature type transform
            featureType = tx.inline(task, dataStore, featureType);

            if (updateMode == UpdateMode.CREATE) {
                LOGGER.info(
                        "Create layer '"
                                + task.getLayer().getResource().getName()
                                + "' "
                                + "with new native schema '"
                                + featureType.getTypeName()
                                + "'");

                dataStore.createSchema(featureType);
                createdNativeTypeName = featureType.getTypeName();

            } else if (updateMode == UpdateMode.REPLACE) {
                if (Arrays.asList(dataStore.getTypeNames()).contains(featureType.getTypeName())) {
                    SimpleFeatureStore fs =
                            (SimpleFeatureStore)
                                    dataStore.getFeatureSource(featureType.getTypeName());

                    if (schemaEqualsIgnoreNamespace(fs.getSchema(), featureType)) {
                        LOGGER.info(
                                "Replace layer '"
                                        + task.getLayer().getResource().getName()
                                        + "' "
                                        + "used to replace contents of '"
                                        + featureType.getTypeName()
                                        + "' native schema");
                        fs.setTransaction(transaction);
                        fs.removeFeatures(Filter.INCLUDE);
                    } else {
                        LOGGER.info(
                                "Replace layer '"
                                        + task.getLayer().getResource().getName()
                                        + "' "
                                        + "used to replace contents of '"
                                        + featureType.getTypeName()
                                        + "' with a new native schema.");

                        dataStore.removeSchema(featureType.getTypeName());
                        dataStore.createSchema(featureType);

                        createdNativeTypeName = featureType.getTypeName();
                    }
                } else {
                    // replace is being used here to update an existing layer
                    // with a new native name
                    LOGGER.info(
                            "Replace layer '"
                                    + task.getLayer().getResource().getName()
                                    + "' "
                                    + "used with new native schema '"
                                    + featureType.getTypeName()
                                    + "'");
                    dataStore.createSchema(featureType);

                    createdNativeTypeName = featureType.getTypeName();
                }
            }

            // Move features
            if (format instanceof DataStoreFormat) {
                error =
                        copyFromFeatureSource(
                                data,
                                task,
                                (DataStoreFormat) format,
                                dataStore,
                                transaction,
                                createdNativeTypeName,
                                featureType.getTypeName(),
                                featureDataConverter,
                                tx);
            } else {
                reader = format.read(data, task);
                error =
                        copyFromFeatureReader(
                                reader,
                                task,
                                format,
                                dataStore,
                                transaction,
                                createdNativeTypeName,
                                featureType.getTypeName(),
                                featureDataConverter,
                                tx);
            }

        } finally {
            try {
                if (reader != null) {
                    format.dispose(reader, task);
                    // @hack catch _all_ Exceptions here - occassionally closing a shapefile
                    // seems to result in an IllegalArgumentException related to not
                    // holding the lock...
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing reader", e);
            }

            transaction.commit();

            // try to cleanup, but if an error occurs here and one hasn't already been set, set the
            // error
            try {
                transaction.close();
            } catch (Exception e) {
                if (error != null) {
                    error = e;
                }
                LOGGER.log(Level.WARNING, "Error closing transaction", e);
            }
        }
        // finally, throw any error
        if (error != null) {
            throw error;
        }
    }

    /**
     * This is a quick way to double check if the two schemas have the same typeName, and and
     * attribute names / bindings.
     *
     * @param type1
     * @param type2
     * @return true if the two schemas are approximately equal (ignore user maps and namespace)
     */
    boolean schemaEqualsIgnoreNamespace(SimpleFeatureType type1, SimpleFeatureType type2) {
        String spec1 = DataUtilities.encodeType(type1);
        String spec2 = DataUtilities.encodeType(type2);

        return spec1.equals(spec2);
    }

    void loadIntoCoverageStore(
            ImportTask task, CoverageStoreInfo store, GridFormat format, RasterTransformChain tx)
            throws IOException {
        @SuppressWarnings("PMD.CloseResource") // conditionally created, then closed

        // see if the store exposes a structured grid coverage reader
        // this is a ResourcePool reader, we should not close it
        GridCoverageReader reader = store.getGridCoverageReader(null, null);
        boolean isStructured = reader instanceof StructuredGridCoverage2DReader;

        if (task.getUpdateMode() == UpdateMode.REPLACE && !isStructured) {
            // Replacing the coverage
            replaceCoverage(format, reader, store, task);
        } else {
            harvestCoverage(task, store, reader, isStructured);
        }
    }

    private void harvestCoverage(
            ImportTask task,
            CoverageStoreInfo store,
            GridCoverageReader reader,
            boolean isStructured)
            throws IOException {
        final String errorMessage =
                "Indirect raster import can only work against a structured grid coverage store (e.g., mosaic), this one is not: ";
        if (!(isStructured)) {
            throw new IllegalArgumentException(errorMessage + store);
        }
        StructuredGridCoverage2DReader sr = (StructuredGridCoverage2DReader) reader;
        ImportData data = task.getData();
        harvestImportData(sr, data);

        // check we have a target resource, if not, create it
        if (task.getUpdateMode() == UpdateMode.CREATE) {
            if (task.getLayer() != null && task.getLayer().getId() == null) {
                addToCatalog(task);
            }
        }
    }

    private void replaceCoverage(
            GridFormat format, GridCoverageReader reader, CoverageStoreInfo store, ImportTask task)
            throws IOException {
        // Get the imported data
        ImportData data = task.getData();
        if (!(data instanceof SpatialFile)) {
            throw new IllegalArgumentException(
                    "Only FileData are supported for replace. This is " + data);
        }

        SpatialFile fileData = (SpatialFile) data;
        File file = fileData.getFile();
        Directory directory = Directory.createNew(getUploadRoot());
        try (FileInputStream fis = new FileInputStream(file)) {
            String name = file.getName();
            directory.accept(name, fis);
            file = directory.child(name);
        }

        String newFile = file.toURI().toURL().getFile();

        // When replacing, we need to make sure that the provided layer and store matches what we
        // have in the catalog.
        CoverageStoreInfo originalStore =
                catalog.getCoverageStoreByName(store.getWorkspace().getName(), store.getName());
        LayerInfo inputLayer = task.getLayer();
        LayerInfo originalLayer = checkResourceExists(store, originalStore, inputLayer);
        CoverageInfo originalCoverage = (CoverageInfo) originalLayer.getResource();

        // Marking original names for restore
        String name = originalCoverage.getName();
        String nativeName = originalCoverage.getNativeName();

        // Marking previous file for eventual delete
        GridCoverage2DReader oldReader = ((GridCoverage2DReader) reader);
        List<File> oldFiles = getFiles(oldReader);

        // Setting up the new coverage
        GridCoverage2DReader newReader = (format.gridFormat()).getReader(file, null);
        store.setURL(newFile);
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(store);
        builder.setWorkspace(store.getWorkspace());
        CoverageInfo coverage;
        try {
            coverage = builder.buildCoverage(newReader, Collections.emptyMap());
        } catch (Exception e) {
            throw new IOException(
                    "Exception occurred while configuring the coverage during the replace: ", e);
        }

        // Updating the old coverage in the catalog with the properties of the imported one
        OwsUtils.copy(coverage, originalCoverage, CoverageInfo.class);
        List<File> newFiles = getFiles(newReader);

        // Restore original names
        originalCoverage.setName(name);
        originalCoverage.setNativeName(nativeName);
        catalog.save(originalCoverage);
        catalog.save(store);

        // Clean up the oldFiles when not overwriting it
        // Removing oldFiles missing from the new list
        oldFiles.removeAll(newFiles);
        for (File oldFile : oldFiles) {
            FileUtils.deleteQuietly(oldFile);
        }
    }

    private List<File> getFiles(GridCoverage2DReader reader) throws IOException {
        ServiceInfo info = reader.getInfo();
        List<File> files = new ArrayList<>();
        if (info instanceof FileServiceInfo) {
            FileServiceInfo filesInfo = (FileServiceInfo) info;
            // We are dealing with singleFile store so we can store the files in a list
            try (CloseableIterator<FileGroupProvider.FileGroup> fileIterator =
                    filesInfo.getFiles(null)) {
                FileGroupProvider.FileGroup fileGroup = fileIterator.next();
                File mainFile = fileGroup.getMainFile();
                files.add(mainFile);
                files.addAll(fileGroup.getSupportFiles());
            }
        } else {
            URL url = info.getSource().toURL();
            if ("file".equalsIgnoreCase(url.getProtocol())) {
                files.add(new File(url.getFile()));
            }
        }
        return files;
    }

    /**
     * Lookup origional layer in the cartalog, will raise an {@link IllegalArgumentException} if
     * resource does not exist.
     *
     * @param store
     * @param originalStore
     * @param inputLayer
     * @return origional layer found in catalog
     * @throws IllegalArgumentException
     */
    private LayerInfo checkResourceExists(
            CoverageStoreInfo store, CoverageStoreInfo originalStore, LayerInfo inputLayer)
            throws IllegalArgumentException {
        String errorMessage =
                "UpdateMode:REPLACE only works against existing resources in the catalog.";
        LayerInfo originalLayer = catalog.getLayerByName(inputLayer.getName());
        if (originalLayer == null) {
            // the layer has been created before the REPLACE updatemode has been set,
            // so it might have a different native name assigned.
            // Let's try with the filename which is used by default to set the layer name
            String url = store.getURL();
            String baseName = FilenameUtils.getBaseName(url);
            originalLayer = catalog.getLayerByName(baseName);
        }
        if (originalStore == null) {
            errorMessage += "\nStore: " + store.getName() + " doesn't exist in the catalog";
        }
        if (originalLayer == null) {
            errorMessage += "\nLayer: " + inputLayer.getName() + " doesn't exist in the catalog";
        }
        if (originalLayer == null || originalStore == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return originalLayer;
    }

    /**
     * Copy content from import data, used to feature source.
     *
     * @param data Import data used to obtain feature source
     * @param task
     * @param format
     * @param dataStoreDestination
     * @param transaction
     * @param createdFeatureTypeName Created table name, or null if re-loading into an existing
     *     table.
     * @param nativeFeatureTypeName Native feature type name (example a table)
     * @param featureDataConverter
     * @param tx
     * @return {@code null} if successful, or error condition throwable
     */
    @SuppressWarnings("unchecked") // vague about feature types
    private Throwable copyFromFeatureSource(
            ImportData data,
            ImportTask task,
            DataStoreFormat format,
            DataStore dataStoreDestination,
            Transaction transaction,
            String createdFeatureTypeName,
            String nativeFeatureTypeName,
            FeatureDataConverter featureDataConverter,
            VectorTransformChain tx) {
        Throwable error = null;
        ProgressMonitor monitor = task.progress();
        try {
            task.clearMessages();

            task.setTotalToProcess(format.getFeatureCount(task.getData(), task));
            LOGGER.fine("beginning import - high level api");

            FeatureSource fs = format.getFeatureSource(data, task);

            FeatureStore featureStore =
                    (FeatureStore) dataStoreDestination.getFeatureSource(nativeFeatureTypeName);
            featureStore.setTransaction(transaction);

            FeatureCollection fc =
                    new ImportTransformFeatureCollection<>(
                            fs.getFeatures(),
                            featureDataConverter,
                            featureStore.getSchema(),
                            tx,
                            task,
                            dataStoreDestination);
            featureStore.addFeatures(fc);

        } catch (Throwable e) {
            error = e;
            LOGGER.fine("Load from source in to target error:" + error);
        }

        if (error != null || monitor.isCanceled()) {
            // all sub exceptions in this catch block should be logged, not thrown
            // as the triggering exception will be thrown
            if (monitor.isCanceled()) {
                LOGGER.log(
                        Level.INFO,
                        "Import from source canceled, data insert into '"
                                + nativeFeatureTypeName
                                + "' rolling back");
            } else if (error != null) {
                LOGGER.log(
                        Level.INFO,
                        "Import from source error, data insert into '"
                                + nativeFeatureTypeName
                                + "' rolling back due to error:"
                                + error);
            }
            // cancel or failure, rollback transaction
            try {
                transaction.rollback();
            } catch (Exception e1) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to load data into '"
                                + nativeFeatureTypeName
                                + "', rolling back data insert failed:"
                                + e1,
                        e1);
            }

            // drop the type that was created as well, only if it was created to start with
            if (createdFeatureTypeName != null) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to load data, removing created schema '"
                                + createdFeatureTypeName
                                + "'");
                try {
                    dropSchema(dataStoreDestination, createdFeatureTypeName);
                } catch (Exception e1) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error dropping schema '" + createdFeatureTypeName + "' after rollback",
                            e1);
                }
            }
        }
        return error;
    }

    /**
     * Copy content from single use FeatureReader, used to obtain content from csv and geojsoon
     * files where no general purpose datastore is available.
     *
     * @param reader
     * @param task
     * @param format
     * @param dataStoreDestination
     * @param transaction
     * @param createdFeatureTypeName Created table name, or null if re-loading into an existing
     *     table.
     * @param nativeFeatureTypeName Native feature type name (example a table)
     * @param featureDataConverter
     * @param tx
     * @return {@code null} if successful, or error condition throwable
     */
    Throwable copyFromFeatureReader(
            FeatureReader reader,
            ImportTask task,
            VectorFormat format,
            DataStore dataStoreDestination,
            Transaction transaction,
            String createdFeatureTypeName,
            String nativeFeatureTypeName,
            FeatureDataConverter featureDataConverter,
            VectorTransformChain tx) {
        LOGGER.fine("begining import - lowlevel api");

        Throwable error = null;
        ProgressMonitor monitor = task.progress();

        // @todo need better way to communicate to client
        int skipped = 0;
        int cnt = 0;
        // metrics
        long startTime = System.currentTimeMillis();

        try {
            task.clearMessages();
            int numberOfFeatures = format.getFeatureCount(task.getData(), task);
            task.setTotalToProcess(numberOfFeatures);
        } catch (IOException noCountAvailable) {
            LOGGER.log(
                    Level.FINE,
                    "Unable to determine number of features to import: " + noCountAvailable,
                    noCountAvailable);
            error = noCountAvailable;
        }

        if (error == null) {
            try (FeatureWriter writer =
                    dataStoreDestination.getFeatureWriterAppend(
                            nativeFeatureTypeName, transaction)) {

                while (reader.hasNext()) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    SimpleFeature feature = (SimpleFeature) reader.next();
                    SimpleFeature next = (SimpleFeature) writer.next();

                    // (JD) TODO: some formats will rearrange the geometry type (like shapefile)
                    // which
                    // makes the geometry the first attribute regardless, so blindly copying over
                    // attributes won't work unless the source type also has the geometry as the
                    // first attribute in the schema
                    featureDataConverter.convert(feature, next);

                    // @hack #45678 - mask empty geometry or postgis will complain
                    Geometry geom = (Geometry) next.getDefaultGeometry();
                    if (geom != null && geom.isEmpty()) {
                        next.setDefaultGeometry(null);
                    }

                    // apply the feature transform
                    next = tx.inline(task, dataStoreDestination, feature, next);

                    if (next == null) {
                        skipped++;
                    } else {
                        writer.write();
                    }
                    task.setNumberProcessed(++cnt);
                }
                if (skipped > 0) {
                    task.addMessage(Level.WARNING, skipped + " features were skipped.");
                }
                LOGGER.info(
                        "Load from reader in to target took "
                                + (System.currentTimeMillis() - startTime));
            } catch (Throwable e) {
                error = e;
                LOGGER.fine("Load from reader in to target error:" + error);
            }
        }
        // no finally block, there is too much to do
        if (error != null || monitor.isCanceled()) {
            // all sub exceptions in this catch block should be logged, not thrown
            // as the triggering exception will be thrown
            if (monitor.isCanceled()) {
                LOGGER.log(
                        Level.INFO,
                        "Import from reader canceled, data insert into '"
                                + nativeFeatureTypeName
                                + "' rolling back");
            } else if (error != null) {
                LOGGER.log(
                        Level.INFO,
                        "Import from reader error, data insert into '"
                                + nativeFeatureTypeName
                                + "' rolling back due to error:"
                                + error);
            }
            // cancel or failure, rollback transaction
            try {
                transaction.rollback();
            } catch (Exception e1) {
                LOGGER.log(Level.WARNING, "Error rolling back transaction", e1);
            }

            // drop the type that was created as well, only if it was created to start with
            if (createdFeatureTypeName != null) {
                try {
                    LOGGER.log(
                            Level.WARNING,
                            "Unable to load data, removing created schema '"
                                    + createdFeatureTypeName
                                    + "'");
                    dropSchema(dataStoreDestination, createdFeatureTypeName);
                } catch (Exception e1) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error dropping schema '" + createdFeatureTypeName + "' after rollback",
                            e1);
                }
            }
        }
        return error;
    }

    StoreInfo lookupDefaultStore() {
        WorkspaceInfo ws = catalog.getDefaultWorkspace();
        if (ws == null) {
            return null;
        }

        return catalog.getDefaultDataStore(ws);
    }

    void addToCatalog(ImportTask task) throws IOException {
        LayerInfo layer = task.getLayer();
        ResourceInfo resource = layer.getResource();
        resource.setStore(task.getStore());

        // add the resource
        String name = findUniqueResourceName(resource);
        resource.setName(name);

        // JD: not setting a native name, it should actually already be set by this point and we
        // don't want to blindly set it to the same name as the resource name, which might have
        // changed to deal with name clashes
        // resource.setNativeName(name);
        resource.setEnabled(true);
        LOGGER.log(
                Level.FINE,
                "Creating target resource {0} for task {1}",
                new Object[] {resource, task.getId()});
        catalog.add(resource);

        // add the layer (and style)
        if (layer.getDefaultStyle().getId() == null) {
            LOGGER.log(
                    Level.FINE,
                    "Creating default style {0} for task {0}",
                    new Object[] {layer.getDefaultStyle(), task.getId()});
            catalog.add(layer.getDefaultStyle());
        }

        layer.setEnabled(true);
        LOGGER.log(
                Level.FINE,
                "Creating target layer  {0} for task {1}",
                new Object[] {layer, task.getId()});
        catalog.add(layer);
    }

    String findUniqueStoreName(StoreInfo store) {
        WorkspaceInfo workspace = store.getWorkspace();

        // TODO: put an upper limit on how many times to try
        String name = store.getName();
        if (catalog.getStoreByName(workspace, store.getName(), StoreInfo.class) != null) {
            int i = 0;
            name += i;
            while (catalog.getStoreByName(workspace, name, StoreInfo.class) != null) {
                name = name.replaceAll(i + "$", String.valueOf(i + 1));
                i++;
            }
        }

        return name;
    }

    String findUniqueResourceName(ResourceInfo resource) throws IOException {

        // TODO: put an upper limit on how many times to try
        StoreInfo store = resource.getStore();
        NamespaceInfo ns = catalog.getNamespaceByPrefix(store.getWorkspace().getName());

        String name = resource.getName();

        // make sure the name conforms to a legal layer name
        if (!Character.isLetter(name.charAt(0))) {
            name = "a_" + name;
        }

        // strip out any non-word characters
        name = name.replaceAll("\\W", "_");

        if (catalog.getResourceByName(ns, name, ResourceInfo.class) != null) {
            int i = 0;
            name += i;
            while (catalog.getResourceByName(ns, name, ResourceInfo.class) != null) {
                name = name.replaceAll(i + "$", String.valueOf(i + 1));
                i++;
            }
        }

        return name;
    }

    String findUniqueNativeFeatureTypeName(FeatureType featureType, DataStoreInfo store)
            throws IOException {
        return findUniqueNativeFeatureTypeName(featureType.getName().getLocalPart(), store);
    }

    private String findUniqueNativeFeatureTypeName(String name, DataStoreInfo store)
            throws IOException {
        DataStore dataStore = (DataStore) store.getDataStore(null);

        // hack for oracle, all names must be upper case
        // TODO: abstract this into FeatureConverter
        if (isOracleDataStore(dataStore)) {
            name = name.toUpperCase();
        }

        // TODO: put an upper limit on how many times to try
        List<String> names = Arrays.asList(dataStore.getTypeNames());
        if (names.contains(name)) {
            int i = 0;
            name += i;
            while (names.contains(name)) {
                name = name.replaceAll(i + "$", String.valueOf(i + 1));
                i++;
            }
        }

        return name;
    }

    boolean isShapefileDataStore(DataStore dataStore) {
        return dataStore instanceof ShapefileDataStore || dataStore instanceof DirectoryDataStore;
    }

    boolean isOracleDataStore(DataStore dataStore) {
        return dataStore instanceof JDBCDataStore
                && "org.geotools.data.oracle.OracleDialect"
                        .equals(((JDBCDataStore) dataStore).getSQLDialect().getClass().getName());
    }

    boolean isPostGISDataStore(DataStore dataStore) {
        return dataStore instanceof JDBCDataStore
                && ((JDBCDataStore) dataStore)
                        .getSQLDialect()
                        .getClass()
                        .getName()
                        .startsWith("org.geotools.data.postgis");
    }

    /*
     * Computes the lat/lon bounding box from the native bounding box and srs, optionally overriding
     * the value already set.
     *
     * If the does not contain a geometry (such as a csv file processing lat/long into a point)
     * this activity may not have required information to work with.
     */
    boolean computeLatLonBoundingBox(ImportTask task, boolean force) throws Exception {
        ResourceInfo r = task.getLayer().getResource();
        if (force || r.getLatLonBoundingBox() == null && r.getNativeBoundingBox() != null) {
            CoordinateReferenceSystem nativeCRS = CRS.decode(r.getSRS());
            ReferencedEnvelope nativeBbox =
                    new ReferencedEnvelope(r.getNativeBoundingBox(), nativeCRS);

            if (nativeBbox.isEmpty()) {
                LOGGER.fine("Import '" + r.getName() + "' native bounding box is empty");
                r.setLatLonBoundingBox(ReferencedEnvelope.create(CRS.decode("EPSG:4326")));
                return true;
            } else {
                r.setLatLonBoundingBox(nativeBbox.transform(CRS.decode("EPSG:4326"), true));
                return true;
            }
        }
        return false;
    }

    // file location methods
    public File getImportRoot() {
        try {
            return catalog.getResourceLoader().findOrCreateDirectory("imports");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File getUploadRoot() {
        String value = configuration.getUploadRoot();

        try {
            if (value != null) {
                Resource uploadsRoot = Resources.fromPath(value);
                return Resources.directory(uploadsRoot, !Resources.exists(uploadsRoot));
            }

            return catalog.getResourceLoader().findOrCreateDirectory("uploads");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        asynchronousJobs.shutdown();
        contextStore.destroy();
    }

    public void delete(ImportContext importContext) throws IOException {
        delete(importContext, false);
    }

    public void delete(ImportContext importContext, boolean purge) throws IOException {
        if (purge) {
            importContext.delete();
        }

        contextStore.remove(importContext);
    }

    private void dropSchema(DataStore ds, String featureTypeName) throws Exception {
        // @todo this needs implementation in geotools
        SimpleFeatureType schema = ds.getSchema(featureTypeName);
        if (schema != null) {
            try {
                ds.removeSchema(featureTypeName);
            } catch (Exception e) {
                LOGGER.warning(
                        "Unable to dropSchema "
                                + featureTypeName
                                + " from datastore "
                                + ds.getClass());
            }
        } else {
            LOGGER.warning(
                    "Unable to dropSchema "
                            + featureTypeName
                            + " as it does not appear to exist in dataStore");
        }
    }

    public XStreamPersister createXStreamPersisterXML() {
        return initXStreamPersister(new XStreamPersisterFactory().createXMLPersister());
    }

    public XStreamPersister createXStreamPersisterJSON() {
        return initXStreamPersister(new XStreamPersisterFactory().createJSONPersister());
    }

    public XStreamPersister initXStreamPersister(XStreamPersister xp) {
        xp.setCatalog(catalog);
        // xp.setReferenceByName(true);

        XStream xs = xp.getXStream();

        // ImportContext
        xs.alias("import", ImportContext.class);

        // ImportTask
        xs.alias("task", ImportTask.class);
        xs.omitField(ImportTask.class, "context");

        // ImportItem
        // xs.alias("item", ImportItem.class);
        // xs.omitField(ImportItem.class, "task");

        // DataFormat
        xs.alias("dataStoreFormat", DataStoreFormat.class);

        // ImportData
        xs.alias("spatialFile", SpatialFile.class);
        xs.alias("database", org.geoserver.importer.Database.class);
        xs.alias("table", Table.class);
        xs.omitField(Table.class, "db");

        xs.alias("vectorTransformChain", VectorTransformChain.class);
        xs.registerLocalConverter(ReprojectTransform.class, "source", new CRSConverter());
        xs.registerLocalConverter(ReprojectTransform.class, "target", new CRSConverter());

        xs.registerLocalConverter(ReferencedEnvelope.class, "crs", new CRSConverter());
        xs.registerLocalConverter(GeneralBounds.class, "crs", new CRSConverter());

        GeoServerSecurityManager securityManager =
                GeoServerExtensions.bean(GeoServerSecurityManager.class);
        xs.registerLocalConverter(
                RemoteData.class, "password", new EncryptedFieldConverter(securityManager));

        // security
        xs.allowTypes(new Class[] {ImportContext.class, ImportTask.class, File.class});
        xs.allowTypeHierarchy(TransformChain.class);
        xs.allowTypeHierarchy(DataFormat.class);
        xs.allowTypeHierarchy(ImportData.class);
        xs.allowTypeHierarchy(ImportTransform.class);
        xs.allowTypeHierarchy(Exception.class);
        xs.allowTypeHierarchy(StackTraceElement.class);
        xs.allowTypeHierarchy(Class.class);

        // normal serialization handles only references in the catalog, the importer
        // is playing with objects that are not persisted in the catalog yet instead
        xs.registerLocalConverter(
                LayerInfoImpl.class,
                "resource",
                new ReflectionConverter(xs.getMapper(), xs.getReflectionProvider()));
        xs.registerLocalConverter(
                LayerInfoImpl.class,
                "defaultStyle",
                new ReflectionConverter(xs.getMapper(), xs.getReflectionProvider()));

        return xp;
    }

    /**
     * Creates a style for the layer being imported from a resource that was included in the
     * directory or archive that the data is being imported from.
     */
    StyleInfo createStyleFromFile(File styleFile, ImportTask task) {
        String ext = FilenameUtils.getExtension(styleFile.getName());
        if (ext != null) {
            StyleHandler styleHandler = Styles.handler(ext);
            if (styleHandler != null) {
                try {
                    StyledLayerDescriptor sld =
                            styleHandler.parse(
                                    styleFile,
                                    null,
                                    null,
                                    new EntityResolverProvider(getGeoServer()).getEntityResolver());

                    Style style = Styles.style(sld);
                    if (style != null) {
                        StyleInfo info = catalog.getFactory().createStyle();

                        String styleName =
                                styleGen.generateUniqueStyleName(task.getLayer().getResource());
                        info.setName(styleName);

                        info.setFilename(styleName + "." + ext);
                        info.setFormat(styleHandler.getFormat());
                        info.setFormatVersion(styleHandler.version(styleFile));
                        info.setWorkspace(task.getStore().getWorkspace());

                        try (InputStream in = new FileInputStream(styleFile)) {
                            catalog.getResourcePool().writeStyle(info, in);
                        }
                        return info;
                    } else {
                        LOGGER.warning("Style file contained no styling: " + styleFile.getPath());
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error parsing style: " + styleFile.getPath(), e);
                }
            } else {
                LOGGER.warning("Unable to find style handler for file extension: " + ext);
            }
        }

        return null;
    }

    /** Returns a copy of the importer configuration */
    public ImporterInfo getConfiguration() {
        return new ImporterInfoImpl(configuration);
    }

    /** Sets the importer configuration, and saves it on disk */
    public void setConfiguration(ImporterInfo configuration) throws IOException {
        configDAO.write(configuration, configFile);
        reloadConfiguration();
    }
}

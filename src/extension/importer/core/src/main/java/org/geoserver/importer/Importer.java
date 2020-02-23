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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.HarvestedSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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

    ConcurrentHashMap<Long, ImportTask> currentlyProcessing =
            new ConcurrentHashMap<Long, ImportTask>();

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
            configDAO.read(configFile, configuration);
            asynchronousJobs.setMaximumPoolSize(configuration.getMaxAsynchronousImports());
            synchronousJobs.setMaximumPoolSize(configuration.getMaxSynchronousImports());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to update importer configuration");
        }
    }

    /** Returns the style generator. */
    public StyleGenerator getStyleGenerator() {
        return styleGen;
    }

    public StyleHandler getStyleHandler() {
        return styleHandler;
    }

    public void setStyleHandler(StyleHandler handler) {
        styleHandler = handler;
    }

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

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // load the context store here to avoid circular dependency on creation of the importer
        if (event instanceof ContextLoadedEvent) {
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
            TreeSet sorted =
                    new TreeSet<ImportContext>(
                            new Comparator<ImportContext>() {
                                @Override
                                public int compare(ImportContext o1, ImportContext o2) {
                                    Date d1 = o1.getUpdated();
                                    Date d2 = o2.getUpdated();
                                    return -1 * d1.compareTo(d2);
                                }
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
        // JD: don't think we really need to maintain these, and they aren't persisted
        // else {
        //    context.setState(ImportContext.State.CANCELLED);
        // }
        return context;
    }

    public Long createContextAsync(
            final ImportData data, final WorkspaceInfo targetWorkspace, final StoreInfo targetStore)
            throws IOException {
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
        List<ImportTask> tasks = new ArrayList<ImportTask>();

        // flatten out the directory into itself and all sub directories and process in order
        for (Directory dir : data.flatten()) {
            // ignore empty directories
            if (dir.getFiles().isEmpty()) continue;

            // group the contents of the directory by format
            Map<DataFormat, List<FileData>> map = new HashMap<DataFormat, List<FileData>>();
            for (FileData f : dir.getFiles()) {
                DataFormat format = f.getFormat();
                List<FileData> files = map.get(format);
                if (files == null) {
                    files = new ArrayList<FileData>();
                    map.put(format, files);
                }
                files.add(f);
            }

            // handle case of importing a single file that we don't know the format of, in this
            // case rather than ignore it we wnat to rpocess it and ssets its state to "NO_FORMAT"
            boolean skipNoFormat = !(map.size() == 1 && map.containsKey(null));

            // if no target store specified group the directory into pieces that can be
            // processed as a single task
            StoreInfo targetStore = context.getTargetStore();
            if (targetStore == null) {

                // create a task for each "format" if that format can handle a directory
                for (DataFormat format : new ArrayList<DataFormat>(map.keySet())) {
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

        List<ImportTask> tasks = new ArrayList<ImportTask>();

        boolean direct = false;

        StoreInfo targetStore = context.getTargetStore();
        if (targetStore == null) {
            // direct import, use the format to create a store
            direct = true;

            if (format != null) {
                targetStore = format.createStore(data, context.getTargetWorkspace(), catalog);
            }

            if (targetStore == null) {
                // format unable to create store, switch to indirect import and use
                // default store from catalog
                targetStore = lookupDefaultStore();

                direct = targetStore == null;
            }
        }

        // are we setting up an harvest against an existing store, and the input is also
        // multi-coverage?
        if (targetStore instanceof CoverageStoreInfo
                && targetStore.getId() != null
                && isMultiCoverageInput(format, data)) {
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
            return Arrays.asList(task);
        }

        if (format != null) {
            // create the set of tasks by having the format list the available items from the input
            // data
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
                tasks.add(t);
            }
        } else if (!skipNoFormat) {
            ImportTask t = new ImportTask(data);
            t.setDirect(direct);
            t.setStore(targetStore);
            prep(t);
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
                // them
                // created in some other way
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

                // first check the case of a style file being uploaded via zip along with rest of
                // files
                if (task.getData() instanceof SpatialFile) {
                    SpatialFile file = (SpatialFile) task.getData();
                    if (file.getStyleFile() != null) {
                        style = createStyleFromFile(file.getStyleFile(), task);
                    }
                }

                if (style == null) {
                    if (r instanceof FeatureTypeInfo) {
                        // since this resource is still detached from the catalog we can't call
                        // through to get it's underlying resource, so we depend on the "native"
                        // type provided from the format
                        FeatureType featureType =
                                (FeatureType) task.getMetadata().get(FeatureType.class);
                        if (featureType != null) {
                            style =
                                    styleGen.createStyle(
                                            styleHandler, (FeatureTypeInfo) r, featureType);
                        } else {
                            throw new RuntimeException("Unable to compute style");
                        }

                    } else if (r instanceof CoverageInfo) {
                        style = styleGen.createStyle(styleHandler, (CoverageInfo) r);
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
            task.setState(ImportTask.State.NO_BOUNDS);
            return false;
        }

        task.setState(ImportTask.State.READY);
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

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Running import " + context.getId());
        }

        for (ImportTask task : context.getTasks()) {
            if (!filter.include(task)) {
                continue;
            }
            if (!task.readyForImport()) {
                continue;
            }

            if (context.progress().isCanceled()) {
                break;
            }
            run(task);
        }

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
                        ioe.printStackTrace();
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
        task.setState(ImportTask.State.RUNNING);

        if (task.isDirect()) {
            // direct import, simply add configured store and layers to catalog
            doDirectImport(task);
        } else {
            // indirect import, read data from the source and into the target store
            doIndirectImport(task);
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
                        runInternal(context, filter, monitor);
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

    public Task<ImportContext> getTask(Long job) {
        return (Task<ImportContext>) asynchronousJobs.getTask(job);
    }

    public List<Task<ImportContext>> getTasks() {
        return (List) asynchronousJobs.getTasks();
    }

    /*
     * an import that involves consuming a data source directly
     */
    void doDirectImport(ImportTask task) throws IOException {
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
            catalog.add(task.getStore());
        }

        task.setState(ImportTask.State.RUNNING);

        try {
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
                calculateBounds(resource);
            }

            // apply post transform
            if (!doPostTransform(task, task.getData(), tx)) {
                return;
            }

            task.setState(ImportTask.State.COMPLETE);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Task failed during import: " + task, e);
            task.setState(ImportTask.State.ERROR);
            task.setError(e);
        }
    }

    /*
     * an import that involves reading from the datastore and writing into a specified target store
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
                    if (task.getUpdateMode() == UpdateMode.CREATE) {
                        addToCatalog(task);
                    }
                    FeatureTypeInfo resource =
                            getCatalog()
                                    .getResourceByName(
                                            featureType.getQualifiedName(), FeatureTypeInfo.class);
                    calculateBounds(resource);
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
            // see if the store exposes a structured grid coverage reader
            StoreInfo store = task.getStore();
            final String errorMessage =
                    "Indirect raster import can only work against a structured grid coverage store (e.g., mosaic), this one is not: ";
            if (!(store instanceof CoverageStoreInfo)) {
                throw new IllegalArgumentException(errorMessage + store);
            }

            // this is a ResourcePool reader, we should not close it
            CoverageStoreInfo cs = (CoverageStoreInfo) store;
            GridCoverageReader reader = cs.getGridCoverageReader(null, null);

            if (!(reader instanceof StructuredGridCoverage2DReader)) {
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
        if (resource.getNativeBoundingBox() == null
                || resource.getNativeBoundingBox().isEmpty()
                || Boolean.TRUE.equals(resource.getMetadata().get("recalculate-bounds"))
                || "true".equals(resource.getMetadata().get("recalculate-bounds"))) {
            // force computation
            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            ReferencedEnvelope nativeBounds = cb.getNativeBounds(resource);
            resource.setNativeBoundingBox(nativeBounds);
            resource.setLatLonBoundingBox(cb.getLatLonBounds(nativeBounds, resource.getCRS()));
            getCatalog().save(resource);

            // Do not re-calculate on subsequent imports
            if (resource.getMetadata().get("recalculate-bounds") != null) {
                resource.getMetadata().remove("recalculate-bounds");
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
        FeatureReader reader = null;

        // using this exception to throw at the end
        Throwable error = null;
        Transaction transaction = new DefaultTransaction();
        try {

            SimpleFeatureType featureType = task.getFeatureType();
            task.setOriginalLayerName(featureType.getTypeName());
            String nativeName = task.getLayer().getResource().getNativeName();
            if (!featureType.getTypeName().equals(nativeName)) {
                SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
                tb.init(featureType);
                tb.setName(nativeName);
                featureType = tb.buildFeatureType();
            }

            final String featureTypeName = featureType.getName().getLocalPart();

            DataStore dataStore = (DataStore) store.getDataStore(null);
            FeatureDataConverter featureDataConverter = FeatureDataConverter.DEFAULT;
            if (isShapefileDataStore(dataStore)) {
                featureDataConverter = FeatureDataConverter.TO_SHAPEFILE;
            } else if (isOracleDataStore(dataStore)) {
                featureDataConverter = FeatureDataConverter.TO_ORACLE;
            } else if (isPostGISDataStore(dataStore)) {
                featureDataConverter = FeatureDataConverter.TO_POSTGIS;
            }

            featureType = featureDataConverter.convertType(featureType, format, data, task);
            UpdateMode updateMode = task.getUpdateMode();
            final String uniquifiedFeatureTypeName;
            if (updateMode == UpdateMode.CREATE) {
                // find a unique type name in the target store
                uniquifiedFeatureTypeName = findUniqueNativeFeatureTypeName(featureType, store);

                if (!uniquifiedFeatureTypeName.equals(featureTypeName)) {
                    // update the metadata
                    task.getLayer().getResource().setName(uniquifiedFeatureTypeName);
                    task.getLayer().getResource().setNativeName(uniquifiedFeatureTypeName);

                    // retype
                    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                    typeBuilder.setName(uniquifiedFeatureTypeName);
                    typeBuilder.addAll(featureType.getAttributeDescriptors());
                    featureType = typeBuilder.buildFeatureType();
                }

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

                // apply the feature type transform
                featureType = tx.inline(task, dataStore, featureType);

                dataStore.createSchema(featureType);
            } else {
                // @todo what to do if featureType transform is present?

                // @todo implement me - need to specify attribute used for id
                if (updateMode == UpdateMode.UPDATE) {
                    throw new UnsupportedOperationException(
                            "updateMode UPDATE is not supported yet");
                }
                uniquifiedFeatureTypeName = featureTypeName;
            }

            if (updateMode == UpdateMode.REPLACE) {

                FeatureStore fs = (FeatureStore) dataStore.getFeatureSource(featureTypeName);
                fs.setTransaction(transaction);
                fs.removeFeatures(Filter.INCLUDE);
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
                                featureTypeName,
                                uniquifiedFeatureTypeName,
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
                                featureTypeName,
                                uniquifiedFeatureTypeName,
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

    private Throwable copyFromFeatureSource(
            ImportData data,
            ImportTask task,
            DataStoreFormat format,
            DataStore dataStoreDestination,
            Transaction transaction,
            String featureTypeName,
            String uniquifiedFeatureTypeName,
            FeatureDataConverter featureDataConverter,
            VectorTransformChain tx)
            throws IOException {
        Throwable error = null;
        ProgressMonitor monitor = task.progress();
        try {
            task.clearMessages();

            task.setTotalToProcess(format.getFeatureCount(task.getData(), task));
            LOGGER.fine("begining import - highlevel api");

            FeatureSource fs = format.getFeatureSource(data, task);
            FeatureCollection fc = fs.getFeatures();

            FeatureStore featureStore =
                    (FeatureStore) dataStoreDestination.getFeatureSource(uniquifiedFeatureTypeName);
            featureStore.setTransaction(transaction);

            fc =
                    new ImportTransformFeatureCollection(
                            fc,
                            featureDataConverter,
                            featureStore.getSchema(),
                            tx,
                            task,
                            dataStoreDestination);

            featureStore.addFeatures(fc);

        } catch (Throwable e) {
            error = e;
        }

        if (error != null || monitor.isCanceled()) {
            // all sub exceptions in this catch block should be logged, not thrown
            // as the triggering exception will be thrown

            // failure, rollback transaction
            try {
                transaction.rollback();
            } catch (Exception e1) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to load data into "
                                + uniquifiedFeatureTypeName
                                + ", rolling back data insert:"
                                + e1,
                        e1);
            }

            // attempt to drop the type that was created as well
            try {
                dropSchema(dataStoreDestination, featureTypeName);
            } catch (Exception e1) {
                LOGGER.log(Level.WARNING, "Error dropping schema in rollback", e1);
            }
        }

        return error;
    }

    Throwable copyFromFeatureReader(
            FeatureReader reader,
            ImportTask task,
            VectorFormat format,
            DataStore dataStoreDestination,
            Transaction transaction,
            String featureTypeName,
            String uniquifiedFeatureTypeName,
            FeatureDataConverter featureDataConverter,
            VectorTransformChain tx)
            throws IOException {
        FeatureWriter writer = null;
        Throwable error = null;
        ProgressMonitor monitor = task.progress();

        // @todo need better way to communicate to client
        int skipped = 0;
        int cnt = 0;
        // metrics
        long startTime = System.currentTimeMillis();
        task.clearMessages();

        task.setTotalToProcess(format.getFeatureCount(task.getData(), task));

        LOGGER.fine("begining import - lowlevel api");
        try {
            writer =
                    dataStoreDestination.getFeatureWriterAppend(
                            uniquifiedFeatureTypeName, transaction);

            while (reader.hasNext()) {
                if (monitor.isCanceled()) {
                    break;
                }
                SimpleFeature feature = (SimpleFeature) reader.next();
                SimpleFeature next = (SimpleFeature) writer.next();

                // (JD) TODO: some formats will rearrange the geometry type (like shapefile) which
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
            LOGGER.info("load to target took " + (System.currentTimeMillis() - startTime));
        } catch (Throwable e) {
            error = e;
        }
        // no finally block, there is too much to do

        if (error != null || monitor.isCanceled()) {
            // all sub exceptions in this catch block should be logged, not thrown
            // as the triggering exception will be thrown

            // failure, rollback transaction
            try {
                transaction.rollback();
            } catch (Exception e1) {
                LOGGER.log(Level.WARNING, "Error rolling back transaction", e1);
            }

            // attempt to drop the type that was created as well
            try {
                dropSchema(dataStoreDestination, featureTypeName);
            } catch (Exception e1) {
                LOGGER.log(Level.WARNING, "Error dropping schema in rollback", e1);
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
        catalog.add(resource);

        // add the layer (and style)
        if (layer.getDefaultStyle().getId() == null) {
            catalog.add(layer.getDefaultStyle());
        }

        layer.setEnabled(true);
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
     * computes the lat/lon bounding box from the native bounding box and srs, optionally overriding
     * the value already set.
     */
    boolean computeLatLonBoundingBox(ImportTask task, boolean force) throws Exception {
        ResourceInfo r = task.getLayer().getResource();
        if (force || r.getLatLonBoundingBox() == null && r.getNativeBoundingBox() != null) {
            CoordinateReferenceSystem nativeCRS = CRS.decode(r.getSRS());
            ReferencedEnvelope nativeBbox =
                    new ReferencedEnvelope(r.getNativeBoundingBox(), nativeCRS);
            r.setLatLonBoundingBox(nativeBbox.transform(CRS.decode("EPSG:4326"), true));
            return true;
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
        xs.registerLocalConverter(GeneralEnvelope.class, "crs", new CRSConverter());

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

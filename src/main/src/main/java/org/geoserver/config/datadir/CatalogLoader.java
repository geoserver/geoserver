/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.config.datadir;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.datadir.DataDirectoryWalker.LayerDirectory;
import org.geoserver.config.datadir.DataDirectoryWalker.StoreDirectory;
import org.geoserver.config.datadir.DataDirectoryWalker.WorkspaceDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geotools.util.logging.Logging;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Specialized loader for GeoServer catalog objects that supports parallel loading.
 *
 * <p>This class is responsible for loading the entire GeoServer catalog structure, including:
 *
 * <ul>
 *   <li>Workspaces and namespaces
 *   <li>Stores (data stores, coverage stores, WMS stores, etc.)
 *   <li>Resources (feature types, coverages, WMS layers, etc.)
 *   <li>Layers
 *   <li>Styles
 *   <li>Layer groups
 * </ul>
 *
 * <p>The loading process uses parallel streams extensively to maximize throughput, especially when loading from network
 * drives or other high-latency storage systems. The loader maintains proper object relationships despite the parallel
 * loading.
 *
 * <p>The catalog loading process follows GeoServer's {@link GeoServerDataDirectory data directory} structure hierarchy
 * and loads objects in the correct dependency order to ensure proper object relationships.
 *
 * @implNote The {@link XStreamPersister} instances used to deserialize {@link CatalogInfo} objects deliberately avoid
 *     resolving {@link ResolvingProxy} references during the parsing phase. This architecture enables safe
 *     multi-threaded parsing while significantly improving overall performance by deferring potentially blocking
 *     operations (such as calling {@link FeatureTypeInfo#getFeatureType()}, that ends up calling the
 *     {@link ResourcePool} and could block on concurrent GeoTools factory lookups) to a controlled resolution phase.
 *     These references are later resolved in the main thread through the {@link CatalogLoaderSanitizer} component,
 *     ensuring system integrity while maximizing parallel processing efficiency.
 */
class CatalogLoader {

    private static final Logger LOGGER =
            Logging.getLogger(CatalogLoader.class.getPackage().getName());

    final DataDirectoryWalker fileWalk;
    final CatalogImpl catalog;

    // delegate sanitization tasks to this helper
    private final CatalogLoaderSanitizer sanitizer;

    /**
     * Creates a new CatalogLoader with the specified catalog and file walker.
     *
     * @param catalog the GeoServer catalog to populate
     * @param fileWalk the directory walker that provides access to catalog files
     */
    public CatalogLoader(CatalogImpl catalog, DataDirectoryWalker fileWalk) {
        requireNonNull(catalog);
        requireNonNull(fileWalk);
        this.catalog = catalog;
        this.fileWalk = fileWalk;
        this.sanitizer = new CatalogLoaderSanitizer(this);
    }

    /**
     * Loads the entire GeoServer catalog from the data directory.
     *
     * <p>This method coordinates the loading of all catalog objects, handling:
     *
     * <ul>
     *   <li>Global styles
     *   <li>Workspaces, their namespaces, stores, layers, styles, and layer groups
     *   <li>Global layer groups
     * </ul>
     *
     * <p>The loading process is parallelized for performance, with proper dependency management to ensure object
     * relationships are maintained.
     */
    public void loadCatalog() {
        final boolean extendedValidation = this.catalog.isExtendedValidation();
        this.catalog.setExtendedValidation(false);

        // admin auth set by GeoServerLoader and propagated to the ForkJoinPool threads
        Authentication admin = SecurityContextHolder.getContext().getAuthentication();
        ForkJoinPool executor = ExecutorFactory.createExecutor(admin);
        try {
            addAll(executor, this::loadGlobalStyles);
            // This is the best place where to initialize default styles since the sanitization during loading might
            // assign a default style on broken or illegal links
            sanitizer.initializeDefaultStyles();

            // load all workspaces and their stores/styles/layers/groups
            addAll(executor, this::loadWorkspaces);
            sanitizer.setDefaultWorkspace();

            addAll(executor, this::loadGlobalLayerGroups);

            catalog.resolve();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Thread interrupted while loading the catalog", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException | IOException e) {
            throw new IllegalStateException(e);
        } finally {
            this.catalog.setExtendedValidation(extendedValidation);
            executor.shutdownNow();
        }
    }

    /**
     * Loads global styles from the data directory.
     *
     * <p>Global styles are not associated with any workspace and are available to all layers.
     */
    private void loadGlobalStyles() {
        loadStyles(null, fileWalk.globalStyles().stream());
    }

    /**
     * Loads all workspaces and their associated resources in parallel.
     *
     * <p>This method streams through workspace directories and loads each workspace along with its namespace, stores,
     * layers, styles, and layer groups.
     */
    private void loadWorkspaces() {
        Stream<WorkspaceDirectory> stream = fileWalk.workspaces().stream();
        stream.parallel().forEach(this::loadWorkspace);
    }

    /**
     * Loads global layer groups from the data directory.
     *
     * <p>Global layer groups are not associated with any workspace and are available globally.
     */
    private void loadGlobalLayerGroups() {
        loadLayerGroups(fileWalk.globalLayerGroups().stream());
    }

    /**
     * Adds the workspace and namespace to the catalog directly from inside the calling worker thread, as well as the
     * styles. For stores, layers, and layer groups, work may be deferred to additional threads in the pool.
     */
    private void loadWorkspace(WorkspaceDirectory wsdir) {
        Optional<WorkspaceInfo> wsinfo = depersist(wsdir.workspaceFile());
        Optional<NamespaceInfo> nsinfo = depersist(wsdir.namespaceFile());

        if (wsinfo.isPresent() && nsinfo.isPresent()) {
            WorkspaceInfo ws = wsinfo.orElseThrow();
            NamespaceInfo ns = nsinfo.orElseThrow();
            addToCatalog(ws);
            addToCatalog(ns);

            loadStyles(ws, wsdir.styles().stream());
            loadStores(wsdir.stores());
            loadLayerGroups(wsdir.layerGroups().stream());
        }
    }

    /**
     * Loads style files and adds the styles to the catalog.
     *
     * <p>Styles are safe to add to the catalog in a worker thread. Each style is validated against the target workspace
     * to ensure it belongs to the correct workspace. May a style file point to a non-existing workspace, or to a
     * different workspace than the one in its folder, it'll be ignored.
     *
     * <p>This validation happens during the load process since the catalog has no way to know when a style with a
     * missing or invalid workspace is being added.
     *
     * @param targetWorkspace the workspace styles are expected to belong to, or null for global styles
     * @param stream stream of paths to style files
     */
    private void loadStyles(@Nullable WorkspaceInfo targetWorkspace, Stream<Path> stream) {
        Stream<StyleInfo> styles = depersist(stream, StyleInfo.class);
        styles.filter(s -> sanitizer.validate(targetWorkspace, s)).forEach(this::addToCatalog);
    }

    /**
     * Loads all stores from a stream of store directories in parallel.
     *
     * @param stream stream of store directories to process
     */
    private void loadStores(Stream<StoreDirectory> stream) {
        stream.parallel().forEach(this::loadStore);
    }

    /**
     * Loads a store from a store directory.
     *
     * @param storeDir the directory containing the store configuration
     */
    private void loadStore(StoreDirectory storeDir) {
        Optional<StoreInfo> store = depersist(storeDir.storeFile);
        if (store.isPresent()) {
            addToCatalog(store.orElseThrow());
            loadLayers(storeDir.layers());
        }
    }

    /**
     * Loads all layers from a stream of layer directories in parallel.
     *
     * @param layers stream of layer directories to process
     */
    private void loadLayers(Stream<LayerDirectory> layers) {
        layers.parallel().forEach(this::loadResourceAndLayer);
    }

    /**
     * Loads a resource and its associated layer from a layer directory.
     *
     * @param layerDir the directory containing the resource and layer configuration
     */
    private void loadResourceAndLayer(LayerDirectory layerDir) {
        Optional<ResourceInfo> resource = depersist(layerDir.resourceFile);
        Optional<LayerInfo> layer = depersist(layerDir.layerFile);
        if (resource.isPresent() && layer.isPresent()) {
            addToCatalog(resource.orElseThrow());
            addToCatalog(layer.orElseThrow());
        }
    }

    /**
     * Loads layer groups from a stream of layer group files.
     *
     * @param stream stream of paths to layer group files
     */
    private void loadLayerGroups(Stream<Path> stream) {
        depersist(stream, LayerGroupInfo.class).forEach(this::addToCatalog);
    }

    /**
     * Runs a task in the executor pool.
     *
     * <p>This method submits a task to the ForkJoinPool, which allows all Stream.parallel() calls within the task to
     * run in the work-stealing threads of the pool, and waits for its termination.
     *
     * @param executor the thread pool to run the task in
     * @param runnable the task to run
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws ExecutionException if the task throws an exception
     */
    private void addAll(ForkJoinPool executor, Runnable runnable) throws InterruptedException, ExecutionException {
        // By submitting a task to the ForkJoinPool, all Stream.parallel() calls will
        // run in the work-stealing threads of the pool, no need to implement fancy classes like ForkJoinTask
        Future<?> future = executor.submit(runnable::run);
        future.get();
    }

    /**
     * Adds a catalog info object to the catalog based on its type.
     *
     * <p>This method dispatches to the appropriate catalog add method based on the type of the catalog info object.
     *
     * @param <C> the type of catalog info
     * @param info the catalog info object to add
     * @return the added catalog info object
     * @throws IllegalArgumentException if the info object is of an unknown type
     */
    private <C extends CatalogInfo> C addToCatalog(C info) {

        sanitizer.resolveProxies(info);

        if (info instanceof WorkspaceInfo) {
            doAddToCatalog((WorkspaceInfo) info, catalog::add, WorkspaceInfo::getName);
        } else if (info instanceof NamespaceInfo) {
            doAddToCatalog((NamespaceInfo) info, catalog::add, NamespaceInfo::getPrefix);
        } else if (info instanceof StoreInfo) doAddToCatalog((StoreInfo) info, catalog::add, StoreInfo::getName);
        else if (info instanceof ResourceInfo) doAddToCatalog((ResourceInfo) info, catalog::add, this::resourceLog);
        else if (info instanceof LayerInfo) {
            doAddToCatalog((LayerInfo) info, catalog::add, LayerInfo::getName);
        } else if (info instanceof LayerGroupInfo) {
            doAddToCatalog((LayerGroupInfo) info, catalog::add, LayerGroupInfo::getName);
        } else if (info instanceof StyleInfo) doAddToCatalog((StyleInfo) info, catalog::add, StyleInfo::getName);
        else {
            throw new IllegalArgumentException(format("Unexpected value: %s", info));
        }
        return info;
    }

    private String resourceLog(CatalogInfo resource) {
        ResourceInfo r = (ResourceInfo) resource;
        return r.getName() + ", " + (r.isEnabled() ? "enabled" : "disabled");
    }

    /**
     * Performs the actual addition of a catalog info object to the catalog.
     *
     * <p>Called from a working thread (for workspaces, namespaces, and styles), or from the calling thread (e.g.
     * {@code main} during the startup process, or the thread that called {@link GeoServerLoader#reload()}).
     *
     * <p>This method handles the resolution of proxies and error logging if the addition fails with the help of
     * {@link CatalogLoaderSanitizer#resolveProxies(CatalogInfo)}
     *
     * @param <I> the type of catalog info
     * @param info the catalog info object to add
     * @param saver the function to use to save the object to the catalog
     * @param name a function to extract a name from the object for logging
     * @return an Optional containing the added object, or empty if addition failed
     */
    private <I extends CatalogInfo> Optional<I> doAddToCatalog(I info, Consumer<I> saver, Function<I, String> name) {
        try {
            saver.accept(info);
            LOGGER.log(Level.CONFIG, () -> format("Loaded %s %s", sanitizer.typeOf(info), name.apply(info)));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> format("Failed to load %s %s", sanitizer.typeOf(info), info.getId()));
            return Optional.empty();
        }
        return Optional.of(info);
    }

    /**
     * Converts a file path to a catalog info object.
     *
     * <p>This method delegates to the XStreamLoader to deserialize the file.
     *
     * @param <C> the type of catalog info
     * @param file the file to deserialize
     * @return an Optional containing the deserialized object, or empty if deserialization failed
     */
    <C extends CatalogInfo> Optional<C> depersist(Path file) {
        return fileWalk.getXStreamLoader().depersist(file);
    }

    /**
     * Converts a stream of file paths to a stream of catalog info objects.
     *
     * <p>This method maps each file path to a catalog info object, filters out failed deserializations, and casts the
     * results to the specified type.
     *
     * @param <C> the type of catalog info
     * @param stream the stream of file paths to deserialize
     * @param type the class to cast the deserialized objects to
     * @return a stream of deserialized catalog info objects
     */
    private <C extends CatalogInfo> Stream<C> depersist(Stream<Path> stream, Class<C> type) {
        return stream.map(this::depersist)
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .map(type::cast);
    }

    /**
     * Persists a catalog info object to a file.
     *
     * <p>This method delegates to the XStreamLoader to serialize the object.
     *
     * @param info the catalog info object to serialize
     * @param path the file path to write to
     * @throws IOException if an error occurs during serialization
     */
    void persist(CatalogInfo info, Path path) throws IOException {
        fileWalk.getXStreamLoader().persist(info, path);
    }
}

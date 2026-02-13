/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.config.datadir;

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
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.datadir.DataDirectoryWalker.LayerDirectory;
import org.geoserver.config.datadir.DataDirectoryWalker.StoreDirectory;
import org.geoserver.config.datadir.DataDirectoryWalker.WorkspaceDirectory;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.logging.Logging;
import org.jspecify.annotations.Nullable;
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
        setTemporaryEntityResolverProvider();
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
            clearTemporaryEntityResolverProvider();
            executor.shutdownNow();
        }
    }

    /**
     * Sets up a temporary EntityResolverProvider during catalog loading to avoid Spring bean dependency issues.
     *
     * <p>This method creates a temporary EntityResolverProvider that doesn't depend on Spring initialization, instead
     * using the global.xml configuration if available. This prevents deadlocks when validating XML files that would
     * otherwise use {@code GeoServerExtensions.bean(EntityResolverProvider.class)}
     *
     * <p>The call chain that leads to the deadlock is:
     *
     * <pre>{@code
     * loadCatalog() ->
     * CatalogImpl.add(LayerGroup) ->
     *  validate(LayerGroup) ->
     *   StyledLayerDescriptor sld = styles.get(i).getSLD() ->
     *    StyleInfoImpl.getSLD() ->
     *     ResourcePool.getSld(StyleInfo) ->
     *      GeoServerDataDirectory.parseSld(StyleInfo) ->
     *       GeoServerDataDirectory.getEntityResolver() ->
     *        GeoServerExtensions.bean(EntityResolverProvider.class)
     * }</pre>
     *
     * <p>The temporary provider respects XML external entity settings from global.xml configuration.
     */
    private void setTemporaryEntityResolverProvider() {
        Optional<Path> gsGlobalFile = fileWalk.gsGlobal();
        Optional<GeoServerInfo> info = gsGlobalFile.flatMap(fileWalk.getXStreamLoader()::depersist);
        GeoServerInfo global = info.orElse(null);
        GeoServer geoServer = new GeoServerImpl();
        if (global != null) {
            geoServer.setGlobal(global);
        }
        EntityResolverProvider provider = new EntityResolverProvider(geoServer);
        fileWalk.getDataDirectory().setEntityResolverProvider(provider);
    }

    /**
     * Clears the temporary EntityResolverProvider after catalog loading completes.
     *
     * <p>This removes the temporary provider set by {@link #setTemporaryEntityResolverProvider()}, allowing normal
     * Spring bean resolution to take over after catalog loading is complete.
     */
    private void clearTemporaryEntityResolverProvider() {
        fileWalk.getDataDirectory().setEntityResolverProvider(null);
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
        Stream<StyleInfo> styles =
                depersist(stream, StyleInfo.class, targetWorkspace == null ? null : targetWorkspace.getName());
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
        Optional<StoreInfo> store = depersist(storeDir.storeFile, pathContext(storeDir.storeFile));
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
        Optional<ResourceInfo> resource = depersist(layerDir.resourceFile, pathContext(layerDir.resourceFile));
        Optional<LayerInfo> layer = depersist(layerDir.layerFile, pathContext(layerDir.layerFile));
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
        depersist(stream, LayerGroupInfo.class, null).forEach(this::addToCatalog);
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
        Future<?> future = executor.submit(runnable);
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

        if (info instanceof WorkspaceInfo workspaceInfo) {
            doAddToCatalog(workspaceInfo, catalog::add, WorkspaceInfo::getName);
        } else if (info instanceof NamespaceInfo namespaceInfo) {
            doAddToCatalog(namespaceInfo, catalog::add, NamespaceInfo::getPrefix);
        } else if (info instanceof StoreInfo storeInfo) doAddToCatalog(storeInfo, catalog::add, StoreInfo::getName);
        else if (info instanceof ResourceInfo resourceInfo)
            doAddToCatalog(resourceInfo, catalog::add, this::resourceLog);
        else if (info instanceof LayerInfo layerInfo) {
            doAddToCatalog(layerInfo, catalog::add, LayerInfo::getName);
        } else if (info instanceof LayerGroupInfo groupInfo) {
            doAddToCatalog(groupInfo, catalog::add, LayerGroupInfo::getName);
        } else if (info instanceof StyleInfo styleInfo) doAddToCatalog(styleInfo, catalog::add, StyleInfo::getName);
        else {
            throw new IllegalArgumentException("Unexpected value: %s".formatted(info));
        }
        return info;
    }

    private String resourceLog(CatalogInfo resource) {
        ResourceInfo r = (ResourceInfo) resource;
        return r.getName() + ", " + (r.isEnabled() ? "enabled" : "disabled");
    }

    private String pathContext(Path path) {
        if (path == null) {
            return null;
        }
        Path parent = path.getParent();
        return parent == null ? path.toString() : parent.toString();
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
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Starting load for %s %s".formatted(sanitizer.typeOf(info), describe(info, name)));
        }
        try {
            saver.accept(info);
            LOGGER.log(Level.CONFIG, () -> "Loaded %s %s".formatted(sanitizer.typeOf(info), describe(info, name)));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> "Failed to load %s %s"
                    .formatted(sanitizer.typeOf(info), describe(info, name)));
            return Optional.empty();
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Finished load for %s %s".formatted(sanitizer.typeOf(info), describe(info, name)));
        }
        return Optional.of(info);
    }

    private <I extends CatalogInfo> String describe(I info, Function<I, String> name) {
        String baseName = name.apply(info);
        String parent = null;
        if (info instanceof LayerInfo layer) {
            parent = layer.getResource() != null ? layer.getResource().prefixedName() : null;
        } else if (info instanceof ResourceInfo resource) {
            StoreInfo store = resource.getStore();
            parent = store != null ? store.getWorkspace().getName() + "/" + store.getName() : null;
        } else if (info instanceof StoreInfo store) {
            parent = store.getWorkspace() != null ? store.getWorkspace().getName() : null;
        } else if (info instanceof StyleInfo style) {
            parent = style.getWorkspace() != null ? style.getWorkspace().getName() : "global";
        }
        StringBuilder sb = new StringBuilder(baseName == null ? "null" : baseName);
        if (parent != null) {
            sb.append(" (parent=").append(parent).append(")");
        }
        return sb.toString();
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
        try {
            return fileWalk.getXStreamLoader().depersist(file);
        } catch (Exception e) {
            String message = "Failed to parse catalog file " + file;
            LOGGER.log(Level.SEVERE, message, e);
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * Deserializes a catalog info object from a file, logging failures with optional context.
     *
     * <p>Delegates to the XStreamLoader to parse the file; on failure, logs at SEVERE with the source file path and the
     * provided context (e.g., workspace or store name) and rethrows as {@link IllegalStateException}.
     *
     * @param <C> the type of catalog info
     * @param file the file to deserialize (not null)
     * @param context optional label (nullable) included in error logs to identify the parent workspace/store
     * @return the deserialized object wrapped in an Optional if successful
     * @throws IllegalStateException if parsing fails
     */
    <C extends CatalogInfo> Optional<C> depersist(Path file, @Nullable String context) {
        try {
            return fileWalk.getXStreamLoader().depersist(file);
        } catch (Exception e) {
            String prefix = context == null ? "" : context + " - ";
            String message = "Failed to parse catalog file " + prefix + file;
            LOGGER.log(Level.SEVERE, message, e);
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * Converts a stream of file paths to a stream of catalog info objects with contextual error logging.
     *
     * <p>Each path is deserialized with {@link #depersist(Path, String)}, which logs failures at SEVERE level,
     * including the provided context (e.g., workspace or store name) and the source file path. Failed deserializations
     * are skipped, so the returned stream only contains successfully parsed objects.
     *
     * @param <C> the type of catalog info
     * @param stream the stream of file paths to deserialize (not null)
     * @param type the class to cast the deserialized objects to (not null)
     * @param context optional contextual label (nullable), such as workspace/store, included in error logs
     * @return a stream of deserialized catalog info objects; empty for entries that failed to parse
     */
    private <C extends CatalogInfo> Stream<C> depersist(Stream<Path> stream, Class<C> type, @Nullable String context) {
        return stream.flatMap(
                file -> depersist(file, context).map(type::cast).map(Stream::of).orElseGet(Stream::empty));
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

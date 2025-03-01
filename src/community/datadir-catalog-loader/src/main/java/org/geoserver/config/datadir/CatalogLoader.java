/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.config.datadir;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.datadir.DataDirectoryWalker.LayerDirectory;
import org.geoserver.config.datadir.DataDirectoryWalker.StoreDirectory;
import org.geoserver.config.datadir.DataDirectoryWalker.WorkspaceDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

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
 * <p>The catalog loading process follows GeoServer's directory structure hierarchy and loads objects in the correct
 * dependency order to ensure proper object relationships.
 */
public class CatalogLoader {

    private static final Logger LOGGER =
            Logging.getLogger(CatalogLoader.class.getPackage().getName());

    private final DataDirectoryWalker fileWalk;
    private final CatalogImpl catalog;

    public CatalogLoader(CatalogImpl catalog, DataDirectoryWalker fileWalk) {
        requireNonNull(catalog);
        requireNonNull(fileWalk);
        this.catalog = catalog;
        this.fileWalk = fileWalk;
    }

    private BlockingQueue<byte[]> sourceQueue = new LinkedBlockingQueue<>(1_000);
    private static final byte[] TERMINAL_TOKEN = new byte[0];

    public void loadCatalog() {
        final boolean extendedValidation = this.catalog.isExtendedValidation();
        this.catalog.setExtendedValidation(false);
        fileWalk.setCatalog(catalog);

        ForkJoinPool executor = ExecutorFactory.createExecutor();
        try {
            addAll(executor, this::loadGlobalStyles);
            // This is the best place where to initialize default styles since the sanitization during loading might
            // assign a default style on broken or illegal links
            initializeDefaultStyles();

            // load all workspaces and their stores/styles/layers/groups
            addAll(executor, this::loadWorkspaces);
            setDefaultWorkspace();

            addAll(executor, this::loadGlobalLayerGroups);

            catalog.resolve();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Thread interrupted while loading the catalog", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        } finally {
            this.catalog.setExtendedValidation(extendedValidation);
            executor.shutdownNow();
        }
    }

    protected void initializeDefaultStyles() {
        initializeStyle(StyleInfo.DEFAULT_POINT, "default_point.sld");
        initializeStyle(StyleInfo.DEFAULT_LINE, "default_line.sld");
        initializeStyle(StyleInfo.DEFAULT_POLYGON, "default_polygon.sld");
        initializeStyle(StyleInfo.DEFAULT_RASTER, "default_raster.sld");
        initializeStyle(StyleInfo.DEFAULT_GENERIC, "default_generic.sld");
    }

    /** Copies a well known style out to the data directory and adds a catalog entry for it. */
    void initializeStyle(String styleName, String sld) {
        if (catalog.getStyleByName(styleName) != null) {
            return;
        }
        // copy the file out to the data directory if necessary
        GeoServerResourceLoader resourceLoader = catalog.getResourceLoader();
        Resource styleResource = resourceLoader.get(Paths.path("styles", sld));
        if (!Resources.exists(styleResource)) {
            try (InputStream in = GeoServerLoader.class.getResourceAsStream(sld);
                    OutputStream out = styleResource.out()) {
                IOUtils.copy(in, out);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error initializing default style %s" + styleName, e);
            }
        }

        // create a style for it
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName(styleName);
        s.setFilename(sld);
        catalog.add(s);
    }

    private void addAll(ForkJoinPool executor, Runnable runnable) throws InterruptedException, ExecutionException {
        Future<?> future = executor.submit(new Producer(runnable));
        consumeQueue(executor);
        future.get();
    }

    private class Producer implements Runnable {
        private Runnable delegate;

        Producer(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                delegate.run();
            } finally {
                try {
                    sourceQueue.put(TERMINAL_TOKEN);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void consumeQueue(ForkJoinPool executor) {
        while (!executor.isShutdown()) {
            try {
                byte[] contents = sourceQueue.take();
                if (contents == TERMINAL_TOKEN) {
                    return;
                }
                parse(contents).ifPresent(this::addToCatalog);
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Unable to keep consuming the queue of loaded catalog files", e);
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }
    }

    private void addToCatalog(CatalogInfo info) {
        if (info instanceof WorkspaceInfo) doAddToCatalog((WorkspaceInfo) info, catalog::add);
        else if (info instanceof NamespaceInfo) doAddToCatalog((NamespaceInfo) info, catalog::add);
        else if (info instanceof StoreInfo) doAddToCatalog((StoreInfo) info, catalog::add);
        else if (info instanceof ResourceInfo) doAddToCatalog((ResourceInfo) info, catalog::add);
        else if (info instanceof LayerInfo) doAddToCatalog((LayerInfo) info, catalog::add);
        else if (info instanceof LayerGroupInfo) doAddToCatalog((LayerGroupInfo) info, catalog::add);
        else if (info instanceof StyleInfo) doAddToCatalog((StyleInfo) info, catalog::add);
        else {
            throw new IllegalArgumentException(format("Unexpected value: %s", info));
        }
    }

    private void setDefaultWorkspace() {
        // set the default workspace, this value might be null in the case of coming
        // from a 2.0.0 data directory. See https://osgeo-org.atlassian.net/browse/GEOS-3440
        Optional<Path> defaultWorkspaceFile = fileWalk.defaultWorkspace();

        if (defaultWorkspaceFile.isPresent()) {
            Optional<WorkspaceInfo> defaultWorkspace = depersist(defaultWorkspaceFile.orElseThrow());
            defaultWorkspace = defaultWorkspace.map(WorkspaceInfo::getId).map(catalog::getWorkspace);
            if (defaultWorkspace.isPresent()) {
                WorkspaceInfo ws = defaultWorkspace.orElseThrow();
                NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
                catalog.setDefaultWorkspace(ws);
                catalog.setDefaultNamespace(ns);
                return;
            }
        }
        WorkspaceInfo ws = catalog.getDefaultWorkspace();
        if (ws != null) {
            Path path = fileWalk.getDefaultWorkspaceFile();
            try {
                persist(ws, path);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to persist " + ws + " at '" + path + "'", e);
            }
        }
    }

    private void loadGlobalStyles() {
        loadStyles(null, fileWalk.globalStyles().stream());
    }

    private void loadWorkspaces() {
        Stream<WorkspaceDirectory> stream = fileWalk.workspaces().stream();
        stream.parallel().forEach(this::loadWorkspace);
    }

    private void loadGlobalLayerGroups() {
        loadLayerGroups(fileWalk.globalLayerGroups().stream());
    }

    /**
     * Adds the workspace and namespace to the catalog directly from inside the calling worker thread, as well as the
     * styles. For stores, layers, and layer groups, their file contents are added to the {@link #sourceQueue} to be
     * processed by the main thread, since it's a bit unpredictable when {@link XStreamPersister} will trigger some
     * potentially blocking operation (e.g. FeatureTypeInfo.getFeatureType())
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

    /** Loads the files and adds the styles to the catalog. Styles are safe to add to the catalog in a worker thread */
    private void loadStyles(@Nullable WorkspaceInfo targetWorkspace, Stream<Path> stream) {
        Stream<StyleInfo> styles = depersist(stream, StyleInfo.class);
        styles.filter(s -> validate(targetWorkspace, s)).forEach(this::addToCatalog);
    }

    private boolean validate(@Nullable WorkspaceInfo expectedWorkspace, StyleInfo s) {
        if (null == expectedWorkspace) {
            if (s.getWorkspace() != null) {
                String ws = s.getWorkspace().getId();
                String msg = "Style %s (%s) is expected to have no workspace but has workspace %s. Style ignored.";
                LOGGER.severe(format(msg, s.getName(), s.getId(), ws));
                return false;
            }
        } else {
            if (s.getWorkspace() == null) {
                String msg = "Style %s[%s] should have workspace %s but has no workspace. Style ignored.";
                LOGGER.severe(format(msg, s.getName(), s.getId(), expectedWorkspace.getName()));
                return false;
            } else if (!expectedWorkspace.getId().equals(s.getWorkspace().getId())) {
                String ws = s.getWorkspace().getId();
                String msg = "Style %s[%s] should have workspace %s but has workspace %s. Style ignored.";
                LOGGER.severe(format(msg, s.getName(), s.getId(), expectedWorkspace.getName(), ws));
                return false;
            }
        }
        return true;
    }

    private void loadStores(Stream<StoreDirectory> stream) {
        stream.parallel().forEach(this::loadStore);
    }

    private void loadStore(StoreDirectory storeDir) {
        Optional<byte[]> store = load(storeDir.storeFile);
        if (store.isPresent()) {
            addToQueue(store.orElseThrow());
            loadLayers(storeDir.layers());
        }
    }

    private void loadLayers(Stream<LayerDirectory> layers) {
        layers.parallel().forEach(this::loadResourceAndLayer);
    }

    private void loadResourceAndLayer(LayerDirectory layerDir) {
        Optional<byte[]> resource = load(layerDir.resourceFile);
        Optional<byte[]> layer = load(layerDir.layerFile);
        if (resource.isPresent() && layer.isPresent()) {
            addToQueue(resource.orElseThrow());
            addToQueue(layer.orElseThrow());
        }
    }

    private void addToQueue(byte[] info) {
        try {
            sourceQueue.put(info);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Unable to keep up adding parsed CatalogInfos to the queue", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
    }

    private <I extends CatalogInfo> Optional<I> doAddToCatalog(I info, Consumer<I> saver) {
        try {
            resolve(info);
            saver.accept(info);
            LOGGER.log(Level.FINE, () -> format("Added to Catalog: %s[%s]", info, info.getId()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> format("Error adding to the catalog %s", info));
            return Optional.empty();
        }
        return Optional.of(info);
    }

    private void resolve(CatalogInfo info) {
        if (info instanceof LayerInfo) resolve((LayerInfo) info);
        if (info instanceof LayerGroupInfo) resolve((LayerGroupInfo) info);
    }

    private void resolve(LayerInfo l) {
        l.setResource(resolveProxy(l.getResource(), l));
        resolveDefaultStyle(l);

        Set<StyleInfo> styles = l.getStyles();
        boolean resolveStyles = styles != null
                && !styles.isEmpty()
                && !(l.getResource() instanceof WMSLayerInfo); // avoid loading remote styles
        if (resolveStyles) {
            List<StyleInfo> resolved =
                    styles.stream().map(s -> resolveLayerStyle(s, l)).collect(Collectors.toList());
            styles.clear();
            styles.addAll(resolved);
        }
    }

    /**
     * Success scenario: layer references a style, and it resolves to either a global style or a style in the same
     * workspace. Failure scenarios:
     *
     * <ul>
     *   <li>defaultStyle is null
     *   <li>defaultStyle belongs to a different workspace
     * </ul>
     *
     * In either case assign one. Either {@link StyleInfo#DEFAULT_RASTER} or {@link StyleInfo#DEFAULT_GENERIC}
     *
     * <p>Precondition: l.getResource() is already resolved (down to the store's workspace)
     */
    private void resolveDefaultStyle(LayerInfo l) {
        StyleInfo defaultStyle = l.getDefaultStyle();
        if (defaultStyle == null) {
            defaultStyle = getDefaultStyleFor(l);
            LOGGER.severe(
                    format("Layer %s has no default style, assigned '%s'", l.prefixedName(), defaultStyle.getName()));
        } else {
            // note: it'd be better to just call resolveLayerStyle for a dangling proxy but there's a test
            // (org.geoserver.catalog.impl.CatalogProxiesTest)
            // that actually checks that it resolved to null
            if (null == resolveProxy(defaultStyle, l, false)) {
                l.setDefaultStyle(null);
                LOGGER.severe(
                        format("Layer %s has a dangling default style %s", l.prefixedName(), defaultStyle.getId()));
            } else {
                defaultStyle = resolveLayerStyle(l.getDefaultStyle(), l);
            }
        }
        l.setDefaultStyle(defaultStyle);
    }

    /**
     * Success scenario: layer references a style, and it resolves to either a global style or a style in the same
     * workspace.
     *
     * <p>Failure scenarios:
     *
     * <ul>
     *   <li>referredStyle is null
     *   <li>referredStyle belongs to a different workspace
     * </ul>
     *
     * In either case return a generic style, either {@link StyleInfo#DEFAULT_RASTER} or
     * {@link StyleInfo#DEFAULT_GENERIC}
     *
     * <p>Precondition: l.getResource() is already resolved (down to the store's workspace)
     */
    private StyleInfo resolveLayerStyle(@NonNull StyleInfo referredStyle, LayerInfo layer) {
        StyleInfo resolvedStyle = resolveProxy(referredStyle, layer, false); // do not fail if not in catalog
        ResourceInfo resource = requireNonNull(layer.getResource(), "resource");
        StoreInfo store = requireNonNull(resource.getStore(), "resource.store");
        if (resolvedStyle == null) {
            resolvedStyle = getDefaultStyleFor(layer);
            LOGGER.severe(format(
                    "Layer %s points to style (%s) which does not exist or belongs to another workspace. Assigning the generic style '%s'",
                    layer.prefixedName(), referredStyle.getId(), resolvedStyle.getName()));
            return resolvedStyle;
        }
        if (resolvedStyle.getWorkspace() == null) { // it's a global style
            return resolvedStyle;
        }
        // verify the style workspace is the same as the layer's workspace
        // if not, find an equally named style in the layer's workspace, or default to a generic style
        WorkspaceInfo layerWs = requireNonNull(store.getWorkspace(), "resource.store.workspace");
        WorkspaceInfo styleWs = resolvedStyle.getWorkspace();
        if (layerWs.getName().equals(styleWs.getName())) {
            return resolvedStyle;
        }

        StyleInfo wrongWsStyle = resolvedStyle;
        resolvedStyle = getDefaultStyleFor(layer);
        LOGGER.severe(format(
                "Layer %s points to style %s[%s] that belongs to another workspace. Assigning the generic style '%s'",
                layer.prefixedName(), wrongWsStyle.prefixedName(), wrongWsStyle.getId(), resolvedStyle.getName()));

        return resolvedStyle;
    }

    private StyleInfo getDefaultStyleFor(LayerInfo l) {
        String defStyleName =
                l.getResource() instanceof FeatureTypeInfo ? StyleInfo.DEFAULT_GENERIC : StyleInfo.DEFAULT_RASTER;
        return catalog.getStyleByName(defStyleName);
    }

    protected void resolve(LayerGroupInfo layerGroup) {
        LayerGroupInfoImpl lg = (LayerGroupInfoImpl) layerGroup;

        lg.setWorkspace(resolveProxy(lg.getWorkspace(), lg));
        lg.setRootLayer(resolveProxy(lg.getRootLayer(), lg));
        lg.setRootLayerStyle(resolveProxy(lg.getRootLayerStyle(), lg));

        if (lg.getLayers() != null && !lg.getLayers().isEmpty()) {
            resolveLayerGroupLayers(lg.getLayers());
        }
        if (lg.getStyles() != null && !lg.getStyles().isEmpty()) {
            resolveLayerGroupStyles(lg.getLayers(), lg.getStyles());
        }

        // now resolves layers and styles defined in layer group styles
        for (LayerGroupStyle groupStyle : lg.getLayerGroupStyles()) {
            resolveLayerGroupLayers(groupStyle.getLayers());
            resolveLayerGroupStyles(groupStyle.getLayers(), groupStyle.getStyles());
        }
    }

    private void resolveLayerGroupStyles(List<PublishedInfo> assignedLayers, List<StyleInfo> styles) {
        for (int i = 0; i < styles.size(); i++) {
            StyleInfo s = styles.get(i);
            if (s != null) {
                PublishedInfo assignedLayer = assignedLayers.get(i);
                StyleInfo resolved = null;
                if (assignedLayer instanceof LayerGroupInfo) {
                    // special case we might have a StyleInfo representing
                    // only the name of a LayerGroupStyle thus not present in Catalog.
                    // We take the ref and create a new object
                    // without searching in catalog.
                    String ref = ResolvingProxy.getRef(s);
                    if (ref != null) {
                        StyleInfo styleInfo = new StyleInfoImpl(catalog);
                        styleInfo.setName(ref);
                        resolved = styleInfo;
                    }
                }
                if (resolved == null) resolved = ResolvingProxy.resolve(catalog, s);

                styles.set(i, resolved);
            }
        }
    }

    private void resolveLayerGroupLayers(List<PublishedInfo> layers) {
        for (int i = 0; i < layers.size(); i++) {
            PublishedInfo l = layers.get(i);

            if (l != null) {
                PublishedInfo resolved;
                if (l instanceof LayerGroupInfo) {
                    resolved = ResolvingProxy.resolve(catalog, (LayerGroupInfo) l);
                    // special case to handle catalog loading, when nested publishibles might not be
                    // loaded.
                    if (resolved == null) {
                        resolved = l;
                    }
                } else if (l instanceof LayerInfo) {
                    resolved = ResolvingProxy.resolve(catalog, (LayerInfo) l);
                    // special case to handle catalog loading, when nested publishibles might not be
                    // loaded.
                    if (resolved == null) {
                        resolved = l;
                    }
                } else {
                    // Special case for null layer (style group)
                    resolved = ResolvingProxy.resolve(catalog, l);
                }
                layers.set(i, resolved);
            }
        }
    }

    private <I extends CatalogInfo> I resolveProxy(I proxy, CatalogInfo referent) {
        return resolveProxy(proxy, referent, true);
    }

    /**
     * @param proxy the potential {@link ResolvingProxy proxy} reference to resolve
     * @param referent the object holding a reference to {@code proxy}, only used for logging
     * @param fail whether to fail with an {@code IllegalStateException} if the {@code proxy} can't be resolved to an
     *     object in the catalog
     * @return The resolved object, {@code null} if {@code proxy} itself was {@code null}, or {@code fail == false} and
     *     the object didn't resolve
     * @throws IllegalStateException if the {@code proxy} does not resolve to an object in the catalog and {@code fail
     *     == true}
     */
    private <I extends CatalogInfo> I resolveProxy(I proxy, CatalogInfo referent, boolean fail) {
        if (proxy == null) return null;
        I resolved = ResolvingProxy.resolve(catalog, proxy);
        if (resolved == null && fail) {
            String msg = format(
                    "%s[%s] has a missing link to %s[%s]. Object ignored.",
                    referent, referent.getId(), typeOf(proxy), proxy.getId());
            LOGGER.severe(msg);
            throw new IllegalStateException(msg);
        }
        return resolved;
    }

    /** @return a string "type-of" proxy for logging purposes */
    private <I extends CatalogInfo> String typeOf(I proxy) {
        if (proxy instanceof WorkspaceInfo) return "WorkspaceInfo";
        if (proxy instanceof NamespaceInfo) return "NamespaceInfo";
        if (proxy instanceof StoreInfo) return "StoreInfo";
        if (proxy instanceof ResourceInfo) return "ResourceInfo";
        if (proxy instanceof LayerInfo) return "LayerInfo";
        if (proxy instanceof LayerGroupInfo) return "LayerGroupInfo";
        if (proxy instanceof StyleInfo) return "StyleInfo";
        return "unknown type";
    }

    private void loadLayerGroups(Stream<Path> stream) {
        load(stream).forEach(this::addToQueue);
    }

    private Optional<byte[]> load(Path file) {
        return fileWalk.getXStreamLoader().load(file);
    }

    private Stream<byte[]> load(Stream<Path> stream) {
        return stream.parallel().map(this::load).filter(Optional::isPresent).map(Optional::orElseThrow);
    }

    private <C extends CatalogInfo> Optional<C> depersist(Path file) {
        return load(file).flatMap(this::parse);
    }

    private <C extends CatalogInfo> Stream<C> depersist(Stream<Path> stream, Class<C> type) {
        return load(stream)
                .map(this::parse)
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .map(type::cast);
    }

    private <C extends CatalogInfo> Optional<C> parse(byte[] contents) {
        try {
            return Optional.of(fileWalk.getXStreamLoader().parse(contents));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error parsing", e);
            return Optional.empty();
        }
    }

    private void persist(CatalogInfo info, Path path) throws IOException {
        fileWalk.getXStreamLoader().persist(info, path);
    }
}

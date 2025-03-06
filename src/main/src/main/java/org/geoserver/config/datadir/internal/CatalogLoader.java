/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.config.datadir.internal;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.datadir.internal.DataDirectoryWalker.LayerDirectory;
import org.geoserver.config.datadir.internal.DataDirectoryWalker.StoreDirectory;
import org.geoserver.config.datadir.internal.DataDirectoryWalker.WorkspaceDirectory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
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
class CatalogLoader {

    private static final Logger LOGGER =
            Logging.getLogger(CatalogLoader.class.getPackage().getName());

    /** Counter for the number of files successfully read and parsed */
    final AtomicLong readFileCount = new AtomicLong();

    private final DataDirectoryWalker fileWalk;
    private final XStreamLoader xstreamLoader;
    private final ExecutorService executor;
    private final CatalogImpl catalog;

    public CatalogLoader(
            CatalogImpl catalog, DataDirectoryWalker fileWalk, XStreamLoader xstreamLoader, ExecutorService executor) {
        requireNonNull(catalog);
        requireNonNull(fileWalk);
        requireNonNull(xstreamLoader);
        requireNonNull(executor);
        this.catalog = catalog;
        this.fileWalk = fileWalk;
        this.xstreamLoader = xstreamLoader;
        this.executor = executor;
    }

    public void loadCatalog() throws InterruptedException, ExecutionException, FactoryException {
        LOGGER.fine("Forcing initialization of CRS subsystem or it'd fail under concurrency");
        CRS.decode("EPSG:4326");
        Future<?> loadTask = executor.submit(this::readCatalog);
        loadTask.get();
    }

    private void readCatalog() {
        readFileCount.set(0);
        boolean extendedValidation = this.catalog.isExtendedValidation();
        this.catalog.setExtendedValidation(false);

        try {
            loadGlobalStyles();
            loadWorkspaces(fileWalk.workspaces().stream());
            loadGlobalLayerGroups();
        } finally {
            this.catalog.setExtendedValidation(extendedValidation);
        }
        LOGGER.config(format("Depersisted %,d Catalog files.", readFileCount.get()));
    }

    private void loadGlobalStyles() {
        loadStyles(null, fileWalk.globalStyles().stream());
    }

    private void loadWorkspaces(Stream<WorkspaceDirectory> stream) {
        stream.parallel().forEach(this::loadWorkspace);
    }

    private void loadGlobalLayerGroups() {
        loadLayerGroups(fileWalk.globalLayerGroups().stream());
    }

    private void loadWorkspace(WorkspaceDirectory wsdir) {
        Optional<WorkspaceInfo> wsinfo = depersist(wsdir.workspaceFile());
        Optional<NamespaceInfo> nsinfo = depersist(wsdir.namespaceFile());

        if (wsinfo.isPresent() && nsinfo.isPresent()) {
            WorkspaceInfo ws = wsinfo.get();
            NamespaceInfo ns = nsinfo.get();
            add(ws, catalog::add);
            add(ns, catalog::add);

            loadStyles(ws, wsdir.styles().stream());
            loadStores(wsdir.stores());
            loadLayerGroups(wsdir.layerGroups().stream());
        }
    }

    private void loadStyles(@Nullable WorkspaceInfo workspace, Stream<Path> stream) {
        depersist(stream)
                .map(StyleInfo.class::cast)
                .filter(s -> validate(workspace, s))
                .forEach(this::add);
    }

    private boolean validate(@Nullable WorkspaceInfo expectedWorkspace, StyleInfo s) {
        if (null == expectedWorkspace) {
            if (s.getWorkspace() != null) {
                String ws = s.getWorkspace().getId();
                String msg = "Style %s (%s) is expected to have no workspace but has workspace %s. Style ignored.";
                LOGGER.severe(() -> format(msg, s.getName(), s.getId(), ws));
                return false;
            }
        } else {
            if (s.getWorkspace() == null) {
                String msg = "Style %s (%s) is should have workspace %s but has no workspace. Style ignored.";
                LOGGER.severe(() -> format(msg, s.getName(), s.getId(), expectedWorkspace.getName()));
                return false;
            } else if (!expectedWorkspace.getId().equals(s.getWorkspace().getId())) {
                String ws = s.getWorkspace().getId();
                String msg = "Style %s (%s) is should have workspace %s but has workspace %s. Style ignored.";
                LOGGER.severe(() -> format(msg, s.getName(), s.getId(), expectedWorkspace.getName(), ws));
                return false;
            }
        }
        return true;
    }

    private void loadStores(Stream<StoreDirectory> stream) {
        stream.parallel().forEach(this::loadStore);
    }

    private void loadStore(StoreDirectory storeDir) {
        Optional<StoreInfo> store = depersist(storeDir.storeFile);
        store.flatMap(this::add).ifPresent(storeInfo -> loadLayers(storeDir.layers()));
    }

    private void loadLayers(Stream<LayerDirectory> layers) {
        layers.parallel().forEach(this::loadResourceAndLayer);
    }

    private void loadResourceAndLayer(LayerDirectory layerDir) {
        loadResource(layerDir.resourceFile).ifPresent(resource -> loadLayer(layerDir));
    }

    private Optional<ResourceInfo> loadResource(Path resourceFile) {
        Optional<ResourceInfo> resource = depersist(resourceFile);
        return resource.filter(res -> null != res.getStore()).flatMap(this::add);
    }

    private void loadLayer(LayerDirectory layerDir) {
        Optional<LayerInfo> layer = depersist(layerDir.layerFile);
        layer.filter(l -> l.getResource() instanceof ResourceInfo).ifPresent(this::add);
    }

    private Optional<StoreInfo> add(StoreInfo info) {
        return add(info, catalog::add);
    }

    private Optional<ResourceInfo> add(ResourceInfo info) {
        return add(info, catalog::add);
    }

    private void add(LayerInfo info) {
        add(info, catalog::add);
    }

    private void add(LayerGroupInfo info) {
        add(info, catalog::add);
    }

    private void add(StyleInfo info) {
        add(info, catalog::add);
    }

    private <I extends CatalogInfo> Optional<I> add(I info, Consumer<I> saver) {
        try {
            saver.accept(info);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> format("Error saving %s", info));
            return Optional.empty();
        }
        return Optional.of(info);
    }

    private void loadLayerGroups(Stream<Path> stream) {
        depersist(stream).map(LayerGroupInfo.class::cast).forEach(this::add);
    }

    private Stream<CatalogInfo> depersist(Stream<Path> stream) {
        return stream.parallel()
                .map(this::depersist)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CatalogInfo.class::cast);
    }

    private <C extends Info> Optional<C> depersist(Path file) {
        Optional<C> info = xstreamLoader.depersist(file, this.catalog);
        if (info.isPresent()) readFileCount.incrementAndGet();
        return info;
    }
}

/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.datadir.internal;

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
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.datadir.internal.DataDirectoryWalker.LayerDirectory;
import org.geoserver.catalog.datadir.internal.DataDirectoryWalker.StoreDirectory;
import org.geoserver.catalog.datadir.internal.DataDirectoryWalker.WorkspaceDirectory;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;

class CatalogConfigLoader {

    private static final Logger LOGGER =
            Logging.getLogger(CatalogConfigLoader.class.getPackage().getName());

    final AtomicLong readFileCount = new AtomicLong();

    private final DataDirectoryWalker fileWalk;
    private final XStreamLoader xstreamLoader;
    private final ExecutorService executor;
    private final CatalogImpl catalog;

    static {
        try {
            LOGGER.info("Forcing initialization of CRS subsystem or it'd fail under concurrency");
            CRS.decode("EPSG:4326");
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    public CatalogConfigLoader(
            CatalogImpl catalog,
            DataDirectoryWalker fileWalk,
            XStreamLoader xstreamLoader,
            ExecutorService executor) {
        requireNonNull(catalog);
        requireNonNull(fileWalk);
        requireNonNull(xstreamLoader);
        requireNonNull(executor);
        this.catalog = catalog;
        this.fileWalk = fileWalk;
        this.xstreamLoader = xstreamLoader;
        this.executor = executor;
    }

    public CatalogImpl loadCatalog() throws Exception {
        Future<CatalogImpl> loadTask = executor.submit(this::readCatalog);
        try {
            return loadTask.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw ((Error) cause);
            throw e;
        }
    }

    private CatalogImpl readCatalog() throws Exception {
        readFileCount.set(0);
        this.catalog.setExtendedValidation(false);

        loadStyles(fileWalk.globalStyles().stream());
        loadWorkspaces(fileWalk.workspaces().stream());
        loadLayerGroups(fileWalk.globalLayerGroups().stream());

        LOGGER.config(String.format("Depersisted %,d Catalog files.", readFileCount.get()));
        return this.catalog;
    }

    private void loadWorkspaces(Stream<WorkspaceDirectory> stream) {
        stream.parallel().forEach(this::loadWorkspace);
    }

    private void loadWorkspace(WorkspaceDirectory wsdir) {
        Optional<WorkspaceInfo> wsinfo = depersist(wsdir.workspaceFile);
        Optional<NamespaceInfo> nsinfo = depersist(wsdir.namespaceFile);

        if (wsinfo.isPresent() && nsinfo.isPresent()) {
            WorkspaceInfo ws = wsinfo.get();
            NamespaceInfo ns = nsinfo.get();
            save(ws, catalog::add);
            save(ns, catalog::add);

            loadStyles(wsdir.styles().stream());
            loadStores(wsdir.stores());
            loadLayerGroups(wsdir.layerGroups().stream());
        }
    }

    private void loadStyles(Stream<Path> stream) {
        depersist(stream).map(StyleInfo.class::cast).forEach(this::save);
    }

    private void loadStores(Stream<StoreDirectory> stream) {
        stream.parallel().forEach(this::loadStore);
    }

    private void loadStore(StoreDirectory storeDir) {
        Optional<StoreInfo> store = depersist(storeDir.storeFile);
        store.flatMap(this::save)
                .ifPresent(
                        storeInfo -> {
                            loadLayers(storeDir.layers());
                        });
    }

    private void loadLayers(Stream<LayerDirectory> layers) {
        layers.parallel().forEach(this::loadResourceAndLayer);
    }

    private void loadResourceAndLayer(LayerDirectory layerDir) {
        loadResource(layerDir.resourceFile).ifPresent(resource -> loadLayer(layerDir));
    }

    private Optional<ResourceInfo> loadResource(Path resourceFile) {
        Optional<ResourceInfo> resource = depersist(resourceFile);
        return resource.filter(res -> null != res.getStore()).flatMap(this::save);
    }

    private void loadLayer(LayerDirectory layerDir) {
        Optional<LayerInfo> layer = depersist(layerDir.layerFile);
        layer.filter(l -> l.getResource() instanceof ResourceInfo).ifPresent(this::save);
    }

    private Optional<StoreInfo> save(StoreInfo info) {
        return save(info, catalog::add);
    }

    private Optional<ResourceInfo> save(ResourceInfo info) {
        return save(info, catalog::add);
    }

    private void save(LayerInfo info) {
        save(info, catalog::add);
    }

    private void save(LayerGroupInfo info) {
        save(info, catalog::add);
    }

    private void save(StyleInfo info) {
        save(info, catalog::add);
    }

    private <I extends CatalogInfo> Optional<I> save(I info, Consumer<I> saver) {
        try {
            saver.accept(info);
        } catch (Exception e) {
            final String name = (String) OwsUtils.get(info, "name");
            LOGGER.log(
                    Level.WARNING,
                    "Error saving {0} {1}: {2}",
                    new Object[] {info.getClass().getSimpleName(), name, e.getMessage()});
            return Optional.empty();
        }
        return Optional.of(info);
    }

    private void loadLayerGroups(Stream<Path> stream) {
        depersist(stream).map(LayerGroupInfo.class::cast).forEach(this::save);
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

    static void logCatalog(final String loaderName, final Catalog catalog) {
        int workspaces = catalog.count(WorkspaceInfo.class, Filter.INCLUDE);
        int namespaces = catalog.count(NamespaceInfo.class, Filter.INCLUDE);
        int stores = catalog.count(StoreInfo.class, Filter.INCLUDE);
        int resources = catalog.count(ResourceInfo.class, Filter.INCLUDE);
        int layers = catalog.count(LayerInfo.class, Filter.INCLUDE);
        int layergroups = catalog.count(LayerGroupInfo.class, Filter.INCLUDE);
        int styles = catalog.count(StyleInfo.class, Filter.INCLUDE);
        LOGGER.info(
                String.format(
                        "%s:\n\t"
                                + "workspaces: %,d\n\t"
                                + "namespaces: %,d\n\t"
                                + "stores: %,d\n\t"
                                + "resources: %,d\n\t"
                                + "layers: %,d\n\t"
                                + "layerGroups: %,d\n\t"
                                + "styles: %,d\n",
                        loaderName,
                        workspaces,
                        namespaces,
                        stores,
                        resources,
                        layers,
                        layergroups,
                        styles));
    }
}

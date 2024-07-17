/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.internal;

import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.DataDirectoryGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geotools.util.logging.Logging;

/**
 * Provides methods to load both the {@link Catalog} and the {@link GeoServer} config from a data
 * directory with a given parallelism.
 *
 * <p>This is not API, but a collaborator of {@link DataDirectoryGeoServerLoader}
 *
 * @implNote a {@link DataDirectoryWalker} is created and used to delegate the actual loading logic
 *     to the {@link CatalogConfigLoader} and {@link GeoServerConfigLoader} collaborators.
 * @see DataDirectoryWalker
 * @see CatalogConfigLoader
 * @see GeoServerConfigLoader
 * @since 2.26
 */
public class DataDirectoryLoader {

    private static final Logger LOGGER =
            Logging.getLogger(DataDirectoryLoader.class.getPackage().getName());

    private static final AtomicInteger threadPoolId = new AtomicInteger();

    private final FileSystemResourceStore resourceStore;
    private final List<XStreamServiceLoader<ServiceInfo>> serviceLoaders;
    private final XStreamPersisterFactory xpf;

    private ForkJoinPool forkJoinPool;
    private DataDirectoryWalker fileWalker;

    private int parallelism;

    public DataDirectoryLoader(
            FileSystemResourceStore resourceStore,
            List<XStreamServiceLoader<ServiceInfo>> serviceLoaders,
            XStreamPersisterFactory xpf,
            int parallelism) {

        this.resourceStore = resourceStore;
        this.serviceLoaders = serviceLoaders;
        this.xpf = xpf;
        this.parallelism = parallelism;
    }

    private void init() {
        if (null == forkJoinPool) {
            XStreamLoader.setXpf(this.xpf);
            Path dataDirRoot = resourceStore.get("").dir().toPath();
            List<String> serviceFileNames =
                    serviceLoaders.stream()
                            .map(XStreamServiceLoader::getFilename)
                            .collect(Collectors.toList());
            this.fileWalker = new DataDirectoryWalker(dataDirRoot, serviceFileNames);

            final boolean asyncMode = false;
            this.forkJoinPool =
                    new ForkJoinPool(
                            parallelism, threadFactory(), uncaughtExceptionHandler(), asyncMode);
        }
    }

    private DataDirectoryWalker fileWalk() {
        init();
        return fileWalker;
    }

    private ForkJoinPool executor() {
        init();
        return forkJoinPool;
    }

    private ForkJoinWorkerThreadFactory threadFactory() {

        final int poolIndex = threadPoolId.incrementAndGet();
        final AtomicInteger threadIndex = new AtomicInteger();

        return pool -> {
            ForkJoinWorkerThread worker =
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(executor());

            worker.setName(
                    String.format(
                            "DatadirLoader-%d-worker-%d",
                            poolIndex, threadIndex.incrementAndGet()));
            return worker;
        };
    }

    private UncaughtExceptionHandler uncaughtExceptionHandler() {
        return (t, ex) -> {
            String msg =
                    String.format(
                            "Uncaught exception loading catalog or config at thread %s: %s",
                            t.getName(), ex.getMessage());
            LOGGER.log(Level.SEVERE, msg, ex);
        };
    }

    public CatalogImpl loadCatalog(CatalogImpl catalog) throws Exception {
        CatalogConfigLoader loader = new CatalogConfigLoader(catalog, fileWalk(), executor());

        return loader.loadCatalog();
    }

    public GeoServer loadGeoServer(GeoServer gs) throws Exception {
        Objects.requireNonNull(gs);

        GeoServerConfigLoader loader =
                new GeoServerConfigLoader(
                        gs, fileWalk(), executor(), resourceStore, serviceLoaders);

        return loader.loadGeoServer();
    }

    public void dispose() {
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            try {
                fileWalker.dispose();
                forkJoinPool.shutdownNow();
            } finally {
                forkJoinPool = null;
            }
        }
    }
}

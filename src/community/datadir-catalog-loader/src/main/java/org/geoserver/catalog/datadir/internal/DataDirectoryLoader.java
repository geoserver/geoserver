/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir.internal;

import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

/**
 * Provides methods to load both the {@link Catalog} and the {@link GeoServer} config from a data
 * directory, returning new instances of each.
 *
 * <p>The loading process is multi-threaded, and will take place in an {@link Executor} whose
 * parallelism is determined by an heuristic resolving to the minimum between {@code 16} and the
 * number of available processors as reported by {@link Runtime#availableProcessors()}, or
 * overridden by the value passed through the environment variable or system property {@literal
 * DATADIR_LOAD_PARALLELISM}.
 *
 * @implNote a {@link DataDirectoryWalker} is created and used to delegate the actual loading logic
 *     to the {@link CatalogConfigLoader} and {@link GeoServerConfigLoader} collaborators.
 * @see DataDirectoryWalker
 * @see CatalogConfigLoader
 * @see GeoServerConfigLoader
 */
public class DataDirectoryLoader {

    private static final String DATADIR_LOAD_PARALLELISM = "DATADIR_LOAD_PARALLELISM";

    private static final Logger LOGGER =
            Logging.getLogger(DataDirectoryLoader.class.getPackage().getName());

    private final DataDirectoryWalker fileWalk;

    private boolean catalogLoaded, geoserverLoaded;

    private FileSystemResourceStore resourceStore;
    private List<XStreamServiceLoader<ServiceInfo>> serviceLoaders;

    private final ForkJoinPool executor;
    private static final AtomicInteger threadPoolId = new AtomicInteger();

    private final XStreamLoader xstreamLoader;

    public DataDirectoryLoader(
            FileSystemResourceStore resourceStore,
            List<XStreamServiceLoader<ServiceInfo>> serviceLoaders) {

        this.resourceStore = resourceStore;
        this.serviceLoaders = serviceLoaders;
        Path dataDirRoot = resourceStore.get("").dir().toPath();
        List<String> serviceFileNames =
                serviceLoaders.stream()
                        .map(XStreamServiceLoader::getFilename)
                        .collect(Collectors.toList());
        this.fileWalk = new DataDirectoryWalker(dataDirRoot, serviceFileNames);
        this.executor = executor();
        this.xstreamLoader = new XStreamLoader();
    }

    private ForkJoinPool executor() {
        final int parallelism = determineParallelism();
        final boolean asyncMode = false;
        final int poolIndex = threadPoolId.incrementAndGet();
        return new ForkJoinPool(
                parallelism, threadFactory(poolIndex), uncaughtExceptionHandler(), asyncMode);
    }

    private ForkJoinWorkerThreadFactory threadFactory(final int poolIndex) {

        final AtomicInteger threadIndex = new AtomicInteger();

        return pool -> {
            ForkJoinWorkerThread worker =
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(executor);

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

    public CatalogImpl loadCatalog(CatalogImpl catalogImpl) throws Exception {
        CatalogConfigLoader loader =
                new CatalogConfigLoader(catalogImpl, fileWalk, xstreamLoader, executor);
        try {
            CatalogImpl catalog = loader.loadCatalog();
            this.catalogLoaded = true;
            tryDispose();
            return catalog;
        } catch (Exception e) {
            dispose();
            throw e;
        }
    }

    public GeoServerImpl loadGeoServer(Catalog realCatalog) throws Exception {
        Objects.requireNonNull(realCatalog);

        GeoServerImpl gs = new GeoServerImpl();
        // required when depersisting workspace settings and services
        gs.setCatalog(realCatalog);

        GeoServerConfigLoader loader =
                new GeoServerConfigLoader(
                        fileWalk, xstreamLoader, executor, gs, resourceStore, serviceLoaders);

        try {
            GeoServerImpl geoserver = loader.loadGeoServer();
            this.geoserverLoaded = true;
            tryDispose();
            return geoserver;
        } catch (Exception e) {
            dispose();
            throw e;
        }
    }

    public synchronized void dispose() {
        if (!executor.isShutdown()) {
            executor.shutdownNow();
            fileWalk.dispose();
        }
    }

    private void tryDispose() {
        if (catalogLoaded && geoserverLoaded) {
            dispose();
        }
    }

    private int determineParallelism() {
        String configuredParallelism = GeoServerExtensions.getProperty(DATADIR_LOAD_PARALLELISM);
        final int processors = Runtime.getRuntime().availableProcessors();
        final int defParallelism = Math.min(processors, 16);
        int parallelism = defParallelism;
        String logTailMessage = "out of " + processors + " available cores.";
        if (StringUtils.hasText(configuredParallelism)) {
            boolean parseFail = false;
            try {
                parallelism = Integer.parseInt(configuredParallelism);
            } catch (NumberFormatException nfe) {
                parseFail = true;
            }
            if (parseFail || parallelism < 1) {
                parallelism = defParallelism;
                LOGGER.log(
                        Level.WARNING,
                        () ->
                                String.format(
                                        "Configured parallelism is invalid: %s=%s, using default of %d",
                                        DATADIR_LOAD_PARALLELISM,
                                        configuredParallelism,
                                        defParallelism));
            } else {
                logTailMessage =
                        "as indicated by the " + DATADIR_LOAD_PARALLELISM + " environment variable";
            }
        }
        LOGGER.log(
                Level.CONFIG,
                "Catalog and configuration loader uses {0} threads {1}",
                new Object[] {parallelism, logTailMessage});
        return parallelism;
    }
}

/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir.internal;

import static org.geoserver.config.datadir.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_THREADS;

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
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

/**
 * Coordinates the loading of both the {@link Catalog} and the {@link GeoServer} config from a GeoServer data directory,
 * returning new instances of each.
 *
 * <p>This class is the central coordinator for the optimized loading process, managing:
 *
 * <ul>
 *   <li>The thread pool used for parallel loading operations
 *   <li>A single instance of {@link DataDirectoryWalker} for efficient directory traversal
 *   <li>Delegation to specialized loader classes for catalog and config
 *   <li>Resource cleanup after loading completes
 * </ul>
 *
 * <p>The loading process is multi-threaded, and will take place in a {@link ForkJoinPool} whose parallelism is
 * determined by an heuristic resolving to the minimum between {@code 16} and the number of available processors as
 * reported by {@link Runtime#availableProcessors()}, or overridden by the value passed through the environment variable
 * or system property {@literal GEOSERVER_DATA_DIR_LOADER_THREADS}.
 *
 * @implNote This class creates a {@link DataDirectoryWalker} for efficient directory traversal and delegates the actual
 *     loading logic to the {@link CatalogLoader} and {@link ConfigLoader} collaborators. The directory walker is reused
 *     between catalog and config loading to avoid redundant filesystem operations. The class also manages the shared
 *     thread pool, configuring it with custom thread naming and exception handling.
 * @see DataDirectoryWalker
 * @see CatalogLoader
 * @see ConfigLoader
 * @see XStreamLoader
 */
public class DataDirectoryLoader {

    private static final Logger LOGGER =
            Logging.getLogger(DataDirectoryLoader.class.getPackage().getName());

    private final DataDirectoryWalker fileWalk;

    private boolean catalogLoaded;
    private boolean geoserverLoaded;

    private GeoServerDataDirectory dataDirectory;
    private List<XStreamServiceLoader<ServiceInfo>> serviceLoaders;

    private final ForkJoinPool executor;
    private static final AtomicInteger threadPoolId = new AtomicInteger();

    private final XStreamLoader xstreamLoader;

    public DataDirectoryLoader(
            GeoServerDataDirectory dataDirectory,
            List<XStreamServiceLoader<ServiceInfo>> serviceLoaders,
            XStreamPersisterFactory xpfac) {

        this.dataDirectory = dataDirectory;
        this.serviceLoaders = serviceLoaders;
        this.fileWalk = createWalker(dataDirectory, serviceLoaders);
        this.executor = createExecutor();
        this.xstreamLoader = new XStreamLoader(xpfac);
    }

    private DataDirectoryWalker createWalker(
            GeoServerDataDirectory dataDirectory, List<XStreamServiceLoader<ServiceInfo>> serviceLoaders) {
        Path dataDirRoot = dataDirectory.getRoot().dir().toPath();
        List<String> serviceFileNames = resolveServiceFileNames(serviceLoaders);
        return new DataDirectoryWalker(dataDirRoot, serviceFileNames);
    }

    private List<String> resolveServiceFileNames(List<XStreamServiceLoader<ServiceInfo>> serviceLoaders) {
        return serviceLoaders.stream().map(XStreamServiceLoader::getFilename).collect(Collectors.toList());
    }

    private ForkJoinPool createExecutor() {
        final int parallelism = determineParallelism();
        final boolean asyncMode = false;
        final int poolIndex = threadPoolId.incrementAndGet();
        return new ForkJoinPool(parallelism, threadFactory(poolIndex), uncaughtExceptionHandler(), asyncMode);
    }

    private ForkJoinWorkerThreadFactory threadFactory(final int poolIndex) {

        final AtomicInteger threadIndex = new AtomicInteger();

        return pool -> {
            ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(executor);

            worker.setName(String.format("DatadirLoader-%d-worker-%d", poolIndex, threadIndex.incrementAndGet()));
            return worker;
        };
    }

    private UncaughtExceptionHandler uncaughtExceptionHandler() {
        return (t, ex) -> {
            String msg = String.format(
                    "Uncaught exception loading catalog or config at thread %s: %s", t.getName(), ex.getMessage());
            LOGGER.log(Level.SEVERE, msg, ex);
        };
    }

    public void loadCatalog(CatalogImpl catalogImpl) throws Exception {
        CatalogLoader loader = new CatalogLoader(catalogImpl, fileWalk, xstreamLoader, executor);
        try {
            loader.loadCatalog();
            this.catalogLoaded = true;
            tryDispose();
        } catch (Exception e) {
            dispose();
            throw e;
        }
    }

    public void loadGeoServer(GeoServer gs) throws Exception {
        Objects.requireNonNull(gs);

        ConfigLoader loader = new ConfigLoader(fileWalk, xstreamLoader, executor, gs, dataDirectory, serviceLoaders);

        try {
            loader.loadGeoServer();
            this.geoserverLoaded = true;
            tryDispose();
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
        String configuredParallelism = GeoServerExtensions.getProperty(GEOSERVER_DATA_DIR_LOADER_THREADS);
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
                        () -> String.format(
                                "Configured parallelism is invalid: %s=%s, using default of %d",
                                GEOSERVER_DATA_DIR_LOADER_THREADS, configuredParallelism, defParallelism));
            } else {
                logTailMessage = "as indicated by the " + GEOSERVER_DATA_DIR_LOADER_THREADS
                        + " environment variable or System property";
            }
        }
        LOGGER.log(Level.CONFIG, "Catalog and configuration loader uses {0} threads {1}", new Object[] {
            parallelism, logTailMessage
        });
        return parallelism;
    }
}

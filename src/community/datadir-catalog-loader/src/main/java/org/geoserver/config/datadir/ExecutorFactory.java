/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static java.lang.String.format;
import static org.geoserver.config.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_THREADS;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

class ExecutorFactory {

    private static final Logger LOGGER =
            Logging.getLogger(ExecutorFactory.class.getPackage().getName());

    private static final AtomicInteger threadPoolId = new AtomicInteger();

    private ExecutorFactory() {
        // private constructor, utility class
    }

    public static ForkJoinPool createExecutor() {
        final int parallelism = determineParallelism();
        final boolean asyncMode = false;
        return new ForkJoinPool(parallelism, threadFactory(), uncaughtExceptionHandler(), asyncMode);
    }

    private static ForkJoinWorkerThreadFactory threadFactory() {

        final int poolIndex = threadPoolId.incrementAndGet();
        final AtomicInteger threadIndex = new AtomicInteger();

        return pool -> {
            ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName(String.format("DatadirLoader-%d-worker-%d", poolIndex, threadIndex.incrementAndGet()));
            return worker;
        };
    }

    /** @return an UncaughtExceptionHandler to log an error message in case of unrecoverable exception */
    private static UncaughtExceptionHandler uncaughtExceptionHandler() {
        return (t, ex) -> {
            String msg = format(
                    "Uncaught exception loading catalog or config at thread %s: %s", t.getName(), ex.getMessage());
            LOGGER.log(Level.SEVERE, msg, ex);
        };
    }

    private static int determineParallelism() {
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

/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static java.lang.String.format;
import static org.geoserver.config.datadir.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_THREADS;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * Utility class for creating and configuring thread pools used by the data directory loader.
 *
 * <p>This factory creates {@link ForkJoinPool} instances with appropriate naming conventions, exception handling, and
 * parallelism levels based on system configuration.
 *
 * @see DataDirectoryGeoServerLoader#GEOSERVER_DATA_DIR_LOADER_THREADS
 */
class ExecutorFactory {

    private static final Logger LOGGER =
            Logging.getLogger(ExecutorFactory.class.getPackage().getName());

    /** Counter used to generate unique identifiers for thread pools */
    private static final AtomicInteger threadPoolId = new AtomicInteger();

    /** Private constructor to prevent instantiation of this utility class. */
    private ExecutorFactory() {
        // private constructor, utility class
    }

    /**
     * Creates a new {@link ForkJoinPool} configured for data directory loading tasks.
     *
     * <p>The parallelism level is determined based on system properties and available processors. The pool uses custom
     * thread naming and exception handling to facilitate debugging.
     *
     * @param admin Admin authentication used to propagate it to the ForkjoinPool threads
     * @return a configured {@link ForkJoinPool} instance ready to execute data directory loading tasks
     */
    public static ForkJoinPool createExecutor(Authentication admin) {
        final int parallelism = determineParallelism();
        final boolean asyncMode = false;
        return new ForkJoinPool(parallelism, threadFactory(admin), uncaughtExceptionHandler(), asyncMode);
    }

    /**
     * Creates a thread factory that produces worker threads with descriptive names.
     *
     * <p>The thread names follow the pattern "DatadirLoader-{poolId}-worker-{threadId}" to help with identification
     * during debugging.
     *
     * @param admin Admin authentication used to propagate it to the ForkjoinPool threads
     * @return a thread factory for creating worker threads
     */
    private static ForkJoinWorkerThreadFactory threadFactory(Authentication admin) {

        final int poolIndex = threadPoolId.incrementAndGet();
        final AtomicInteger threadIndex = new AtomicInteger();

        return pool -> {
            SecurityContextHolder.getContext().setAuthentication(admin);
            ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName(String.format("DatadirLoader-%d-worker-%d", poolIndex, threadIndex.incrementAndGet()));
            return worker;
        };
    }

    /**
     * Creates an exception handler to properly log unhandled exceptions in worker threads.
     *
     * @return an UncaughtExceptionHandler to log an error message in case of unrecoverable exception
     */
    private static UncaughtExceptionHandler uncaughtExceptionHandler() {
        return (t, ex) -> {
            String msg = format(
                    "Uncaught exception loading catalog or config at thread %s: %s", t.getName(), ex.getMessage());
            LOGGER.log(Level.SEVERE, msg, ex);
        };
    }

    /**
     * Determines the appropriate parallelism level for the thread pool.
     *
     * <p>This method considers:
     *
     * <ul>
     *   <li>The {@code GEOSERVER_DATA_DIR_LOADER_THREADS} system property or environment variable
     *   <li>The number of available processors on the system
     *   <li>A sensible default and maximum to prevent resource exhaustion
     * </ul>
     *
     * @return the number of threads to use in the pool
     */
    static int determineParallelism() {
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
            } else if (parallelism > processors) {
                parallelism = processors;
                LOGGER.log(
                        Level.WARNING,
                        () -> String.format(
                                "Configured parallelism is invalid: %s=%s, using maximum of %d as per available processors",
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

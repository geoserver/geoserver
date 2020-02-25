/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.platform.ExtensionPriority;
import org.geotools.image.io.ImageIOExt;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;

/**
 * Initializes Coverage Access settings from configuration.
 *
 * @author Daniele Romagnoli, GeoSolutions
 */
public class CoverageAccessInitializer implements GeoServerInitializer, ExtensionPriority {

    GeoServer gs;

    public void initialize(GeoServer geoServer) throws Exception {
        this.gs = geoServer;
        final GeoServerInfo geoserverInfo = geoServer.getGlobal();
        CoverageAccessInfo coverageAccess = geoserverInfo.getCoverageAccess();
        if (coverageAccess == null) {
            coverageAccess = new CoverageAccessInfoImpl();
            geoserverInfo.setCoverageAccess(coverageAccess);
        }
        initCoverage(coverageAccess);

        geoServer.addListener(
                new ConfigurationListenerAdapter() {

                    public void handleGlobalChange(
                            GeoServerInfo global,
                            List<String> propertyNames,
                            List<Object> oldValues,
                            List<Object> newValues) {
                        if (propertyNames.contains("coverageAccess")) {
                            // Make sure to proceed with coverageAccess init
                            // only in case the global change involved that section
                            initCoverage(global.getCoverageAccess());
                        }
                    }

                    @Override
                    public void handlePostGlobalChange(GeoServerInfo global) {
                        // No need to handle that change too
                    }
                });
    }

    void initCoverage(CoverageAccessInfo coverageAccess) {
        if (coverageAccess != null) {
            ThreadPoolExecutor executor = coverageAccess.getThreadPoolExecutor();

            // First initialization
            if (executor == null) {
                final Hints defHints = GeoTools.getDefaultHints();

                // Looking for executor from default Hints
                if (defHints != null && defHints.containsKey(Hints.EXECUTOR_SERVICE)) {
                    executor = (ThreadPoolExecutor) defHints.get(Hints.EXECUTOR_SERVICE);
                }
            }
            if (executor == null) {
                // No Executor found: Create a new one
                executor =
                        new ThreadPoolExecutor(
                                coverageAccess.getCorePoolSize(),
                                coverageAccess.getMaxPoolSize(),
                                coverageAccess.getKeepAliveTime(),
                                TimeUnit.MILLISECONDS,
                                coverageAccess.getQueueType() == QueueType.UNBOUNDED
                                        ? new LinkedBlockingQueue<Runnable>()
                                        : new SynchronousQueue<Runnable>());
                coverageAccess.setThreadPoolExecutor(executor);
            } else {

                // //
                //
                // Overriding values
                //
                // //
                final BlockingQueue<Runnable> queue = executor.getQueue();
                final QueueType queueType = coverageAccess.getQueueType();

                // If the queue type is the same, I can simply override the parameter settings.
                if ((queue instanceof LinkedBlockingQueue && queueType == QueueType.UNBOUNDED)
                        || (queue instanceof SynchronousQueue && queueType == QueueType.DIRECT)) {
                    executor.setCorePoolSize(coverageAccess.getCorePoolSize());
                    executor.setMaximumPoolSize(coverageAccess.getMaxPoolSize());
                    executor.setKeepAliveTime(
                            coverageAccess.getKeepAliveTime(), TimeUnit.MILLISECONDS);
                    coverageAccess.setThreadPoolExecutor(executor);
                } else {

                    // The queue type has been changed. Initializing a new ThreadPoolExecutor by
                    // previously shutting down the current one
                    executor.shutdown();
                    if (!executor.isTerminated()) {
                        executor.shutdownNow();
                    }

                    executor =
                            new ThreadPoolExecutor(
                                    coverageAccess.getCorePoolSize(),
                                    coverageAccess.getMaxPoolSize(),
                                    coverageAccess.getKeepAliveTime(),
                                    TimeUnit.MILLISECONDS,
                                    coverageAccess.getQueueType() == QueueType.DIRECT
                                            ? new SynchronousQueue<Runnable>()
                                            : new LinkedBlockingQueue<Runnable>());
                    coverageAccess.setThreadPoolExecutor(executor);
                }
            }

            // set the executor in the resource pool
            gs.getCatalog().getResourcePool().setCoverageExecutor(executor);

            // setup the memory/file system cache usage threshold
            ImageIOExt.setFilesystemThreshold(coverageAccess.getImageIOCacheThreshold() * 1024);
        }
    }

    public int getPriority() {
        return 0;
    }
}

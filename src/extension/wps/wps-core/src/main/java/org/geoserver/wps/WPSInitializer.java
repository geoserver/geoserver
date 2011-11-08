package org.geoserver.wps;

import java.util.List;

import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.wps.executor.DefaultProcessManager;
import org.geoserver.wps.executor.WPSExecutionManager;

/**
 * Initializes WPS functionality from configuration.
 * 
 * @author Andrea Aime, GeoSolutions
 */
public class WPSInitializer implements GeoServerInitializer {

    WPSExecutionManager executionManager;

    DefaultProcessManager processManager;

    WPSStorageCleaner cleaner;

    public WPSInitializer(WPSExecutionManager executionManager,
            DefaultProcessManager processManager, WPSStorageCleaner cleaner) {
        this.executionManager = executionManager;
        this.processManager = processManager;
        this.cleaner = cleaner;
    }

    public void initialize(final GeoServer geoServer) throws Exception {
        initWPS(geoServer.getService(WPSInfo.class));

        geoServer.addListener(new ConfigurationListenerAdapter() {

            public void handleGlobalChange(GeoServerInfo global, List<String> propertyNames,
                    List<Object> oldValues, List<Object> newValues) {

                initWPS(geoServer.getService(WPSInfo.class));
            }

            @Override
            public void handlePostGlobalChange(GeoServerInfo global) {
                initWPS(geoServer.getService(WPSInfo.class));
            }
        });
    }

    void initWPS(WPSInfo info) {
        // Handle the http connection timeout.
        // The specified timeout is in seconds. Convert it to milliseconds
        double timeout = info.getConnectionTimeout();
        if (timeout > 0) {
            executionManager.setConnectionTimeout((int) timeout * 1000);
        } else {
            // specified timeout == -1 represents infinite timeout.
            // by convention, for infinite URLConnection timeouts, we need to use zero.
            executionManager.setConnectionTimeout(0);
        }

        // handle the resource expiration timeout
        timeout = info.getResourceExpirationTimeout();
        if (timeout > 0) {
            cleaner.setExpirationDelay((int) timeout * 1000);
        } else {
            // specified timeout == -1, so we use the default of five minutes
            cleaner.setExpirationDelay(5 * 60 * 1000);
        }

        // the max number of synch proceesses
        int defaultMaxProcesses = Runtime.getRuntime().availableProcessors() * 2;
        int maxSynch = info.getMaxSynchronousProcesses();
        if (maxSynch > 0) {
            processManager.setMaxSynchronousProcesses(maxSynch);
        } else {
            processManager.setMaxSynchronousProcesses(defaultMaxProcesses);
        }

        // the max number of asynch proceesses
        int maxAsynch = info.getMaxAsynchronousProcesses();
        if (maxAsynch > 0) {
            processManager.setMaxAsynchronousProcesses(maxAsynch);
        } else {
            processManager.setMaxAsynchronousProcesses(defaultMaxProcesses);
        }
    }
}

/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.wps.executor.DefaultProcessManager;
import org.geoserver.wps.executor.WPSExecutionManager;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;

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
        initWPS(geoServer.getService(WPSInfo.class), geoServer);

        geoServer.addListener(new ConfigurationListenerAdapter() {
            @Override
            public void handlePostGlobalChange(GeoServerInfo global) {
                initWPS(geoServer.getService(WPSInfo.class), geoServer);
            }
        });
    }

    void initWPS(WPSInfo info, GeoServer geoServer) {
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

        lookupNewProcessGroups(info, geoServer);
    }

    static void lookupNewProcessGroups(WPSInfo info, GeoServer geoServer) {
        List<ProcessGroupInfo> newGroups = new ArrayList();

        for (ProcessGroupInfo available : lookupProcessGroups()) {
            boolean found = false;
            for (ProcessGroupInfo configured : info.getProcessGroups()) {
                if (configured.getFactoryClass().equals(available.getFactoryClass())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                //add it
                newGroups.add(available);
            }
        }

        //only save if we have anything new to add
        if (!newGroups.isEmpty()) {
            info.getProcessGroups().addAll(newGroups);
            geoServer.save(info);
        }
    }

    static List<ProcessGroupInfo> lookupProcessGroups() {
        List<ProcessGroupInfo> processFactories = new ArrayList<ProcessGroupInfo>();
        
        // here we build a full list of process factories infos, covering all available
        // factories: this makes sure the available factories are availablefrom both
        // GUI and REST configuration
        
        // get the full list of factories
        List<ProcessFactory> factories = new ArrayList<ProcessFactory>(Processors.getProcessFactories());
        
        // ensure there is a stable order across invocations, JDK and so on
        Collections.sort(factories, new Comparator<ProcessFactory>() {

            @Override
            public int compare(ProcessFactory o1, ProcessFactory o2) {
                if(o1 == null) {
                    return o2 == null ? 0 : -1;
                } else if(o2 == null) {
                    return 1;
                } else {
                    return o1.getClass().getName().compareTo(o2.getClass().getName());
                }
            }
            
        });
        
        // build the result, adding the ProcessFactoryInfo as necessary for the factories
        // that do not already have a configuration
        for (final ProcessFactory pf : factories) {
            ProcessGroupInfo pfi = new ProcessGroupInfoImpl();
            pfi.setEnabled(true);
            pfi.setFactoryClass(pf.getClass());
            
            processFactories.add(pfi);
        }
        
        return processFactories;
    }
}

/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jai;

import it.geosolutions.jaiext.JAIExt;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.JAIEXTInfo;
import org.geoserver.config.JAIInfo;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.image.ImageWorker;
import org.geotools.image.jai.Registry;

/**
 * Initializes JAI functionality from configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class JAIInitializer implements GeoServerInitializer {

    public void initialize(GeoServer geoServer) throws Exception {
        initJAI(geoServer.getGlobal().getJAI());

        geoServer.addListener(
                new ConfigurationListenerAdapter() {

                    public void handleGlobalChange(
                            GeoServerInfo global,
                            List<String> propertyNames,
                            List<Object> oldValues,
                            List<Object> newValues) {

                        if (propertyNames.contains(
                                "jAI")) { // TODO: check why the propertyname is reported as jAI
                            // instead of JAI
                            // Make sure to proceed with JAI init
                            // only in case the global change involved that section
                            initJAI(global.getJAI());
                        }
                    }
                });
    }

    void initJAI(JAIInfo jai) {

        JAI jaiDef = JAI.getDefaultInstance();
        jai.setJAI(jaiDef);

        // JAIEXT initialization
        if (ImageWorker.isJaiExtEnabled()) {
            if (jai.getJAIEXTInfo() != null) {
                JAIEXTInfo jaiext = jai.getJAIEXTInfo();
                Set<String> jaiOperations = jaiext.getJAIOperations();
                Set<String> jaiExtOperations = jaiext.getJAIEXTOperations();
                if (jaiOperations != null && !jaiOperations.isEmpty()) {
                    JAIExt.registerOperations(jaiOperations, false);
                    for (String opName : jaiOperations) {
                        // Remove operations with old descriptors
                        CoverageProcessor.removeOperationFromProcessors(opName);
                        JAIExt.setJAIAcceleration(opName, true);
                    }
                }
                if (jaiExtOperations != null && !jaiExtOperations.isEmpty()) {
                    Set<String> newJai = new TreeSet<String>(jaiExtOperations);
                    if (jaiOperations != null && !jaiOperations.isEmpty()) {
                        newJai.removeAll(jaiOperations);
                    }
                    for (String opName : newJai) {
                        if (!JAIExt.isJAIExtOperation(opName)) {
                            // Remove operations with old descriptors
                            CoverageProcessor.removeOperationFromProcessors(opName);
                        }
                    }
                    JAIExt.registerOperations(newJai, true);
                }
                // Update all the CoverageProcessor instances
                CoverageProcessor.updateProcessors();
            }
        }

        //

        // setting JAI wide hints
        jaiDef.setRenderingHint(JAI.KEY_CACHED_TILE_RECYCLING_ENABLED, jai.isRecycling());

        // tile factory and recycler
        if (jai.isRecycling()
                && !(jaiDef.getRenderingHint(JAI.KEY_TILE_FACTORY)
                        instanceof ConcurrentTileFactory)) {
            final ConcurrentTileFactory recyclingFactory = new ConcurrentTileFactory();
            jaiDef.setRenderingHint(JAI.KEY_TILE_FACTORY, recyclingFactory);
            jaiDef.setRenderingHint(JAI.KEY_TILE_RECYCLER, recyclingFactory);
        } else {
            if (!jai.isRecycling()) {
                final PassThroughTileFactory passThroughFactory = new PassThroughTileFactory();
                jaiDef.setRenderingHint(JAI.KEY_TILE_FACTORY, passThroughFactory);
                jaiDef.setRenderingHint(JAI.KEY_TILE_RECYCLER, passThroughFactory);
            }
        }

        // Setting up Cache Capacity
        TileCache jaiCache = jaiDef.getTileCache();
        jai.setTileCache(jaiCache);

        long jaiMemory = (long) (jai.getMemoryCapacity() * Runtime.getRuntime().maxMemory());
        jaiCache.setMemoryCapacity(jaiMemory);

        // Setting up Cache Threshold
        jaiCache.setMemoryThreshold((float) jai.getMemoryThreshold());

        jaiDef.getTileScheduler().setParallelism(jai.getTileThreads());
        jaiDef.getTileScheduler().setPrefetchParallelism(jai.getTileThreads());
        jaiDef.getTileScheduler().setPriority(jai.getTilePriority());
        jaiDef.getTileScheduler().setPrefetchPriority(jai.getTilePriority());

        // Workaround for native mosaic BUG
        Registry.setNativeAccelerationAllowed("Mosaic", jai.isAllowNativeMosaic(), jaiDef);
        // Workaround for native Warp BUG
        Registry.setNativeAccelerationAllowed("Warp", jai.isAllowNativeWarp(), jaiDef);
    }
}

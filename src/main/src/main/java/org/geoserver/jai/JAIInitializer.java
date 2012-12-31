/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jai;

import java.util.List;

import javax.media.jai.JAI;

import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.JAIInfo;
import org.geotools.image.jai.Registry;

import com.sun.media.jai.util.SunTileCache;

/**
 * Initializes JAI functionality from configuration.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 * TODO: we should figure out if we want JAI to be core to the model or a plugin
 * ... right now it is both
 *
 */
public class JAIInitializer implements GeoServerInitializer {

    public void initialize(GeoServer geoServer) throws Exception {
        initJAI( geoServer.getGlobal().getJAI() );
        
        geoServer.addListener( new ConfigurationListenerAdapter() {

            public void handleGlobalChange(GeoServerInfo global,
                    List<String> propertyNames, List<Object> oldValues,
                    List<Object> newValues) {
                
                if (propertyNames.contains("jAI")) {//TODO: check why the propertyname is reported as jAI instead of JAI
                    // Make sure to proceed with JAI init
                    // only in case the global change involved that section
                    initJAI(global.getJAI() );
                }
            }
        });
    }

    void initJAI(JAIInfo jai) {
        
        JAI jaiDef = JAI.getDefaultInstance();
        jai.setJAI( jaiDef );
        
        // setup concurrent operation registry
        if(!(jaiDef.getOperationRegistry() instanceof ConcurrentOperationRegistry)) {
            jaiDef.setOperationRegistry(ConcurrentOperationRegistry.initializeRegistry());
        }
        
        // setting JAI wide hints
        jaiDef.setRenderingHint(JAI.KEY_CACHED_TILE_RECYCLING_ENABLED, jai.isRecycling());
        
        // tile factory and recycler
        if(jai.isRecycling() && !(jaiDef.getRenderingHint(JAI.KEY_TILE_FACTORY) instanceof ConcurrentTileFactory)) {
            final ConcurrentTileFactory recyclingFactory = new ConcurrentTileFactory();
            jaiDef.setRenderingHint(JAI.KEY_TILE_FACTORY, recyclingFactory);
            jaiDef.setRenderingHint(JAI.KEY_TILE_RECYCLER, recyclingFactory);
        }
        
        // Setting up Cache Capacity
        SunTileCache jaiCache = (SunTileCache) jaiDef.getTileCache();
        jai.setTileCache( jaiCache );
        
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
    }
}

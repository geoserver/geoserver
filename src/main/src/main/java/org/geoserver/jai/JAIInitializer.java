package org.geoserver.jai;

import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.RecyclingTileFactory;

import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.JAIInfo;
import org.geotools.image.io.ImageIOExt;
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
                
                initJAI( global.getJAI() );
            }
            
            @Override
            public void handlePostGlobalChange(GeoServerInfo global) {
                initJAI(global.getJAI());
            }
        });
    }

    void initJAI(JAIInfo jai) {
        
        JAI jaiDef = JAI.getDefaultInstance();
        jai.setJAI( jaiDef );
        
        // setting JAI wide hints
        jaiDef.setRenderingHint(JAI.KEY_CACHED_TILE_RECYCLING_ENABLED, jai.isRecycling());
        
        // tile factory and recycler
        if(jai.isRecycling()) {
            final RecyclingTileFactory recyclingFactory = new RecyclingTileFactory();
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

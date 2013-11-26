/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.GeoServer;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.logging.Logging;

/**
 * Internal Catalog Store that automatically loads mappings from mapping files in GeoServer Data Directory.
 * 
 * @author Niels Charlier
 *
 */
public class GeoServerInternalCatalogStore extends InternalCatalogStore {
    
    protected static final Logger LOGGER = Logging.getLogger(GeoServerInternalCatalogStore.class);
    
    protected Map<String, PropertyFileWatcher> watchers = new HashMap<String, PropertyFileWatcher>();
    
    /**
     * Get Mapping, update from file if changed
     * 
     * @param typeName
     * @return the mapping
     */
    public CatalogStoreMapping getMapping(String typeName) {
        
        PropertyFileWatcher watcher = watchers.get(typeName);
        
        if (watcher!=null && watcher.isModified()  ) {
            try {
                addMapping (typeName, CatalogStoreMapping.parse(new HashMap<String, String>((Map) watcher.getProperties())));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e.toString());
            }
        }
        
        return super.getMapping( typeName);
    }

    /**
     * Create GeoServerInternalCatalogStore
     * 
     * @param geoserver
     * @throws IOException
     */
    public GeoServerInternalCatalogStore(GeoServer geoserver) throws IOException {
        super( geoserver.getCatalog());
        GeoServerResourceLoader loader = geoserver.getCatalog().getResourceLoader();
        File dir = loader.findOrCreateDirectory("csw");
        for (String typeName : descriptorByType.keySet()) {
            File f = new File(dir, typeName + ".properties");

            PropertyFileWatcher watcher = new PropertyFileWatcher(f);
            watchers.put(typeName, watcher);
            
            if (!f.exists()) {           
                IOUtils.copy(getClass().getResourceAsStream(typeName + ".default.properties"),f);
            }
            
            addMapping (typeName, CatalogStoreMapping.parse(new HashMap<String, String>((Map) watcher.getProperties())));
        }
    }       
    
}

/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;

/**
 * Internal Catalog Store that automatically loads mappings from mapping files in GeoServer Data Directory.
 * 
 * @author Niels Charlier
 *
 */
public class GeoServerInternalCatalogStore extends InternalCatalogStore {

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
        for (File f : dir.listFiles()) {       
            Properties properties = new Properties();
            FileInputStream in = new FileInputStream(f);
            properties.load(in);
            in.close();
            addMapping (f.getName(), CatalogStoreMapping.parse(new HashMap<String, String>((Map) properties)));
        }
    }       
    
}

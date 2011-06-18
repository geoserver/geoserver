package org.geoserver.spatialite;

import java.io.File;
import java.io.IOException;

import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.spatialite.SpatiaLiteDataStoreFactory;

/**
 * Initializes an H2 data store factory setting its location to the geoserver
 *  data directory.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class SpatiaLiteDataStoreFactoryInitializer extends 
    DataStoreFactoryInitializer<SpatiaLiteDataStoreFactory> {

    GeoServerResourceLoader resourceLoader;
    
    public SpatiaLiteDataStoreFactoryInitializer() {
        super( SpatiaLiteDataStoreFactory.class );
    }
    
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    public void initialize(SpatiaLiteDataStoreFactory factory) {
        //create an h2 directory
        File h2;
        try {
            h2 = resourceLoader.findOrCreateDirectory("spatialite");
        } 
        catch (IOException e) {
            throw new RuntimeException("Unable to create spatialite directory", e);
        }
        
        factory.setBaseDirectory( h2 );
    }
}
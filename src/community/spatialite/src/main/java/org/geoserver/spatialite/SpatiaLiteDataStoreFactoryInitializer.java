package org.geoserver.spatialite;

import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.spatialite.SpatiaLiteDataStoreFactory;

public class SpatiaLiteDataStoreFactoryInitializer 
    extends DataStoreFactoryInitializer<SpatiaLiteDataStoreFactory> {

    GeoServerResourceLoader resourceLoader;

    protected SpatiaLiteDataStoreFactoryInitializer() {
        super(SpatiaLiteDataStoreFactory.class);
    }

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    @Override
    public void initialize(SpatiaLiteDataStoreFactory factory) {
        factory.setBaseDirectory(resourceLoader.getBaseDirectory());
    }

}

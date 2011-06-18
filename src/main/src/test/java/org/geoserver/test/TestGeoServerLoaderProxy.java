package org.geoserver.test;

import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.LegacyGeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.context.ApplicationContext;

public class TestGeoServerLoaderProxy extends GeoServerLoaderProxy {

    public TestGeoServerLoaderProxy(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    @Override
    protected GeoServerLoader lookupGeoServerLoader(ApplicationContext appContext) {
        return new LegacyGeoServerLoader(resourceLoader);
    }

}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.geopkg.GeoPkgDataStoreFactory;

/**
 * Initializes an GeoPkg data store factory setting its location to the geoserver data directory.
 *
 * @author Justin Deoliveira, Boundless
 */
public class GeoPkgDataStoreFactoryInitializer
        extends DataStoreFactoryInitializer<GeoPkgDataStoreFactory> {

    GeoServerResourceLoader resourceLoader;

    public GeoPkgDataStoreFactoryInitializer() {
        super(GeoPkgDataStoreFactory.class);
    }

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void initialize(GeoPkgDataStoreFactory factory) {
        factory.setBaseDirectory(resourceLoader.getBaseDirectory());
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;

/**
 * Listener interface for {@linkplain DefaultGeoServerLoader} load catalog and geoServer startup
 * process.
 */
public interface GeoServerLoaderListener {

    public static final GeoServerLoaderListener EMPTY_LISTENER = new EmptyGeoServerLoaderListener();

    /**
     * Listener method executed after GeoServer catalog loading procedure.
     *
     * @param catalog GeoServer catalog instance
     * @param xp {@linkplain XStreamPersister} instance
     */
    void loadCatalog(Catalog catalog, XStreamPersister xp);

    /**
     * Listener method executed after GeoServer setup loading procedure.
     *
     * @param geoServer GeoServer instance
     * @param xp {@linkplain XStreamPersister} instance
     */
    void loadGeoServer(GeoServer geoServer, XStreamPersister xp);

    /** Empty implementation to mark there is no listener active. */
    public static final class EmptyGeoServerLoaderListener implements GeoServerLoaderListener {

        @Override
        public void loadCatalog(Catalog catalog, XStreamPersister xp) {}

        @Override
        public void loadGeoServer(GeoServer geoServer, XStreamPersister xp) {}
    }
}

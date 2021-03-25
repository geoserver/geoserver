/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;

/**
 * Testing utility listener to check the roles involved on catalog and geoserver startup loading
 * process. Defined in testing applicationContext.xml file as id="rootStartupListener".
 */
public class RootStartupListener implements GeoServerLoaderListener {

    private static volatile GeoServerLoaderListener delegate =
            GeoServerLoaderListener.EMPTY_LISTENER;

    @Override
    public void loadCatalog(Catalog catalog, XStreamPersister xp) {
        delegate.loadCatalog(catalog, xp);
    }

    @Override
    public void loadGeoServer(GeoServer geoServer, XStreamPersister xp) {
        delegate.loadGeoServer(geoServer, xp);
    }

    public static void setListener(GeoServerLoaderListener listener) {
        delegate = listener;
    }
}

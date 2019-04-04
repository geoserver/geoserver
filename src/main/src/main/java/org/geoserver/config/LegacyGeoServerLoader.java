/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.util.LegacyCatalogImporter;
import org.geoserver.config.util.LegacyConfigurationImporter;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerResourceLoader;

/**
 * Extension of GeoServerLoader which uses the legacy (1.x) style data directory to load
 * configuration.
 *
 * @author Justin Deoliveira, OpenGEO
 */
public class LegacyGeoServerLoader extends DefaultGeoServerLoader {

    public LegacyGeoServerLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    @Override
    protected void readCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        catalog.setResourceLoader(resourceLoader);

        // look for legacy catalog.xml
        File f = resourceLoader.find("catalog.xml");
        if (f != null) {
            LegacyCatalogImporter catalogImporter = new LegacyCatalogImporter();
            catalogImporter.setResourceLoader(resourceLoader);
            catalogImporter.setCatalog(catalog);

            catalogImporter.imprt(resourceLoader.getBaseDirectory());
        } else {
            LOGGER.warning("No catalog file found.");
        }
    }

    @Override
    protected void readConfiguration(GeoServer geoServer, XStreamPersister xp) throws Exception {
        // look for legacy services.xml
        File f = resourceLoader.find("services.xml");
        if (f != null) {
            // load configuration
            LegacyConfigurationImporter importer = new LegacyConfigurationImporter();
            importer.setConfiguration(geoServer);
            importer.imprt(resourceLoader.getBaseDirectory());
        } else {
            LOGGER.warning("No configuration file found.");
        }
    }
}

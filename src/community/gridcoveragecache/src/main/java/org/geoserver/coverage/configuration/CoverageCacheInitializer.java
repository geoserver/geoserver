/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage.configuration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.gwc.GWC;
import org.geotools.util.logging.Logging;

public class CoverageCacheInitializer {

    private static final Logger LOGGER = Logging.getLogger(CoverageCacheInitializer.class);


    public CoverageCacheInitializer(GWC mediator, Catalog gsCatalog, CoverageCacheConfigPersister persister) throws IOException {
        // Add a new Listener to the Catalog
        gsCatalog.addListener(new CoverageListener(mediator, gsCatalog));
        
        // Handle the Configuration
        final File configFile = persister.findConfigFile();
        if (configFile == null) {
            if(LOGGER.isLoggable(Level.FINE)){
                LOGGER.fine("CoverageCache specific configuration not found, creating with old defaults");
            }
            CoverageCacheConfig oldDefaults = CoverageCacheConfig.getOldDefaults();
            persister.save(oldDefaults);
        }

        final CoverageCacheConfig config = persister.getConfiguration();
        checkNotNull(config);
    }
}

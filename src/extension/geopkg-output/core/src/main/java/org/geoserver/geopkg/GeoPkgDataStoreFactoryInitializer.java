/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import java.util.logging.Logger;
import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.util.logging.Logging;

/**
 * Initializes an GeoPkg data store factory setting its location to the geoserver data directory.
 *
 * <p>Also logs a warning when neither the {@code read_only} nor the {@code immutable} parameter is configured, since
 * GeoPackage-backed layers served read-only benefit significantly from enabling one of these options to avoid SQLite
 * file-lock contention under concurrent load (see GEOS-12094).
 *
 * @author Justin Deoliveira, Boundless
 */
public class GeoPkgDataStoreFactoryInitializer extends DataStoreFactoryInitializer<GeoPkgDataStoreFactory> {

    static final Logger LOGGER = Logging.getLogger(GeoPkgDataStoreFactoryInitializer.class);

    GeoServerResourceLoader resourceLoader;

    public GeoPkgDataStoreFactoryInitializer() {
        super(GeoPkgDataStoreFactory.class);
    }

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void initialize(GeoPkgDataStoreFactory factory) {
        factory.setBaseDirectory(resourceLoader.getBaseDirectory());
        warnIfConcurrentAccessParamsMissing(factory);
    }

    /**
     * Logs a warning that GeoPackage stores used for read-only serving should enable the {@code read_only} or
     * {@code immutable} parameter to prevent SQLite file-lock contention under concurrent WMS/WFS load (GEOS-12094).
     * Both parameters default to {@code false}; this advisory is emitted once at startup so administrators are aware of
     * the option.
     */
    void warnIfConcurrentAccessParamsMissing(GeoPkgDataStoreFactory factory) {
        LOGGER.warning("GeoPackage datastore initialized with default settings. GeoPackage layers served "
                + "read-only under concurrent WMS/WFS load may experience SQLite file-lock "
                + "contention and thread hangs (GEOS-12094). If your GeoPackage files are "
                + "not modified while GeoServer is running, consider setting the 'immutable' "
                + "parameter to true on each GeoPackage store in the GeoServer Data Store "
                + "configuration UI. Alternatively, use 'read_only' to allow shared-read "
                + "access without exclusive locks.");
    }
}

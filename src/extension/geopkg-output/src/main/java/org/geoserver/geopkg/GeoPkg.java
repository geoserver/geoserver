/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.geotools.geopkg.GeoPackage;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.sqlite.SQLiteConfig;

/**
 * GeoPackage Utility class.
 *
 * @author Justin Deoliveira, Boundless
 */
public class GeoPkg {

    /** package file extension */
    public static final String EXTENSION = "gpkg";

    /** format mime type */
    public static final String LEGACY_MIME_TYPE = "application/x-gpkg";

    public static final String MIME_TYPE = "application/geopackage+sqlite3";
    public static final Collection<String> MIME_TYPES =
            Lists.newArrayList(MIME_TYPE, LEGACY_MIME_TYPE);

    /** names/aliases for the format */
    public static final Collection<String> NAMES =
            Lists.newArrayList("geopackage", "geopkg", "gpkg");

    /**
     * Initialize a GeoPackage connection with top speed for single user writing
     *
     * @param file The GeoPackage location
     */
    public static GeoPackage getGeoPackage(File file) throws IOException {
        SQLiteConfig config = new SQLiteConfig();
        config.setSharedCache(true);
        config.setJournalMode(SQLiteConfig.JournalMode.OFF);
        config.setPragma(SQLiteConfig.Pragma.SYNCHRONOUS, "OFF");
        config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
        Map<String, Object> params = new HashMap<>();
        params.put(JDBCDataStoreFactory.BATCH_INSERT_SIZE.key, 10000);
        return new GeoPackage(file, config, params);
    }
}

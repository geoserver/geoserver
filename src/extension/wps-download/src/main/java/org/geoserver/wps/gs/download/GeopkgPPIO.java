/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geotools.geopkg.Entry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.sqlite.SQLiteConfig;

/**
 * Base class for GeoPackage based PPIOs, subclasses provide specialized behavior to store rasters
 * and vectors
 */
abstract class GeopkgPPIO extends ComplexPPIO {
    /** Registered GeoPackage MIME type */
    public static final String MIME_TYPE = "application/geopackage+sqlite3";

    public GeopkgPPIO(Class<?> externalType, Class<?> internalType) {
        super(externalType, internalType, MIME_TYPE);
    }

    @Override
    public PPIODirection getDirection() {
        // no reading for the moment.
        // may make sense to implement if there is only one layer in the pacakge
        return PPIODirection.ENCODING;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException("GeoPackage reading not supported yet");
    }

    protected GeoPackage getGeoPackage(File file) throws IOException {
        SQLiteConfig config = new SQLiteConfig();
        config.setSharedCache(true);
        config.setJournalMode(SQLiteConfig.JournalMode.OFF);
        config.setPragma(SQLiteConfig.Pragma.SYNCHRONOUS, "OFF");
        config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
        Map<String, Object> params = new HashMap<>();
        params.put(JDBCDataStoreFactory.BATCH_INSERT_SIZE.key, 10000);
        return new GeoPackage(file, config, params);
    }

    protected String abstractOrDescription(ResourceInfo meta) {
        return meta.getAbstract() != null ? meta.getAbstract() : meta.getDescription();
    }

    protected void setupEntryMetadata(Entry e, Object ri) {
        if (ri instanceof ResourceInfo) {
            ResourceInfo meta = (ResourceInfo) ri;
            // initialize entry metadata
            e.setTableName(((ResourceInfo) ri).getName());
            e.setIdentifier(meta.getTitle());
            e.setDescription(abstractOrDescription(meta));
        }
    }

    @Override
    public String getFileExtension() {
        return "gpkg";
    }
}

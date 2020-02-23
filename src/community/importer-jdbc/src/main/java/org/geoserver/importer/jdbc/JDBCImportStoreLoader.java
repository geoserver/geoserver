/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.importer.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.data.DataStoreFinder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

public class JDBCImportStoreLoader implements DisposableBean {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logging.getLogger(JDBCImportStoreLoader.class);

    private static final String JDBCSTATUS_NAME = "jdbc-import-store.properties";
    private GeoServerDataDirectory dataDir;
    private JDBCDataStore store;

    public JDBCDataStore getStore() {
        return store;
    }

    /** Loads a new {@link JDBCDatastore} from the data directory, and */
    public JDBCImportStoreLoader(GeoServerDataDirectory dd) throws IOException {
        // see if we have the JDBCDatastore configuration ready, otherwise create one from the
        // classpath
        this.dataDir = dd;
        try {
            Properties params = getParameters();

            store = (JDBCDataStore) DataStoreFinder.getDataStore(params);
        } catch (IOException e) {
            LOGGER.info(
                    "can't find or create JDBC import store configuration file: "
                            + JDBCSTATUS_NAME);
            LOGGER.log(Level.FINE, "no config file?", e);
        }
    }

    public Properties getParameters() throws IOException {
        Resource resource = dataDir.get(JDBCSTATUS_NAME);
        if (resource.getType() == Type.UNDEFINED) {
            // setup a H2
            Properties fixture = new Properties();
            fixture.put("user", "geotools");
            fixture.put("password", "geotools");
            fixture.put(
                    "database", dataDir.getRoot().dir().getAbsolutePath() + "/importer/h2-store");
            fixture.put("dbtype", "h2");
            try (OutputStream os = resource.out()) {
                fixture.store(os, "Defaulting to local H2 database");
            }
        }
        Properties params = new Properties();
        try (InputStream is = resource.in()) {
            params.load(is);
        }
        return params;
    }

    @Override
    public void destroy() throws Exception {
        store.dispose();
    }
}

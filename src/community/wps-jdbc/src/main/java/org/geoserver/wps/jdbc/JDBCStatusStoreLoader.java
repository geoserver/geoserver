/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

public class JDBCStatusStoreLoader implements DisposableBean {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logging.getLogger(JDBCStatusStoreLoader.class);

    private static final String JDBCSTATUS_NAME = "jdbcstatusstore.props";
    private GeoServerDataDirectory dataDir;
    private DataStore store;

    public DataStore getStore() {
        return store;
    }

    /** Loads a new {@link JDBCDatastore} from the data directory, and */
    public JDBCStatusStoreLoader(GeoServerDataDirectory dd) throws IOException {
        // see if we have the JDBCDatastore configuration ready, otherwise create one from the
        // classpath
        dataDir = dd;
        try {
            Properties params = getParameters();

            store = DataStoreFinder.getDataStore(params);

        } catch (IOException e) {
            LOGGER.info("can't find or create JDBC Status store configuration file");
            LOGGER.log(Level.FINE, "no config file?", e);
        }
    }

    public Properties getParameters() throws IOException {

        Resource resource = dataDir.get(JDBCSTATUS_NAME);
        if (resource.getType() == Type.UNDEFINED) {
            try (OutputStream os = resource.out();
                    InputStream is =
                            JDBCStatusStoreLoader.class.getResourceAsStream(JDBCSTATUS_NAME)) {
                IOUtils.copy(is, os);
            }
        }
        Properties params = new Properties();
        try (InputStream is = resource.in()) {

            params.load(is);
        }
        return params;
    }

    public void saveParameters(Properties props) throws IOException {
        Resource resource = dataDir.get(JDBCSTATUS_NAME);
        if (resource.getType() != Type.UNDEFINED) {
            Resource backup = dataDir.get(JDBCSTATUS_NAME + ".bak");
            try (OutputStream os = backup.out();
                    InputStream is = resource.in()) {
                IOUtils.copy(is, os);
                is.close();
                os.close();
            }
        }

        OutputStream os = resource.out();
        props.store(os, "saved by GeoServer @" + new Date());
        os.close();
    }

    @Override
    public void destroy() throws Exception {
        store.dispose();
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static org.geogig.geoserver.config.LogStore.PROP_DRIVER_CLASS;
import static org.geogig.geoserver.config.LogStore.PROP_ENABLED;
import static org.geogig.geoserver.config.LogStore.PROP_MAX_CONNECTIONS;
import static org.geogig.geoserver.config.LogStore.PROP_PASSWORD;
import static org.geogig.geoserver.config.LogStore.PROP_RUN_SCRIPT;
import static org.geogig.geoserver.config.LogStore.PROP_SCRIPT;
import static org.geogig.geoserver.config.LogStore.PROP_URL;
import static org.geogig.geoserver.config.LogStore.PROP_USER;

import java.io.File;
import java.util.Properties;

public class HSQLLogStoreTest extends AbstractLogStoreTest {

    @Override
    protected void populateConfigProperties(Properties props) {

        final String driverClassName = "org.hsqldb.jdbcDriver";
        final File dbFile = new File(tmpDir.getRoot(), "logstore.hsql");
        final String jdbcUrl = "jdbc:hsqldb:file:" + dbFile.getAbsolutePath();

        props.setProperty(PROP_ENABLED, "true");
        props.setProperty(PROP_DRIVER_CLASS, driverClassName);
        props.setProperty(PROP_URL, jdbcUrl);
        props.setProperty(PROP_USER, "sa");
        props.setProperty(PROP_PASSWORD, "");
        props.setProperty(PROP_MAX_CONNECTIONS, "10");
        props.setProperty(PROP_SCRIPT, "hsqldb.sql");
        props.setProperty(PROP_RUN_SCRIPT, "true");

        // runScript(driverClassName, jdbcUrl, getClass().getResource("hsqldb.sql"), "sa", null);
    }
}

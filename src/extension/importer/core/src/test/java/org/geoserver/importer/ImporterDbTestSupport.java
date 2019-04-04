/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import org.geoserver.data.test.LiveDbmsData;
import org.geoserver.data.test.SystemTestData;

public abstract class ImporterDbTestSupport extends ImporterTestSupport {

    @Override
    public SystemTestData createTestData() throws Exception {
        return new DbmsTestData(getDataDirectory().root(), getFixtureId(), null);
    }

    protected void doSetUpInternal() throws Exception {}

    protected abstract String getFixtureId();

    protected Connection getConnection() throws Exception {
        return ((DbmsTestData) getTestData()).getConnection();
    }

    protected Map getConnectionParams() throws IOException {
        return ((DbmsTestData) getTestData()).getConnectionParams();
    }

    protected void run(String sql, Statement st) throws SQLException {
        st.execute(sql);
    }

    protected void runSafe(String sql, Statement st) {
        try {
            run(sql, st);
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
        }
    }

    class DbmsTestData extends LiveDbmsData {

        public DbmsTestData(File dataDirSourceDirectory, String fixtureId, File sqlScript)
                throws IOException {
            super(dataDirSourceDirectory, fixtureId, sqlScript);
            getFilteredPaths().clear();
        }

        public File getFixture() {
            return fixture;
        }

        public Connection getConnection() throws Exception {
            Map p = getConnectionParams();
            Class.forName((String) p.get("driver"));

            String url = (String) p.get("url");
            String user = (String) p.get("username");
            String passwd = (String) p.get("password");

            return DriverManager.getConnection(url, user, passwd);
        }

        public Map getConnectionParams() throws IOException {
            Properties props = new Properties();
            FileInputStream fin = new FileInputStream(getFixture());
            try {
                props.load(fin);
            } finally {
                fin.close();
            }

            return new HashMap(props);
        }
    }
}

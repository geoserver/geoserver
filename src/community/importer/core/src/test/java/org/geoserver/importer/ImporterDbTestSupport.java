/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.geoserver.data.test.LiveDbmsData;
import org.geoserver.data.test.MockData;
import org.geotools.data.DataStore;

public abstract class ImporterDbTestSupport extends ImporterTestSupport {

    @Override
    public DbTestData buildTestData() throws Exception {
        return new DbTestData();
    }

    @Override
    protected final void setUpInternal() throws Exception {
        if (getTestData().isTestDataAvailable()) {
            super.setUpInternal();
            doSetUpInternal();
        }
    }

    protected void doSetUpInternal() throws Exception {
    }

    protected abstract String getFixtureId();

    protected Connection getConnection() throws Exception  {
        return ((DbTestData)getTestData()).getConnection();
    }

    protected Map getConnectionParams() throws IOException {
        return ((DbTestData)getTestData()).getConnectionParams();
    }

    protected void run(String sql, Statement st) throws SQLException {
        st.execute(sql);
    }

    protected void runSafe(String sql, Statement st) {
        try {
            run(sql, st);
        }
        catch(SQLException e) {
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
    }
    
    class DbTestData extends MockData {

        DbmsTestData dbTestData;
        
        public DbTestData() throws IOException {
            dbTestData = new DbmsTestData(getDataDirectoryRoot(), getFixtureId(), null);
        } 
    
        @Override
        public void setUp() throws IOException {
            try {
                dbTestData.setUp();
            } catch (Exception e) {
                throw new IOException(e);
            }
            super.setUp();
        }

        @Override
        public boolean isTestDataAvailable() {
            if (dbTestData.isTestDataAvailable()) {
                //actually try a connection
                try {
                    getConnection();
                    return true;
                }
                catch(Exception e) {
                    LOGGER.log(Level.SEVERE, "Could not obtain connection", e);
                }
            }
            return false;
        }

        public Connection getConnection() throws Exception {
            Map p = getConnectionParams();
            Class.forName((String)p.get("driver"));

            String url = (String) p.get("url");
            String user = (String) p.get("username");
            String passwd = (String) p.get("password");

            return DriverManager.getConnection(url, user, passwd);
        }

        public Map getConnectionParams() throws IOException {
            Properties props = new Properties();
            FileInputStream fin = new FileInputStream(dbTestData.getFixture());
            try {
                props.load(fin);
            }
            finally {
                fin.close();
            }

            return new HashMap(props);
        }
    }
}

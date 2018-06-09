/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.jdbc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.data.test.LiveDbmsData;
import org.geoserver.security.AbstractSecurityServiceTest;
import org.geoserver.security.impl.Util;
import org.geoserver.util.IOUtils;

public class LiveDbmsDataSecurity extends LiveDbmsData {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.jdbc");
    protected Boolean available = null;

    public LiveDbmsDataSecurity(File dataDirSourceDirectory, String fixtureId, File sqlScript)
            throws IOException {
        super(dataDirSourceDirectory, fixtureId, sqlScript);
    }

    public LiveDbmsDataSecurity(String fixtureId) throws Exception {
        this(AbstractSecurityServiceTest.unpackTestDataDir(), fixtureId, null);
    }

    @Override
    public void setUp() throws Exception {
        data = IOUtils.createRandomDirectory("./target", "live", "data");
        IOUtils.deepCopy(source, data);
    }

    /* (non-Javadoc)
     * @see org.geoserver.data.test.LiveDbmsData#isTestDataAvailable()
     *
     * Checks if a connection is possible
     */
    @Override
    public boolean isTestDataAvailable() {

        if (available != null) return available;

        available = super.isTestDataAvailable();

        if (!available) return available; // false

        Properties props = null;
        try {
            props = Util.loadUniversal(new FileInputStream(fixture));
        } catch (IOException e1) {
            // should not happen
            throw new RuntimeException(e1);
        }
        String msgPrefix = "Disabling test based on fixture " + fixtureId + " since ";

        String driverClassName = props.getProperty("driver");
        if (driverClassName == null) {
            LOGGER.warning(
                    msgPrefix + "property \"driver\" not found in " + fixture.getAbsolutePath());
            available = false;
            return available;
        }

        String url = props.getProperty("url");
        if (url == null) {
            LOGGER.warning(
                    msgPrefix + "property \"url\" not found in " + fixture.getAbsolutePath());
            available = false;
            return available;
        }

        String user = props.getProperty("user");
        if (user == null) user = props.getProperty("username"); // to be sure
        String password = props.getProperty("password");

        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            LOGGER.warning(msgPrefix + " driver class not found: " + driverClassName);
            available = false;
            return available;
        }

        Connection con = null;
        try {
            if (user == null) con = DriverManager.getConnection(url);
            else con = DriverManager.getConnection(url, user, password);
            con.close();
        } catch (SQLException ex) {
            LOGGER.warning(msgPrefix + " an sql error:\n " + ex.getMessage());
            available = false;
            return available;
        }
        available = true;
        return available;
    }

    public File getFixture() {
        return fixture;
    }
}

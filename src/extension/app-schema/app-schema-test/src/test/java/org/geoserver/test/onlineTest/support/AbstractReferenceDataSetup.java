/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCTestSetup;
import org.geotools.referencing.CRS;

/**
 * Base class that initialise and provides the methods for online test to take place. Other tests that intends to run
 * their unit test online should extend from this class and implement the abstract methods.
 *
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public abstract class AbstractReferenceDataSetup extends JDBCTestSetup {

    /** System property set to totally disable any online tests */
    public static final String ONLINE_TEST_PROFILE = "onlineTestProfile";

    /** Geopkg file path to setup correctly */
    protected String geopkgDir;

    protected Logger LOGGER = Logger.getLogger(AbstractReferenceDataSetup.class);

    /**
     * A static map which tracks which fixture files can not be found. This prevents continually looking up the file and
     * reporting it not found to the user.
     */
    protected static Map<String, Boolean> found = new HashMap<>();

    // The type of database to use.
    @Override
    public abstract JDBCDataStoreFactory createDataStoreFactory();

    // Setup the data.
    @Override
    public abstract void setUp() throws Exception;

    @Override
    protected abstract Properties createExampleFixture();

    @Override
    public void setUpData() throws Exception {
        super.setUpData();
    }

    @Override
    public void initializeDatabase() throws Exception {
        super.initializeDatabase();
    }

    // retrieve the id of the database.
    public abstract String getDatabaseID();

    protected Map<String, Boolean> getOnlineMap() {
        return found;
    }

    /** Load fixture configuration. Create example if absent. */
    protected void configureFixture() {
        if (fixture == null) {
            String fixtureId = getDatabaseID();
            if (fixtureId == null) {
                return; // not available (turn test off)
            }
            try {
                // load the fixture
                File base = GSFixtureUtilitiesDelegate.getFixtureDirectory();
                // look for a "profile", these can be used to group related fixtures
                String profile = System.getProperty(ONLINE_TEST_PROFILE);
                if (profile != null && !"".equals(profile)) {
                    base = new File(base, profile);
                }
                File fixtureFile = GSFixtureUtilitiesDelegate.getFixtureFile(base, fixtureId);
                // Sets the fixtureFile to be used for interpolation in the mapping file

                Boolean exists = found.get(fixtureFile.getCanonicalPath());
                if (exists == null || exists.booleanValue()) {
                    if (fixtureFile.exists()) {
                        fixture = GSFixtureUtilitiesDelegate.loadProperties(fixtureFile);
                        // update jdbc connection url. take geopkg file from resources and use that
                        // geopkg in the tests
                        if (fixtureId.equals("geopkg")) {
                            fixture.setProperty(
                                    "url", fixture.getProperty("url").substring(0, 11) + geopkgDir.substring(4));
                        }
                        found.put(fixtureFile.getCanonicalPath(), true);
                        System.setProperty("app-schema.properties", fixtureFile.getPath());
                    } else {
                        // no fixture file, if no profile was specified write out a template
                        // fixture using the offline fixture properties
                        if (profile == null) {
                            Properties exampleFixture = createExampleFixture();
                            if (exampleFixture != null) {
                                File exFixtureFile = new File(fixtureFile.getAbsolutePath() + ".example");
                                if (!exFixtureFile.exists()) {
                                    createExampleFixture(exFixtureFile, exampleFixture);
                                }
                            }
                        }
                        found.put(fixtureFile.getCanonicalPath(), false);
                    }
                }

                if (fixture == null && exists == null) {
                    // only report if exists == null since it means that this is
                    // the first time trying to load the fixture
                    GSFixtureUtilitiesDelegate.printSkipNotice(fixtureId, fixtureFile);
                }
            } catch (Exception e) {
                LOGGER.log(Level.ERROR, "", e);
            }
        }
    }

    /** Creates Example Fixture */
    protected void createExampleFixture(File exFixtureFile, Properties exampleFixture) {
        try {
            exFixtureFile.getParentFile().mkdirs();
            exFixtureFile.createNewFile();

            try (FileOutputStream fout = new FileOutputStream(exFixtureFile)) {
                exampleFixture.store(
                        fout,
                        "This is an example fixture. Update the "
                                + "values and remove the .example suffix to enable the test");
                fout.flush();
            }
            LOGGER.info("Wrote example fixture file to " + exFixtureFile);
        } catch (IOException ioe) {
            LOGGER.log(Level.WARN, "Unable to write out example fixture " + exFixtureFile, ioe);
        }
    }

    public void run(String input, boolean replaceNewLine) throws Exception {
        if (replaceNewLine) {
            run(input);
        } else {
            super.run(input);
        }
    }

    @Override
    public void run(String input) throws Exception {
        super.run(input.replaceAll(DatabaseUtil.NEWLINE, " "));
    }

    /**
     * Get srid from geometry type.
     *
     * @param type geometry type
     * @return srid
     */
    protected int getSrid(GeometryType type) {
        int srid = -1;
        CoordinateReferenceSystem crs = type.getCoordinateReferenceSystem();
        if (crs != null) {
            try {
                srid = CRS.lookupEpsgCode(crs, true);
            } catch (FactoryException e) {
                // will use -1 by default
                LOGGER.warn("Exception occurred when looking up srid! CRS will be ignored.");
            }
        }
        return srid;
    }

    protected Properties getFixture() {
        return fixture;
    }
}

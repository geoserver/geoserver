/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import junit.framework.TestResult;
import org.geoserver.test.AbstractAppSchemaWfsTestSupport;
import org.geoserver.test.NamespaceTestData;

/**
 * Base class that provides the Wfs test support framework and perform checks on the fixture and the
 * availabilities of the fixture required
 * 
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 * 
 */
public abstract class AbstractDataReferenceWfsTest extends AbstractAppSchemaWfsTestSupport {
    protected AbstractReferenceDataSetup setup = null;

    protected Properties fixture = null;

    protected boolean available;

    public AbstractDataReferenceWfsTest() throws Exception {
        setup = this.getReferenceDataSetup();
        available = this.checkAvailable();
        if (available)
            initialiseTest();
    }

    /**
     * A static map which tracks which fixtures are offline. This prevents continually trying to run
     * a test when an external resource is offline.
     */
    protected static Map<String, Boolean> online = new HashMap<String, Boolean>();

    /**
     * System property set to totally disable any online tests
     */
    public static final String ONLINE_TEST_PROFILE = "onlineTestProfile";

    /**
     * The key in the test fixture property file used to set the behaviour of the online test if
     * {@link #connect()} fails.
     */
    public static final String SKIP_ON_FAILURE_KEY = "skip.on.failure";

    /**
     * The default value used for {@link #SKIP_ON_FAILURE_KEY} if it is not present.
     */
    public static final String SKIP_ON_FAILURE_DEFAULT = "true";

    protected boolean skipOnFailure = true;

    /**
     * A static map which tracks which fixture files can not be found. This prevents continually
     * looking up the file and reporting it not found to the user.
     */
    protected static Map<String, Boolean> found = new HashMap<String, Boolean>();

    @Override
    protected void oneTimeSetUp() throws Exception {
        if (available) {
            setup.setUp();
        }
        super.oneTimeSetUp();
    }

    public abstract AbstractReferenceDataSetup getReferenceDataSetup() throws Exception;

    public void connect() throws Exception {

        setup.initializeDatabase();
        setup.setUpData();
    }

    @Override
    public void run(TestResult result) {
        if (available) {
            super.run(result);
        }
    }

    /**
     * Loads the test fixture for the test case.
     * <p>
     * The fixture id is obtained via {@link #getFixtureId()}.
     * </p>
     */

    protected final void initialiseTest() throws Exception {
        skipOnFailure = Boolean.parseBoolean(fixture.getProperty(SKIP_ON_FAILURE_KEY,
                SKIP_ON_FAILURE_DEFAULT));
        // call the setUp template method
        try {
            connect();
        } catch (Exception e) {
            if (skipOnFailure) {
                // disable the test
                fixture = null;
                // leave some trace of the swallowed exception
                e.printStackTrace();
            } else {
                // do not swallow the exception
                throw e;
            }
        }
    }

    /**
     * Check whether the fixture is available. This method also loads the configuration if present,
     * and tests the connection using {@link #isOnline()}.
     * 
     * @return true if fixture is available for use
     * @throws FileNotFoundException
     */
    protected boolean checkAvailable() throws Exception {

        configureFixture();
        if (fixture == null) {
            return false;
        } else {
            String fixtureId = getFixtureId();
            setup.setFixture(fixture);
            // do an online/offline check
            Boolean available = (Boolean) online.get(fixtureId);
            if (available == null || available.booleanValue()) {
                // test the connection
                try {
                    available = isOnline();
                } catch (Throwable t) {
                    System.out.println("Skipping " + fixtureId
                            + " tests, resources not available: " + t.getMessage());
                    t.printStackTrace();
                    available = Boolean.FALSE;
                }
                online.put(fixtureId, available);
            }
            return available;
        }
    }

    private Boolean isOnline() {
        try {
            DataSource dataSource = setup.getDataSource();
            Connection cx = dataSource.getConnection();
            cx.close();
            return true;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    protected String getFixtureId() {
        return setup.getDatabaseID();
    }

    /**
     * Load fixture configuration. Create example if absent.
     */
    private void configureFixture() {
        if (fixture == null) {
            String fixtureId = getFixtureId();
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
                        found.put(fixtureFile.getCanonicalPath(), true);
                        System.setProperty("app-schema.properties", fixtureFile.getPath());
                    } else {
                        // no fixture file, if no profile was specified write out a template
                        // fixture using the offline fixture properties
                        if (profile == null) {
                            Properties exampleFixture = createExampleFixture();
                            if (exampleFixture != null) {
                                File exFixtureFile = new File(fixtureFile.getAbsolutePath()
                                        + ".example");
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
                e.printStackTrace();
            }
        }
    }

    protected Properties createExampleFixture() {
        return this.setup.createExampleFixture();
    }

    /**
     * Creates Example Fixture
     * 
     * @param exFixtureFile
     * @param exampleFixture
     */
    protected void createExampleFixture(File exFixtureFile, Properties exampleFixture) {
        try {
            exFixtureFile.getParentFile().mkdirs();
            exFixtureFile.createNewFile();

            FileOutputStream fout = new FileOutputStream(exFixtureFile);

            exampleFixture.store(fout, "This is an example fixture. Update the "
                    + "values and remove the .example suffix to enable the test");
            fout.flush();
            fout.close();
            System.out.println("Wrote example fixture file to " + exFixtureFile);
        } catch (IOException ioe) {
            System.out.println("Unable to write out example fixture " + exFixtureFile);
            ioe.printStackTrace();
        }
    }

    @Override
    protected abstract NamespaceTestData buildTestData();

}

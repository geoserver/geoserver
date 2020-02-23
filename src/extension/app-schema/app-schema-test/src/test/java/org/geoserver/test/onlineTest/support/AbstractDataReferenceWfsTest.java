/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.support;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.junit.Assume;

/**
 * Base class that provides the Wfs test support framework and perform checks on the fixture and the
 * availabilities of the fixture required
 *
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public abstract class AbstractDataReferenceWfsTest extends AbstractAppSchemaTestSupport {
    protected AbstractReferenceDataSetup setup = null;

    protected Properties fixture = null;

    protected boolean available;

    public AbstractDataReferenceWfsTest() throws Exception {
        setup = this.getReferenceDataSetup();
        available = this.checkAvailable();
        Assume.assumeTrue(available);
        if (available) initialiseTest();
    }

    /**
     * The key in the test fixture property file used to set the behaviour of the online test if
     * {@link #connect()} fails.
     */
    public static final String SKIP_ON_FAILURE_KEY = "skip.on.failure";

    /** The default value used for {@link #SKIP_ON_FAILURE_KEY} if it is not present. */
    public static final String SKIP_ON_FAILURE_DEFAULT = "true";

    protected boolean skipOnFailure = true;

    /**
     * A static map which tracks which fixture files can not be found. This prevents continually
     * looking up the file and reporting it not found to the user.
     */
    protected static Map<String, Boolean> found = new HashMap<String, Boolean>();

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        setup.setUp();

        super.setUpTestData(testData);
    }

    public abstract AbstractReferenceDataSetup getReferenceDataSetup() throws Exception;

    public void connect() throws Exception {
        setup.initializeDatabase();
        setup.setUpData();
    }

    /**
     * Loads the test fixture for the test case.
     *
     * <p>The fixture id is obtained via {@link #getFixtureId()}.
     */
    protected final void initialiseTest() throws Exception {
        skipOnFailure =
                Boolean.parseBoolean(
                        fixture.getProperty(SKIP_ON_FAILURE_KEY, SKIP_ON_FAILURE_DEFAULT));
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
     */
    protected boolean checkAvailable() throws Exception {

        setup.configureFixture();
        fixture = setup.getFixture();
        if (fixture == null) {
            return false;
        } else {
            String fixtureId = getFixtureId();
            setup.setFixture(fixture);
            // do an online/offline check
            Map<String, Boolean> online = setup.getOnlineMap();
            Boolean available = (Boolean) online.get(fixtureId);
            if (available == null || available.booleanValue()) {
                // test the connection
                try {
                    available = isOnline();
                } catch (Throwable t) {
                    System.out.println(
                            "Skipping "
                                    + fixtureId
                                    + " tests, resources not available: "
                                    + t.getMessage());
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
}

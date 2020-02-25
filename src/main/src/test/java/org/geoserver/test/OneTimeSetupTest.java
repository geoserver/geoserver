/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import junit.extensions.TestSetup;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Abstract class for tests that do need to run a one time setup/teardown phase.
 *
 * <p>Candidate tests are the ones that setup an expensive test fixture that happens to be reusable
 * in subsequent tests (that is, its state is not modified running the tests).
 *
 * <p>Such tests can extend this class, implement the expensive setup/teardown phases in {@link
 * #oneTimeSetUp()} and {@link #oneTimeTearDown()}, and eventual per method setup/teardown in {@link
 * #setUpInternal()} and {@link #tearDownInternal()}.
 *
 * <p>In order to activate the one time setup the generic MyOneTimeSetupTest class will also have to
 * add the following static method:
 *
 * <pre>
 * public static Test suite() {
 *     return new OneTimeTestSetup(new MyOneTimeSetupTest());
 * }
 * </pre>
 *
 * @author Andrea Aime - TOPP
 * @author Gabriel Roldan - TOPP
 */
public abstract class OneTimeSetupTest extends TestCase {
    private static boolean oneTimeSetupDone;
    private static boolean forceOneTimeTearDown;
    private boolean inSetup;
    private boolean inTearDown;

    /**
     * Test suite wrapper
     *
     * @author Andrea Aime - TOPP
     * @author Gabriel Roldan - TOPP
     */
    protected static class OneTimeTestSetup extends TestSetup {

        private OneTimeSetupTest test;

        public OneTimeTestSetup(OneTimeSetupTest test) {
            super(new TestSuite(test.getClass()));
            this.test = test;
        }

        @Override
        protected void setUp() throws Exception {
            super.setUp();
            oneTimeSetupDone = true;
            forceOneTimeTearDown = false;
            test.oneTimeSetUp();
        }

        @Override
        protected void tearDown() throws Exception {
            super.tearDown();
            oneTimeSetupDone = false;
            forceOneTimeTearDown = false;
            test.oneTimeTearDown();
        }
    }

    /**
     * This provides the one time setup for the expensive fixture. The fields making up the fixture
     * will have to be static ones, to avoid loosing their values as JUnit goes through the test
     * methods (for each one a new instance of the class will be created)
     */
    protected abstract void oneTimeSetUp() throws Exception;

    /** This provides the one time teardown for the expensive fixture. */
    protected abstract void oneTimeTearDown() throws Exception;

    /**
     * Provides the proper behavior so that the one time setup is run once matter how the test is
     * started. If you need to implement a per test method setup, override {@link #setUpInternal()}
     */
    @Override
    protected final void setUp() throws Exception {
        if (inSetup)
            throw new RuntimeException(
                    "setUpInternal seems to call back to super.setUp(). "
                            + "It should call super.setUpInternal instead");
        try {
            inSetup = true;

            if (!oneTimeSetupDone) {
                oneTimeSetUp();
                oneTimeSetupDone = true;
                forceOneTimeTearDown = true;
            }

            setUpInternal();
        } finally {
            inSetup = false;
        }
    }

    /**
     * Provides the proper behavior so that the one time tear down is once no matter how the test is
     * started. If you need to implement a per test method setup, overide {@link
     * #tearDownInternal()}
     */
    @Override
    protected final void tearDown() throws Exception {
        if (inTearDown)
            throw new RuntimeException(
                    "tearDownInternal seems to call back to super.tearDown(). "
                            + "It should call super.tearDownInternal instead");

        try {
            inTearDown = true;
            tearDownInternal();
            if (forceOneTimeTearDown) {
                oneTimeSetupDone = false;
                forceOneTimeTearDown = false;
                oneTimeTearDown();
            }
        } finally {
            inTearDown = false;
        }
    }

    /** Per method setup (fixture can be stored in non static fields) */
    protected void setUpInternal() throws Exception {}

    /** Per method tear down */
    protected void tearDownInternal() throws Exception {}
}

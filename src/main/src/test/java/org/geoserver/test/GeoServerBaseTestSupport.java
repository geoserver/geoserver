/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geotools.feature.NameImpl;
import org.geotools.util.Version;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.opengis.feature.type.Name;

/**
 * Base test support class for GeoServer test cases.
 *
 * <h2>Test Setup Lifecycle</h2>
 *
 * <p>This class provides a number of hooks for subclasses that are called throughout the life cycle
 * of the test. These include:
 *
 * <ul>
 *   <li>{@link #createTestData()} - The first subclass hook called to created the {@link TestData}
 *       for the test
 *   <li>{@link #setUp(TestData)} - Called after the test data setup has been completed and provides
 *       subclass with a chance to any setup it requires
 *   <li>{@link #tearDown(TestData)} - Called after the test has run and before the test setup tear
 *       down.
 * </ul>
 *
 * <p>Additionally a test class may use the standard JUnit annotations such as {@link Before},
 * {@link BeforeClass}, {@link After}, {@link AfterClass} to define additional life cycle setup and
 * tear down methods. Generally these methods will execute after methods of the super class with the
 * same annotation.
 *
 * <h2>Test Setup Frequency</h2>
 *
 * <p>The {@link TestSetup} annotation is used to control the frequency at which the test setup will
 * occur over the life of the test class. It controls whether the test setup is run repeatedly for
 * each test method or once for the all the test methods of the class.
 *
 * <p>The annotation is defined at the class level. For example, to define a single (one-time)
 * setup: <code>
 * <pre>
 * {@literal @}TestSetup(run=TestSetupFrequency.ONCE)
 * public class MyTest extends GeoServerBaseTestSupport {
 * }
 * </pre>
 * </code>
 *
 * @author Justin Deoliveira, OpenGeo
 * @param <T>
 */
@SuppressWarnings({
    "PMD.JUnit4TestShouldUseBeforeAnnotation",
    "PMD.JUnit4TestShouldUseAfterAnnotation"
})
public abstract class GeoServerBaseTestSupport<T extends TestData> {

    /** Common logger for test cases */
    protected static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.test");

    /** test data */
    protected static TestData testData;

    /** test instance, used to give subclass hooks for one time setup/teardown */
    protected static GeoServerBaseTestSupport test;

    /** Controls the frequency of the test setup */
    protected static TestSetupFrequency testSetupFrequency = null;

    //  @Rule
    //  public TestWatcher watcher = new TestWatcher() {
    //      protected void finished(org.junit.runner.Description description) {
    //          System.out.println(description);
    //      };
    //  };

    @Rule
    public TestRule runSetup =
            (base, description) -> {
                if (description.getAnnotation(RunTestSetup.class) != null) {
                    try {
                        doTearDownClass();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return base;
            };

    /** Checks for existence of a system property named "quietTests". */
    public static boolean isQuietTests() {
        String quietTests = System.getProperty("quietTests");
        return quietTests != null && !"false".equalsIgnoreCase(quietTests);
    }

    @BeforeClass
    public static final void setUpLogging() throws Exception {
        if (isQuietTests()) {
            Logging.getLogger("org.geoserver").setLevel(Level.SEVERE);
            Logging.getLogger("org.vfny.geoserver").setLevel(Level.SEVERE);
            Logging.getLogger("org.geotools").setLevel(Level.SEVERE);
        }
    }

    @BeforeClass
    public static final void setUpReferencing() throws Exception {
        // do we need to reset the referencing subsystem and reorient it with lon/lat order?
        if (System.getProperty("org.geotools.referencing.forceXY") == null
                || !"http".equals(Hints.getSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING))) {
            System.setProperty("org.geotools.referencing.forceXY", "true");
            Hints.putSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING, "http");
        }
    }

    @Before
    @SuppressWarnings("unchecked")
    public final void doSetup() throws Exception {
        if (testData == null) {
            test = this;
            testData = createTestData();

            Logger imageLogger = Logger.getLogger("org.geotools.image");
            Level previousLevel = imageLogger.getLevel();
            try {
                imageLogger.setLevel(Level.WARNING);
                testData.setUp();
            } finally {
                imageLogger.setLevel(previousLevel);
            }

            setUp((T) testData);
        }
    }

    @SuppressWarnings("unchecked")
    protected T getTestData() {
        return (T) testData;
    }

    /**
     * Creates the {@link TestData} implementation for this test.
     *
     * <p>If the concrete {@link TestData} class provides any configurable options that control how
     * its setup will operate they should be set/unset in this method before turning the new
     * instance.
     */
    protected abstract T createTestData() throws Exception;

    /**
     * Subclass hook for set up before the test run.
     *
     * <p>This methods should be used for setup that occurs after the {@link TestData} instance has
     * been setup.
     */
    protected void setUp(T testData) throws Exception {}

    @After
    public final void doTearDown() throws Exception {
        if (testSetupFrequency == null) {
            testSetupFrequency = lookupTestSetupPolicy();
        }
        if (testSetupFrequency != TestSetupFrequency.ONCE) {
            doTearDownClass();
        }
    }

    protected TestSetupFrequency lookupTestSetupPolicy() {
        Class<?> clazz = getClass();
        while (clazz != null && !Object.class.equals(clazz)) {
            TestSetup testSetup = (TestSetup) clazz.getAnnotation(TestSetup.class);
            if (testSetup != null) {
                return testSetup.run();
            }
            clazz = clazz.getSuperclass();
        }
        return TestSetupFrequency.REPEAT;
    }

    @AfterClass
    @SuppressWarnings("unchecked")
    public static final void doTearDownClass() throws Exception {
        if (testData != null) {
            try {
                try {
                    test.tearDown(testData);
                } catch (Throwable t) {
                    LOGGER.log(Logging.FATAL, "Failure to tear down test support: " + t, t);
                    throw t;
                }
                // reset log4j2 to default, to drop any open files
                LogManager.shutdown();
                @SuppressWarnings({
                    "resource",
                    "PMD.CloseResource"
                }) // current context, no need to enforce AutoClosable
                LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
                loggerContext.reconfigure(new DefaultConfiguration());

                try {
                    testData.tearDown();
                } catch (Throwable t) {
                    LOGGER.log(
                            Logging.FATAL,
                            "Failure to remove contents of the temporary data directory: " + t,
                            t);
                    throw t;
                }
            } finally {
                // clean up the static variables anyways, otherwise a failure
                // to tear down will pollute the test and test data used by subsequent tests
                testData = null;
                test = null;
            }
        }
    }

    @AfterClass
    public static void clearTestSetupFrequency() {
        testSetupFrequency = null;
    }

    /**
     * Subclass hook for set up before the test run.
     *
     * <p>This methods should be used for setup that occurs after the {@link TestData} instance has
     * been setup.
     */
    protected void tearDown(T testData) throws Exception {}

    // common convenience methods
    /** Returns a qualified name into a string of the form "[<prefix>:]<localPart>". */
    protected String toString(QName qName) {
        if (qName.getPrefix() != null) {
            return qName.getPrefix() + ":" + qName.getLocalPart();
        } else {
            return qName.getLocalPart();
        }
    }

    /** Returns a qualified name into a GeoTools type name. */
    protected Name toName(QName qName) {
        return qName.getNamespaceURI() != null
                ? new NameImpl(qName.getNamespaceURI(), qName.getLocalPart())
                : new NameImpl(qName.getLocalPart());
    }

    protected static boolean equalsIgnoringNewlineStyle(String s1, String s2) {
        if (s1 == null) {
            return s2 == null;
        } else if (s2 == null) {
            return false;
        }

        return normalizeLineEnds(s1).equals(normalizeLineEnds(s2));
    }

    private static String normalizeLineEnds(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

    protected void assertEqualsIgnoreNewLineStyle(String expected, String actual) {
        if (!equalsIgnoringNewlineStyle(expected, actual)) {
            throw new ComparisonFailure("", expected, actual);
        }
    }

    /**
     * Returns the {@link Service} matching the given service id and version, null if not found, an
     * exception if multiple are found (should not happen)
     */
    protected Service getService(String id, Version version) {
        List<Service> services =
                GeoServerExtensions.extensions(Service.class).stream()
                        .filter(s -> id.equals(s.getId()) && version.equals(s.getVersion()))
                        .collect(Collectors.toList());
        if (services.isEmpty()) {
            return null;
        }
        if (services.size() > 1) {
            throw new RuntimeException(
                    "Found more than one service with the required id and version: " + services);
        }
        return services.get(0);
    }
}

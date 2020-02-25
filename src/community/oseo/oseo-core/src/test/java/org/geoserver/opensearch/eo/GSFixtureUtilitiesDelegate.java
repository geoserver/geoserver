/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.File;
import java.util.Properties;
import junit.framework.TestCase;
import org.geotools.test.FixtureUtilities;

/**
 * Static methods to support the implementation of tests that use fixture configuration files. See
 * {@link OnlineTestCase} and {@link OnlineTestSupport} for details. This slightly differ from
 * org.geotools.test.FixtureUtilities as it points to a different directory. This utilities delegate
 * most of its method call to FixtureUtilities except where directory location is concerned. Note:
 * Static method cannot be overridden hence this implementation.
 *
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public class GSFixtureUtilitiesDelegate {

    /** Load {@link Properties} from a {@link File}. */
    public static Properties loadProperties(File file) {
        return FixtureUtilities.loadProperties(file);
    }

    /**
     * Return the directory containing GeoServer test fixture configuration files. This is
     * ".geoserver" in the user home directory.
     */
    public static File getFixtureDirectory() {
        return new File(System.getProperty("user.home") + File.separator + ".geoserver");
    }

    /**
     * Return the file that should contain the fixture configuration properties. It is not
     * guaranteed to exist.
     *
     * <p>
     *
     * Dots "." in the fixture id represent a subdirectory path under the GeoTools configuration
     * file directory. For example, an id <code>a.b.foo</code> would be resolved to
     * <code>.geotools/a/b/foo.properties<code>.
     *
     *            the base fixture configuration file directory, typically ".geotools" in the user
     *            home directory.
     *            the fixture id
     */
    public static File getFixtureFile(File fixtureDirectory, String fixtureId) {
        return FixtureUtilities.getFixtureFile(fixtureDirectory, fixtureId);
    }

    /**
     * Print a notice that tests are being skipped, identifying the property file whose absence is
     * responsible.
     *
     * @param fixtureId the fixture id
     * @param fixtureFile the missing fixture configuration file
     */
    public static void printSkipNotice(String fixtureId, File fixtureFile) {
        FixtureUtilities.printSkipNotice(fixtureId, fixtureFile);
    }

    /**
     * Return Properties loaded from a fixture configuration file, or null if not found.
     *
     * <p>If a fixture configuration file is not found, a notice is printed to standard output
     * stating that tests for this fixture id are skipped.
     *
     * <p>This method allows tests that cannot extend {@link OnlineTestCase} or {@link
     * OnlineTestSupport} because they already extend another class (for example, a non-online test
     * framework) to access fixture configuration files in the same way that those classes do. Only
     * basic fixture configuration loading is supported. This method does not support the extra
     * services such as fixture caching and connection testing provided by {@link OnlineTestCase}
     * and {@link OnlineTestSupport}.
     *
     * <p>A JUnit 4 test fixture can readily be disabled in the absence of a fixture configuration
     * file by placing <code>Assume.assumeNotNull(FixtureUtilities.loadFixture(fixtureId))</code> or
     * similar in its <code>@BeforeClass</code> method. JUnit 3 tests must provide their own logic,
     * typically overriding {@link TestCase#run()} or {@link TestCase#runTest()}, or providing a
     * suite.
     *
     * @param fixtureId the fixture id, where dots "." are converted to subdirectories.
     * @return the fixture Properties or null
     * @see OnlineTestCase
     * @see OnlineTestSupport
     */
    public static Properties loadFixture(String fixtureId) {
        File fixtureFile = getFixtureFile(getFixtureDirectory(), fixtureId);
        if (fixtureFile.exists()) {
            return loadProperties(fixtureFile);
        } else {
            printSkipNotice(fixtureId, fixtureFile);
            return null;
        }
    }
}

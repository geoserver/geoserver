package org.geoserver.metadata;

import static org.geoserver.web.GeoServerApplication.GEOSERVER_CSRF_DISABLED;

import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractWicketMetadataTest extends AbstractMetadataTest {

    @Autowired protected GeoServerApplication app;

    protected WicketTester tester;

    @BeforeClass
    public static void initialize() {
        // disable browser detection, makes testing harder for nothing
        GeoServerApplication.DETECT_BROWSER = false;
        // prevent Wicket from bragging about us being in dev mode (and run
        // the tests as if we were in production all the time)
        System.setProperty("wicket.configuration", "deployment");
        // Disable CSRF protection for tests, since the test framework doesn't set the Referer
        System.setProperty(GEOSERVER_CSRF_DISABLED, "true");

        // make sure that we check the english i18n when needed
        Locale.setDefault(Locale.ENGLISH);
    }

    @Before
    public void start() {
        tester = new WicketTester(app, true);
    }

    @After
    public void stop() {
        tester.destroy();
    }

    /**
     * Prints the specified component/page containment hierarchy to the standard output
     *
     * <p>Each line in the dump looks like: <componentId>(class) 'value'
     *
     * @param c the component to be printed
     * @param dumpClass if enabled, the component classes are printed as well
     * @param dumpValue if enabled, the component values are printed as well
     */
    public void print(Component c, boolean dumpClass, boolean dumpValue) {
        if (isQuietTests()) {
            return;
        }

        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }

    /**
     * Prints the specified component/page containment hierarchy to the standard output
     *
     * <p>Each line in the dump looks like: <componentId>(class) 'value'
     *
     * @param c the component to be printed
     * @param dumpClass if enabled, the component classes are printed as well
     * @param dumpValue if enabled, the component values are printed as well
     */
    public void print(Component c, boolean dumpClass, boolean dumpValue, boolean dumpPath) {
        if (isQuietTests()) {
            return;
        }

        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }

    /** Checks for existence of a system property named "quietTests". */
    public static boolean isQuietTests() {
        String quietTests = System.getProperty("quietTests");
        return quietTests != null && !"false".equalsIgnoreCase(quietTests);
    }
}

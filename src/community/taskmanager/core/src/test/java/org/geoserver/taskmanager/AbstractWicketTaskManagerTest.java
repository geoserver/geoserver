package org.geoserver.taskmanager;

import java.util.Locale;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.GeoServerApplication;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractWicketTaskManagerTest extends AbstractTaskManagerTest {

    @Autowired protected GeoServerApplication app;

    protected WicketTester tester;

    @BeforeClass
    public static void initialize() {
        // disable browser detection, makes testing harder for nothing
        GeoServerApplication.DETECT_BROWSER = false;
        // prevent Wicket from bragging about us being in dev mode (and run
        // the tests as if we were in production all the time)
        System.setProperty("wicket.configuration", "deployment");

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

    /** Logs in as administrator. */
    public void login() {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }
}

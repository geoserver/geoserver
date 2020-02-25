/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.settings.ResourceSettings;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.test.GeoServerBaseTestSupport;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.junit.Before;

/**
 * Base class for testing GeoServer Wicket components in isolation (not full Spring context init, no
 * data dir). Useful for quick testing of components that do not interact with other GeoServer core
 * components.
 */
public class GeoServerWicketUnitTestSupport {
    protected WicketTester tester;

    @Before
    public void setUp() {
        tester = new WicketTester(new TestWebApplication(), true);
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
        if (GeoServerBaseTestSupport.isQuietTests()) {
            return;
        }

        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }

    /**
     * A {@link WebApplication} with just enough initialization to test components meant to run in
     * GeoServer, but not really depending on a application context
     */
    protected static class TestWebApplication extends WebApplication {
        @Override
        public Class<? extends Page> getHomePage() {
            return null;
        }

        @Override
        protected void init() {
            super.init();

            // this setup allows the GeoServer i18n to work in unit tests
            ResourceSettings resourceSettings = getResourceSettings();
            resourceSettings.setUseMinifiedResources(false);
            resourceSettings.setResourceStreamLocator(new GeoServerResourceStreamLocator());
            List<IStringResourceLoader> stringResourceLoaders =
                    resourceSettings.getStringResourceLoaders();
            stringResourceLoaders.add(0, new GeoServerStringResourceLoader());
            stringResourceLoaders.add(0, new ClassStringResourceLoader(GeoServerApplication.class));
        }
    }
}

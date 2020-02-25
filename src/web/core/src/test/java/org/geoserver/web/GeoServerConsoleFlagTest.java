/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertEquals;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * Test the JVM argument GEOSERVER_CONSOLE_DISABLED actually disables the GeoServer web console when
 * set
 */
public class GeoServerConsoleFlagTest extends GeoServerSystemTestSupport {

    private static final String CONSOLE_DISABLED_PUBLISHER = "filePublisher";
    private static final String CONSOLE_ENABLED_PUBLISHER = "wicket";

    private static final String WEB_MAPPING = "/web";
    private static final String WEB_WILDCARD_MAPPING = "/web/**";
    private static final String WEB_RESOURCES_WILDCARD_MAPPING = "/web/resources/**";

    private void setGeoserverConsoleDisabled(boolean disabled) {
        System.setProperty("GEOSERVER_CONSOLE_DISABLED", Boolean.toString(disabled));
    }

    private SimpleUrlHandlerMapping getWebDispatcherMapping(boolean disabled) throws Exception {
        setGeoserverConsoleDisabled(disabled);
        // tear down the test so that we close down the pre-existing Wicket filter
        tearDown(getTestData());
        setUp(getTestData());

        // Get the list of URL mappings from spring
        return (SimpleUrlHandlerMapping) applicationContext.getBean("webDispatcherMapping");
    }

    @Test
    public void testGeoServerConsoleDisabledTrue() throws Exception {
        // when the console is disabled the filePublisher method is used -- this
        // fetches files instead of delegating requests to wicket
        SimpleUrlHandlerMapping mapping = getWebDispatcherMapping(true);

        assertEquals(mapping.getUrlMap().get(WEB_MAPPING), CONSOLE_DISABLED_PUBLISHER);
        assertEquals(mapping.getUrlMap().get(WEB_WILDCARD_MAPPING), CONSOLE_DISABLED_PUBLISHER);
        assertEquals(
                mapping.getUrlMap().get(WEB_RESOURCES_WILDCARD_MAPPING),
                CONSOLE_DISABLED_PUBLISHER);
    }

    @Test
    public void testGeoserverConsoleDisabledFalse() throws Exception {
        // with the console enabled (default) requests are passed to wicket and
        // the gui is displayed
        SimpleUrlHandlerMapping mapping = getWebDispatcherMapping(false);

        assertEquals(mapping.getUrlMap().get(WEB_MAPPING), CONSOLE_ENABLED_PUBLISHER);
        assertEquals(mapping.getUrlMap().get(WEB_WILDCARD_MAPPING), CONSOLE_ENABLED_PUBLISHER);
        assertEquals(
                mapping.getUrlMap().get(WEB_RESOURCES_WILDCARD_MAPPING), CONSOLE_ENABLED_PUBLISHER);
    }

    @After
    public void cleanup() throws Exception {
        // restore default state
        setGeoserverConsoleDisabled(false);
        tearDown(getTestData());
    }
}

/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.assertTrue;

import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/** @author Carlo Cancellieri - GeoSolutions SAS */
public class AboutStatusControllerTest extends GeoServerSystemTestSupport {

    private static String BASEPATH = RestBaseController.ROOT_PATH;

    @Test
    public void testGetStatusHTML() throws Exception {
        String html = getAsString(BASEPATH + "/about/status");
        assertTrue(html.contains("Available"));
        assertTrue(html.contains("Enabled"));
    }

    @Test
    public void testGetStatusXML() throws Exception {
        getAsDOM(BASEPATH + "/about/status.xml");
    }

    @Test
    public void testGetStatusJSON() throws Exception {
        getAsJSON(BASEPATH + "/about/status.json");
    }

    @Test
    public void testGetSingleModule() throws Exception {
        String html = getAsString(BASEPATH + "/about/status/gs-main");
        assertTrue(html.contains("<b>Module</b> : gs-main"));
        assertTrue(html.contains("<b>Enabled</b> : true"));
    }

    @Test
    public void testMalformedModuleName() throws Exception {
        String html = getAsString(BASEPATH + "/about/status/fake1_module");
        assertTrue(html.contains("No such module"));
    }
}

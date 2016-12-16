/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest;

import static org.junit.Assert.assertTrue;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class ModuleStatusTest extends GeoServerSystemTestSupport {

    @Test
    public void testGetStatusHTML() throws Exception {
        String html = getAsString("/rest/about/status");
        assertTrue(html.contains("Available"));
        assertTrue(html.contains("Enabled"));
    }

    @Test
    public void testGetSingleModule() throws Exception {
        String html = getAsString("/rest/about/status/gs-main");
        assertTrue(html.contains("<b>Module</b> : gs-main"));
        assertTrue(html.contains("<b>Enabled</b> : true"));
    }

    @Test
    public void testMalformedModuleName() throws Exception {
        String html = getAsString("/rest/about/status/fake1_module");
        assertTrue(html.contains("No such module"));
    }

}

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
    public void testGetHTML() throws Exception {
        String html = getAsString("/rest/about/status");
        assertTrue(html.contains("Available"));
        assertTrue(html.contains("Enabled"));
    }

}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class APIConformanceTest {

    private static final APIConformance CORE =
            new APIConformance("https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/core");

    @Test
    public void testExtendName() {
        APIConformance html = CORE.extend("html");

        assertEquals("https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/html", html.getId());
        assertEquals("html", html.getProperty());
    }

    @Test
    public void testExtendHyphenatedName() {
        APIConformance spatialSubsetting = CORE.extend("spatial-subsetting");

        assertEquals(
                "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting", spatialSubsetting.getId());
        assertEquals("spatialSubsetting", spatialSubsetting.getProperty());
    }

    @Test
    public void testExtendURI() {
        APIConformance filter = CORE.extend(ConformanceClass.FILTER);

        assertEquals(ConformanceClass.FILTER, filter.getId());
    }
}

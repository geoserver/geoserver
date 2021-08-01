/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.coverages;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import org.junit.Test;

public class DomainSetTest extends CoveragesTestSupport {

    private static final double EPS = 1e-6;

    @Test
    public void testDemDomainSet() throws Exception {
        DocumentContext domain =
                getAsJSONPath("ogc/coverages/collections/rs:DEM/coverage/domainset", 200);

        // root properties
        assertEquals("DomainSetType", domain.read("type"));

        // model domain description
        DocumentContext generalGrid = readContext(domain, "generalGrid");
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", generalGrid.read("srsName"));
        assertEquals(Arrays.asList("Long", "Lat"), generalGrid.read("axisLabels"));

        DocumentContext longAxis = readContext(generalGrid, "axis[0]");
        assertEquals("RegularAxis", longAxis.read("type"));
        assertEquals("Long", longAxis.read("axisLabel"));
        assertEquals(145, longAxis.read("lowerBound", Double.class), EPS);
        assertEquals(146, longAxis.read("upperBound", Double.class), EPS);
        assertEquals(0.008333334, longAxis.read("resolution", Double.class), EPS);
        assertEquals("deg", longAxis.read("uomLabel"));

        DocumentContext latAxis = readContext(generalGrid, "axis[1]");
        assertEquals("RegularAxis", latAxis.read("type"));
        assertEquals("Lat", latAxis.read("axisLabel"));
        assertEquals(-43, latAxis.read("lowerBound", Double.class), EPS);
        assertEquals(-41, latAxis.read("upperBound", Double.class), EPS);
        assertEquals(0.008333334, latAxis.read("resolution", Double.class), EPS);
        assertEquals("deg", latAxis.read("uomLabel"));

        // raster space description
        DocumentContext gridLimits = readContext(generalGrid, "gridLimits");
        assertEquals("http://www.opengis.net/def/crs/OGC/0/Index2D", gridLimits.read("srsName"));
        assertEquals(Arrays.asList("i", "j"), gridLimits.read("axisLabels"));

        DocumentContext iAxis = readContext(gridLimits, "axis[0]");
        assertEquals("IndexAxis", iAxis.read("type"));
        assertEquals("i", iAxis.read("axisLabel"));
        assertEquals(0, (int) iAxis.read("lowerBound", Integer.class));
        assertEquals(119, (int) iAxis.read("upperBound", Integer.class));

        DocumentContext jAxis = readContext(gridLimits, "axis[1]");
        assertEquals("IndexAxis", jAxis.read("type"));
        assertEquals("j", jAxis.read("axisLabel"));
        assertEquals(0, (int) jAxis.read("lowerBound", Integer.class));
        assertEquals(239, (int) jAxis.read("upperBound", Integer.class));
    }
}

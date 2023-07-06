/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import static org.geoserver.catalog.DimensionPresentation.CONTINUOUS_INTERVAL;
import static org.geoserver.catalog.DimensionPresentation.DISCRETE_INTERVAL;
import static org.geoserver.catalog.ResourceInfo.TIME;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.geoserver.catalog.DimensionPresentation;
import org.junit.Test;

public class DomainSetTest extends CoveragesTestSupport {

    private static final double EPS = 1e-6;

    @Test
    public void testDemDomainSet() throws Exception {
        DocumentContext domain =
                getAsJSONPath("ogc/coverages/v1/collections/rs:DEM/coverage/domainset", 200);

        // root properties
        assertEquals("DomainSetType", domain.read("type"));

        // model domain description
        DocumentContext generalGrid = readContext(domain, "generalGrid");
        assertEquals(CoveragesService.DEFAULT_CRS, generalGrid.read("srsName"));
        assertEquals(Arrays.asList("Long", "Lat"), generalGrid.read("axisLabels"));

        DocumentContext longAxis = readContext(generalGrid, "axis[0]");
        assertEquals("RegularAxisType", longAxis.read("type"));
        assertEquals("Long", longAxis.read("axisLabel"));
        assertEquals(145, longAxis.read("lowerBound", Double.class), EPS);
        assertEquals(146, longAxis.read("upperBound", Double.class), EPS);
        assertEquals(0.008333334, longAxis.read("resolution", Double.class), EPS);
        assertEquals("deg", longAxis.read("uomLabel"));

        DocumentContext latAxis = readContext(generalGrid, "axis[1]");
        assertEquals("RegularAxisType", latAxis.read("type"));
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
        assertEquals("IndexAxisType", iAxis.read("type"));
        assertEquals("i", iAxis.read("axisLabel"));
        assertEquals(0, (int) iAxis.read("lowerBound", Integer.class));
        assertEquals(119, (int) iAxis.read("upperBound", Integer.class));

        DocumentContext jAxis = readContext(gridLimits, "axis[1]");
        assertEquals("IndexAxisType", jAxis.read("type"));
        assertEquals("j", jAxis.read("axisLabel"));
        assertEquals(0, (int) jAxis.read("lowerBound", Integer.class));
        assertEquals(239, (int) jAxis.read("upperBound", Integer.class));
    }

    @Test
    public void testTimeListPresentation() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, DimensionPresentation.LIST, null, null, null);

        DocumentContext domain =
                getAsJSONPath("ogc/coverages/v1/collections/sf:timeseries/coverage/domainset", 200);

        // root properties
        assertEquals("DomainSetType", domain.read("type"));

        // model domain description
        DocumentContext generalGrid = readContext(domain, "generalGrid");
        assertEquals(CoveragesService.DEFAULT_CRS, generalGrid.read("srsName"));
        assertEquals(Arrays.asList("Long", "Lat", "Time"), generalGrid.read("axisLabels"));

        // focus on the time axis
        DocumentContext timeAxis = readContext(generalGrid, "axis[2]");
        assertEquals("IrregularAxisType", timeAxis.read("type"));
        assertEquals("Time", timeAxis.read("axisLabel"));
        assertEquals("s", timeAxis.read("uomLabel"));
        List<String> expectedTimes =
                IntStream.range(2014, 2020)
                        .mapToObj(y -> y + "-01-01T00:00:00.000Z")
                        .collect(Collectors.toList());
        assertEquals(expectedTimes, timeAxis.read("coordinate"));

        // check the grid representation
        DocumentContext gridLimits = readContext(generalGrid, "gridLimits");
        assertEquals("http://www.opengis.net/def/crs/OGC/0/Index3D", gridLimits.read("srsName"));
        assertEquals(Arrays.asList("i", "j", "k"), gridLimits.read("axisLabels"));

        DocumentContext kAxis = readContext(gridLimits, "axis[2]");
        assertEquals("IndexAxisType", kAxis.read("type"));
        assertEquals("k", kAxis.read("axisLabel"));
        assertEquals(0, (int) kAxis.read("lowerBound", Integer.class));
        assertEquals(6, (int) kAxis.read("upperBound", Integer.class));
    }

    @Test
    public void testTimeIntervalPresentation() throws Exception {
        double oneYear = 1000d * 60 * 60 * 24 * 365;
        setupRasterDimension(TIMESERIES, TIME, DISCRETE_INTERVAL, oneYear, null, "s");

        DocumentContext domain =
                getAsJSONPath("ogc/coverages/v1/collections/sf:timeseries/coverage/domainset", 200);

        // root properties
        assertEquals("DomainSetType", domain.read("type"));

        // model domain description
        DocumentContext generalGrid = readContext(domain, "generalGrid");
        assertEquals(CoveragesService.DEFAULT_CRS, generalGrid.read("srsName"));
        assertEquals(Arrays.asList("Long", "Lat", "Time"), generalGrid.read("axisLabels"));

        // focus on the time axis
        DocumentContext timeAxis = readContext(generalGrid, "axis[2]");
        assertEquals("RegularAxisType", timeAxis.read("type"));
        assertEquals("Time", timeAxis.read("axisLabel"));
        assertEquals("y", timeAxis.read("uomLabel"));
        assertEquals(1, (int) timeAxis.read("resolution", Integer.class));
        assertEquals("2014-01-01T00:00:00.000Z", timeAxis.read("lowerBound"));
        assertEquals("2019-01-01T00:00:00.000Z", timeAxis.read("upperBound"));

        // check the grid representation
        DocumentContext gridLimits = readContext(generalGrid, "gridLimits");
        assertEquals("http://www.opengis.net/def/crs/OGC/0/Index3D", gridLimits.read("srsName"));
        assertEquals(Arrays.asList("i", "j", "k"), gridLimits.read("axisLabels"));

        DocumentContext kAxis = readContext(gridLimits, "axis[2]");
        assertEquals("IndexAxisType", kAxis.read("type"));
        assertEquals("k", kAxis.read("axisLabel"));
        assertEquals(0, (int) kAxis.read("lowerBound", Integer.class));
        assertEquals(5, (int) kAxis.read("upperBound", Integer.class));
    }

    @Test
    public void testTimeContinuousIntervalPresentation() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, CONTINUOUS_INTERVAL, null, null, "s");

        DocumentContext domain =
                getAsJSONPath("ogc/coverages/v1/collections/sf:timeseries/coverage/domainset", 200);

        // root properties
        assertEquals("DomainSetType", domain.read("type"));

        // model domain description
        DocumentContext generalGrid = readContext(domain, "generalGrid");
        assertEquals(CoveragesService.DEFAULT_CRS, generalGrid.read("srsName"));
        assertEquals(Arrays.asList("Long", "Lat", "Time"), generalGrid.read("axisLabels"));

        // focus on the time axis
        DocumentContext timeAxis = readContext(generalGrid, "axis[2]");
        assertEquals("RegularAxisType", timeAxis.read("type"));
        assertEquals("Time", timeAxis.read("axisLabel"));
        assertEquals("s", timeAxis.read("uomLabel"));
        assertEquals("2014-01-01T00:00:00.000Z", timeAxis.read("lowerBound"));
        assertEquals("2019-01-01T00:00:00.000Z", timeAxis.read("upperBound"));

        // check the grid representation
        DocumentContext gridLimits = readContext(generalGrid, "gridLimits");
        assertEquals("http://www.opengis.net/def/crs/OGC/0/Index3D", gridLimits.read("srsName"));
        assertEquals(Arrays.asList("i", "j", "k"), gridLimits.read("axisLabels"));

        DocumentContext kAxis = readContext(gridLimits, "axis[2]");
        assertEquals("IndexAxisType", kAxis.read("type"));
        assertEquals("k", kAxis.read("axisLabel"));
        assertEquals(0, (int) kAxis.read("lowerBound", Integer.class));
        // 1826 days between the two dates, due to a leap year, 2016
        assertEquals(157766400, (int) kAxis.read("upperBound", Integer.class));
    }
}

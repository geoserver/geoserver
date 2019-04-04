/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geoserver.wcs2_0.response.GMLCoverageResponseDelegate;
import org.geotools.util.NumberRange;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Testing {@link GMLCoverageResponseDelegate}
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class GMLGetCoverageKVPTest extends WCSTestSupport {

    private static final double DELTA = 1E-6;

    @Test
    public void gmlFormat() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&format=application%2Fgml%2Bxml");

        assertEquals("application/gml+xml", response.getContentType());
        Document dom = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
    }

    @Test
    public void gmlFormatCoverageBandDetails() throws Exception {
        Catalog catalog = getCatalog();

        CoverageInfo c = catalog.getCoverageByName("wcs", "BlueMarble");
        List<CoverageDimensionInfo> dimensions = c.getDimensions();
        CoverageDimensionInfo dimension = dimensions.get(0);
        assertEquals("RED_BAND", dimension.getName());
        NumberRange range = dimension.getRange();
        assertEquals(Double.NEGATIVE_INFINITY, range.getMinimum(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, range.getMaximum(), DELTA);
        assertEquals("GridSampleDimension[-Infinity,Infinity]", dimension.getDescription());
        List<Double> nullValues = dimension.getNullValues();
        assertEquals(0, nullValues.size());
        assertEquals("W.m-2.Sr-1", dimension.getUnit());

        int i = 1;
        for (CoverageDimensionInfo dimensionInfo : dimensions) {
            // Updating dimension properties
            dimensionInfo.getNullValues().add(-999d);
            dimensionInfo.setDescription("GridSampleDimension[-100.0,1000.0]");
            dimensionInfo.setUnit("m");
            dimensionInfo.setRange(NumberRange.create(-100, 1000));
            dimensionInfo.setName("Band" + (i++));
        }
        catalog.save(c);

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&format=application%2Fgml%2Bxml");
        Document dom = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        String name = xpath.evaluate("//swe:field/@name", dom);
        assertEquals("Band1", name);
        String interval = xpath.evaluate("//swe:interval", dom);
        assertEquals("-100 1000", interval);
        String unit = xpath.evaluate("//swe:uom/@code", dom);
        assertEquals("m", unit);
        String noData = xpath.evaluate("//swe:nilValue", dom);
        assertEquals("-999.0", noData);
    }
}

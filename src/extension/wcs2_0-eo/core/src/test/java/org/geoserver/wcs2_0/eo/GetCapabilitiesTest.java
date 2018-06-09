/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

import static org.junit.Assert.assertEquals;

import org.geoserver.wcs.WCSInfo;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetCapabilitiesTest extends WCSEOTestSupport {

    @Test
    public void testEOExtensions() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&version=2.0.1&service=WCS");
        // print(dom);

        // operations metadata checks
        assertEquals(
                "1", xpath.evaluate("count(//ows:Operation[@name='DescribeEOCoverageSet'])", dom));
        assertEquals("1", xpath.evaluate("count(//ows:Constraint[@name='CountDefault'])", dom));
        assertEquals(
                "20",
                xpath.evaluate("//ows:Constraint[@name='CountDefault']/ows:DefaultValue", dom));

        // dataset series checks
        assertEquals("4", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary)", dom));
        // check time ranges
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss'])",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss']/ows:WGS84BoundingBox)",
                        dom));
        assertEquals(
                "2008-10-31T00:00:00.000Z",
                xpath.evaluate(
                        "//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss']/gml:TimePeriod/gml:beginPosition",
                        dom));
        assertEquals(
                "2008-11-07T00:00:00.000Z",
                xpath.evaluate(
                        "//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss']/gml:TimePeriod/gml:endPosition",
                        dom));
        // check water temp
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss'])",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss']/ows:WGS84BoundingBox)",
                        dom));
        assertEquals(
                "2008-10-31T00:00:00.000Z",
                xpath.evaluate(
                        "//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss']/gml:TimePeriod/gml:beginPosition",
                        dom));
        assertEquals(
                "2008-11-01T00:00:00.000Z",
                xpath.evaluate(
                        "//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss']/gml:TimePeriod/gml:endPosition",
                        dom));
    }

    @Test
    public void testDisableEOExtensions() throws Exception {
        // disable EO extensions
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(WCSEOMetadata.ENABLED.key, false);
        getGeoServer().save(wcs);

        Document dom = getAsDOM("wcs?request=GetCapabilities&version=2.0.1&service=WCS");

        assertEquals(
                "0", xpath.evaluate("count(//ows:Operation[@name='DescribeEOCoverageSet'])", dom));
        assertEquals("0", xpath.evaluate("count(//ows:Constraint[@name='CountDefault'])", dom));
        assertEquals("0", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary)", dom));
    }
}

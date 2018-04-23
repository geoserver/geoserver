/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import si.uom.SI;

import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.measure.Measure;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetFeatureCurvesTest extends WFSCurvesTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do not call super, we only need the curved data sets
        testData.setUpSecurity();
    }

    @Test
    public void testLinearizeWFS10() throws Exception {
        Document dom = getAsDOM("wfs?service=wfs&version=1.0&request=GetFeature&typeName="
                + getLayerId(CURVELINES));
        // print(dom);

        assertEquals("1", xpath.evaluate(
                "count(//gml:featureMember/cite:curvelines[@fid='cp.1']/cite:geom/gml:LineString)",
                dom));
        assertEquals("1", xpath.evaluate(
                "count(//gml:featureMember/cite:curvelines[@fid='cp.2']/cite:geom/gml:LineString)",
                dom));
        assertEquals("1", xpath.evaluate(
                "count(//gml:featureMember/cite:curvelines[@fid='cp.3']/cite:geom/gml:LineString)",
                dom));

        // compute number of coordinates
        String coords = xpath
                .evaluate(
                        "//gml:featureMember/cite:curvelines[@fid='cp.2']/cite:geom/gml:LineString/gml:coordinates",
                        dom);
        int coordCountDefault = coords.split("\\s+").length;

        // now alter the feature type and set a linearization tolerance
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(CURVELINES));
        ft.setCircularArcPresent(true);
        ft.setLinearizationTolerance(new Measure(1, SI.METRE));
        getCatalog().save(ft);

        dom = getAsDOM("wfs?service=wfs&version=1.0&request=GetFeature&typeName="
                + getLayerId(CURVELINES));
        // print(dom);
        int coordCount100m = countCoordinates(dom, xpath,
                "//gml:featureMember/cite:curvelines[@fid='cp.2']/cite:geom/gml:LineString/gml:coordinates");
        assertTrue(coordCount100m > coordCountDefault);
    }

    private int countCoordinates(Document dom, XpathEngine xpath, String path)
            throws XpathException {
        String coords = xpath.evaluate(path, dom);
        int coordCount = coords.split("\\s+").length;
        return coordCount;
    }

    @Test
    public void testCurveLineWFS11() throws Exception {
        Document dom = getAsDOM("wfs?service=wfs&version=1.1&request=GetFeature&typeName="
                + getLayerId(CURVELINES));
        // print(dom);

        // check the compound curve
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvelines[@gml:id='cp.1']/cite:geom/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                10,
                countCoordinates(dom, xpath,
                        "//cite:curvelines[@gml:id='cp.1']/cite:geom/gml:Curve/gml:segments/gml:ArcString/gml:posList"));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvelines[@gml:id='cp.1']/cite:geom/gml:Curve/gml:segments/gml:LineStringSegment)",
                        dom));
        assertEquals(
                8,
                countCoordinates(
                        dom,
                        xpath,
                        "//cite:curvelines[@gml:id='cp.1']/cite:geom/gml:Curve/gml:segments/gml:LineStringSegment/gml:posList"));

        // check the circle
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvelines[@gml:id='cp.2']/cite:geom/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                10,
                countCoordinates(dom, xpath,
                        "//cite:curvelines[@gml:id='cp.2']/cite:geom/gml:Curve/gml:segments/gml:ArcString/gml:posList"));

        // check the wave
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvelines[@gml:id='cp.3']/cite:geom/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                10,
                countCoordinates(dom, xpath,
                        "//cite:curvelines[@gml:id='cp.3']/cite:geom/gml:Curve/gml:segments/gml:ArcString/gml:posList"));
    }

    @Test
    public void testCurveMultiLineWFS11() throws Exception {
        Document dom = getAsDOM("wfs?service=wfs&version=1.1&request=GetFeature&typeName="
                + getLayerId(CURVEMULTILINES) + "&featureid=cp.1");
        // print(dom);

        // check the compound curve
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvemultilines[@gml:id='cp.1']/cite:geom/gml:MultiCurve/gml:curveMember/gml:LineString)",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvemultilines[@gml:id='cp.1']/cite:geom/gml:MultiCurve/gml:curveMember/gml:Curve)",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvemultilines[@gml:id='cp.1']/cite:geom/gml:MultiCurve/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvemultilines[@gml:id='cp.1']/cite:geom/gml:MultiCurve/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
    }
    
    @Test
    public void testCurvePolygons() throws Exception {
        Document dom = getAsDOM("wfs?service=wfs&version=1.1&request=GetFeature&typeName="
                + getLayerId(CURVEPOLYGONS) + "&featureid=cp.1");
        // print(dom);

        // check the compound curve
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvepolygons[@gml:id='cp.1']/cite:geom/gml:Polygon/gml:exterior/gml:Ring/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvepolygons[@gml:id='cp.1']/cite:geom/gml:Polygon/gml:exterior/gml:Ring/gml:curveMember/gml:LineString)",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvepolygons[@gml:id='cp.1']/cite:geom/gml:Polygon/gml:interior/gml:Ring/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
    }


}

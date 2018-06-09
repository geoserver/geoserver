/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetFeatureCurvesTest extends WFS20TestSupport {

    QName CURVELINES = new QName(MockData.CITE_URI, "curvelines", MockData.CITE_PREFIX);

    QName CURVEMULTILINES = new QName(MockData.CITE_URI, "curvemultilines", MockData.CITE_PREFIX);

    QName CURVEPOLYGONS = new QName(MockData.CITE_URI, "curvepolygons", MockData.CITE_PREFIX);

    XpathEngine xpath;

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        // TODO Auto-generated method stub
        super.setUpInternal(testData);

        testData.addWorkspace(MockData.CITE_PREFIX, MockData.CITE_URI, getCatalog());
        testData.addVectorLayer(
                CURVELINES,
                Collections.EMPTY_MAP,
                "curvelines.properties",
                MockData.class,
                getCatalog());
        testData.addVectorLayer(
                CURVEMULTILINES,
                Collections.EMPTY_MAP,
                "curvemultilines.properties",
                MockData.class,
                getCatalog());
        testData.addVectorLayer(
                CURVEPOLYGONS,
                Collections.EMPTY_MAP,
                "curvepolygons.properties",
                MockData.class,
                getCatalog());

        FeatureTypeInfo curveLines = getCatalog().getFeatureTypeByName(getLayerId(CURVELINES));
        curveLines.setCircularArcPresent(true);
        curveLines.setLinearizationTolerance(null);
        getCatalog().save(curveLines);

        FeatureTypeInfo curveMultiLines =
                getCatalog().getFeatureTypeByName(getLayerId(CURVEMULTILINES));
        curveMultiLines.setCircularArcPresent(true);
        curveMultiLines.setLinearizationTolerance(null);
        getCatalog().save(curveMultiLines);
    }

    @Before
    public void setXPath() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do not call super, we only need the curved data sets
        testData.setUpSecurity();
    }

    private int countCoordinates(Document dom, XpathEngine xpath, String path)
            throws XpathException {
        String coords = xpath.evaluate(path, dom);
        int coordCount = coords.split("\\s+").length;
        return coordCount;
    }

    @Test
    public void testCurveLine() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=wfs&version=2.0&request=GetFeature&typeName="
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
                countCoordinates(
                        dom,
                        xpath,
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
                countCoordinates(
                        dom,
                        xpath,
                        "//cite:curvelines[@gml:id='cp.2']/cite:geom/gml:Curve/gml:segments/gml:ArcString/gml:posList"));

        // check the wave
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvelines[@gml:id='cp.3']/cite:geom/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                10,
                countCoordinates(
                        dom,
                        xpath,
                        "//cite:curvelines[@gml:id='cp.3']/cite:geom/gml:Curve/gml:segments/gml:ArcString/gml:posList"));
    }

    @Test
    public void testCurveMultiLine() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=wfs&version=2.0&request=GetFeature&typeName="
                                + getLayerId(CURVEMULTILINES)
                                + "&featureid=cp.1");
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
        Document dom =
                getAsDOM(
                        "wfs?service=wfs&version=2.0&request=GetFeature&typeName="
                                + getLayerId(CURVEPOLYGONS)
                                + "&featureid=cp.1");
        // print(dom);

        // check the compound curve
        xpath.evaluate(
                "count(//cite:curvepolygons[@gml:id='cp.1']/cite:geom/gml:Polygon/gml:exterior/gml:Ring/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                dom);
        xpath.evaluate(
                "count(//cite:curvepolygons[@gml:id='cp.1']/cite:geom/gml:Polygon/gml:exterior/gml:Ring/gml:curveMember/gml:LineString)",
                dom);
        xpath.evaluate(
                "count(//cite:curvepolygons[@gml:id='cp.1']/cite:geom/gml:Polygon/gml:interior/gml:Ring/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                dom);
    }
}

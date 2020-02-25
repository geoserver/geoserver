/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import javax.xml.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.coordinatesequence.CoordinateSequences;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.w3c.dom.Document;

public class Transaction3DTest extends WFSTestSupport {

    static final QName FULL3D =
            new QName(SystemTestData.CITE_URI, "full3d", SystemTestData.CITE_PREFIX);
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    private XpathEngine xpath;
    private WKTReader wkt = new WKTReader();

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // only need the 3d test data, not the rest
    }

    @Before
    public void setupXPathEngine() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Before
    public void revert() throws Exception {
        getTestData().addVectorLayer(FULL3D, Collections.emptyMap(), getClass(), getCatalog());
    }

    @Test
    public void testInsert3DPoint() throws Exception {
        Document insertDom = postRequest("insertPoint3d.xml");
        print(insertDom);
        String fid = assertSuccesfulInsert(insertDom);
        Document featureDom = getFeature(fid);
        print(featureDom);
        assertEquals("New point", xpath.evaluate("//cite:full3d/gml:name", featureDom));
        assertEquals(
                "3", xpath.evaluate("//cite:full3d/cite:geom/gml:Point/@srsDimension", featureDom));
        assertEquals(
                "204330 491816 16",
                xpath.evaluate("//cite:full3d/cite:geom/gml:Point/gml:pos", featureDom));
        // check it's actually 3d as a geometry
        SimpleFeature feature = getFeatureFromStore(fid);
        Geometry g = (Geometry) feature.getDefaultGeometry();
        assertEqualND(g, wkt.read("POINT(204330 491816 16)"));
    }

    @Test
    public void testInsert3DLinestring() throws Exception {
        Document insertDom = postRequest("insertLinestring3d.xml");
        // print(insertDom);
        String fid = assertSuccesfulInsert(insertDom);
        Document featureDom = getFeature(fid);
        assertEquals("New line", xpath.evaluate("//cite:full3d/gml:name", featureDom));
        assertEquals(
                "3",
                xpath.evaluate("//cite:full3d/cite:geom/gml:LineString/@srsDimension", featureDom));
        assertEquals(
                "204330 491816 16 204319 491814 16",
                xpath.evaluate("//cite:full3d/cite:geom/gml:LineString/gml:posList", featureDom));
        // check it's actually 3d as a geometry
        SimpleFeature feature = getFeatureFromStore(fid);
        Geometry g = (Geometry) feature.getDefaultGeometry();
        assertEqualND(g, wkt.read("LINESTRING(204330 491816 16, 204319 491814 16)"));
    }

    @Test
    public void testInsert3DPolygon() throws Exception {
        Document insertDom = postRequest("insertPolygon3d.xml");
        // print(insertDom);
        String fid = assertSuccesfulInsert(insertDom);
        Document featureDom = getFeature(fid);
        // print(featureDom);
        assertEquals("New polygon", xpath.evaluate("//cite:full3d/gml:name", featureDom));
        assertEquals(
                "3",
                xpath.evaluate("//cite:full3d/cite:geom/gml:Polygon/@srsDimension", featureDom));
        assertEquals(
                "94000 471000 10 94001 471000 11 94001 471001 12 94000 471001 13 94000 471000 10",
                xpath.evaluate(
                        "//cite:full3d/cite:geom/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                        featureDom));
        // check it's actually 3d as a geometry
        SimpleFeature feature = getFeatureFromStore(fid);
        Geometry g = (Geometry) feature.getDefaultGeometry();
        assertEqualND(
                g,
                wkt.read(
                        "POLYGON((94000 471000 10, 94001 471000 11, 94001 471001 12, 94000 471001 13, 94000 471000 10))"));
    }

    @Test
    public void testDelete3DPoint() throws Exception {
        Document deleteDom = postRequest("delete3d.xml", "${id}", "full3d.point");
        // print(deleteDom);
        assertSuccesfulDelete(deleteDom);
        assertNull(getFeatureFromStore("full3d.point"));
        assertEquals(2, getCountFromStore(Filter.INCLUDE));
    }

    @Test
    public void testDelete3DLineString() throws Exception {
        Document deleteDom = postRequest("delete3d.xml", "${id}", "full3d.ls");
        // print(deleteDom);
        assertSuccesfulDelete(deleteDom);
        assertNull(getFeatureFromStore("full3d.ls"));
        assertEquals(2, getCountFromStore(Filter.INCLUDE));
    }

    @Test
    public void testDelete3DPolygon() throws Exception {
        Document deleteDom = postRequest("delete3d.xml", "${id}", "full3d.poly");
        // print(deleteDom);
        assertSuccesfulDelete(deleteDom);
        assertNull(getFeatureFromStore("full3d.poly"));
        assertEquals(2, getCountFromStore(Filter.INCLUDE));
    }

    @Test
    public void testUpdate3DPoint() throws Exception {
        Document updateDom = postRequest("updatePoint3d.xml");
        // print(deleteDom);
        assertSuccesfulUpdate(updateDom);
        SimpleFeature feature = getFeatureFromStore("full3d.point");
        Geometry g = (Geometry) feature.getDefaultGeometry();
        assertEqualND(g, wkt.read("POINT(204330 491816 16)"));
        assertEquals(3, getCountFromStore(Filter.INCLUDE));
    }

    @Test
    public void testUpdate3DLinestring() throws Exception {
        Document updateDom = postRequest("updateLinestring3d.xml");
        // print(deleteDom);
        assertSuccesfulUpdate(updateDom);
        SimpleFeature feature = getFeatureFromStore("full3d.ls");
        Geometry g = (Geometry) feature.getDefaultGeometry();
        assertEqualND(g, wkt.read("LINESTRING(204330 491816 16, 204319 491814 16)"));
        assertEquals(3, getCountFromStore(Filter.INCLUDE));
    }

    @Test
    public void testUpdate3DPolygon() throws Exception {
        Document updateDom = postRequest("updatePolygon3d.xml");
        // print(deleteDom);
        assertSuccesfulUpdate(updateDom);
        SimpleFeature feature = getFeatureFromStore("full3d.poly");
        Geometry g = (Geometry) feature.getDefaultGeometry();
        assertEqualND(
                g,
                wkt.read(
                        "POLYGON((94000 471000 10, 94001 471000 11, 94001 471001 12, 94000 471001 13, 94000 471000 10))"));
        assertEquals(3, getCountFromStore(Filter.INCLUDE));
    }

    private String assertSuccesfulInsert(Document dom) throws XpathException {
        assertEquals(
                "1",
                xpath.evaluate(
                        "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalInserted", dom));
        return xpath.evaluate(
                "/wfs:TransactionResponse/wfs:InsertResults/wfs:Feature/ogc:FeatureId/@fid", dom);
    }

    private void assertSuccesfulDelete(Document dom) throws XpathException {
        assertEquals(
                "1",
                xpath.evaluate(
                        "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalDeleted", dom));
    }

    private void assertSuccesfulUpdate(Document dom) throws XpathException {
        assertEquals(
                "1",
                xpath.evaluate(
                        "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalUpdated", dom));
    }

    private void assertEqualND(Geometry test, Geometry expected) {
        WKTWriter writer = new WKTWriter(3);
        assertTrue(
                "Expected " + writer.write(expected) + " but got " + writer.write(test),
                CoordinateSequences.equalsND(expected, test));
    }

    private Document postRequest(String requestFile, String... variableMap)
            throws IOException, Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream(requestFile), "UTF-8");
        if (variableMap != null) {
            for (int i = 0; i < variableMap.length; i += 2) {
                String key = variableMap[i];
                String value = variableMap[i + 1];
                xml = xml.replace(key, value);
            }
        }
        Document dom = postAsDOM("wfs", xml);
        return dom;
    }

    private Document getFeature(String featureId) throws IOException, Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName="
                                + getLayerId(FULL3D)
                                + "&featureId="
                                + featureId);
        assertEquals("1", xpath.evaluate("count(//cite:full3d)", dom));
        return dom;
    }

    private SimpleFeature getFeatureFromStore(String fid) throws IOException {
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(getLayerId(FULL3D));
        SimpleFeatureSource featureSource =
                (SimpleFeatureSource) ftInfo.getFeatureSource(null, null);
        SimpleFeatureCollection fc = featureSource.getFeatures(FF.id(FF.featureId(fid)));
        SimpleFeature first = DataUtilities.first(fc);
        return first;
    }

    private int getCountFromStore(Filter filter) throws IOException {
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(getLayerId(FULL3D));
        SimpleFeatureSource featureSource =
                (SimpleFeatureSource) ftInfo.getFeatureSource(null, null);
        SimpleFeatureCollection fc = featureSource.getFeatures(filter);
        return fc.size();
    }
}

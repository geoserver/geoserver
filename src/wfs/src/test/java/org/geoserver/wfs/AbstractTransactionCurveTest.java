/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.CircularArc;
import org.geotools.geometry.jts.CircularRing;
import org.geotools.geometry.jts.CircularString;
import org.geotools.geometry.jts.CompoundCurvedGeometry;
import org.geotools.geometry.jts.CurvedGeometries;
import org.geotools.geometry.jts.SingleCurvedGeometry;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;

/**
 * Base class for WFS-T curve test, it expects to find the test requests in the same package as its
 * implementing subclasses
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractTransactionCurveTest extends WFSCurvesTestSupport {

    @Test
    public void testInsertArc() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("insertArc.xml"), "UTF-8");
        Document dom = postAsDOM("wfs", xml);

        // print(dom);
        checkSuccesfulTransaction(dom, 1, 0, 0);

        SimpleFeature first = getSingleFeature(CURVELINES, "Arc far away");

        Geometry g = (Geometry) first.getDefaultGeometry();
        assertNotNull(g);
        assertTrue(g instanceof SingleCurvedGeometry);
        SingleCurvedGeometry<?> curved = (SingleCurvedGeometry<?>) g;
        double[] cp = curved.getControlPoints();
        assertArrayEquals(new double[] {10, 15, 15, 20, 20, 15}, cp, 0d);
    }

    @Test
    public void testUpdateCompoundCurve() throws Exception {
        String xml =
                IOUtils.toString(
                        getClass().getResourceAsStream("updateCompoundCurve.xml"), "UTF-8");
        Document dom = postAsDOM("wfs", xml);

        // print(dom);
        checkSuccesfulTransaction(dom, 0, 1, 0);

        SimpleFeature first = getSingleFeature(CURVELINES, "Compound");

        Geometry g = (Geometry) first.getDefaultGeometry();
        assertNotNull(g);
        assertTrue(g instanceof CompoundCurvedGeometry<?>);
        CompoundCurvedGeometry<?> compound = (CompoundCurvedGeometry<?>) g;
        List<LineString> components = compound.getComponents();
        assertEquals(3, components.size());

        LineString ls1 = components.get(0);
        assertEquals(2, ls1.getNumPoints());
        assertEquals(new Coordinate(10, 45), ls1.getCoordinateN(0));
        assertEquals(new Coordinate(20, 45), ls1.getCoordinateN(1));

        CircularString cs = (CircularString) components.get(1);
        assertArrayEquals(
                new double[] {20.0, 45.0, 23.0, 48.0, 20.0, 51.0}, cs.getControlPoints(), 0d);

        LineString ls2 = components.get(2);
        assertEquals(2, ls2.getNumPoints());
        assertEquals(new Coordinate(20, 51), ls2.getCoordinateN(0));
        assertEquals(new Coordinate(10, 51), ls2.getCoordinateN(1));
    }

    @Test
    public void testInsertCurvePolygon() throws Exception {
        String xml =
                IOUtils.toString(getClass().getResourceAsStream("insertCurvePolygon.xml"), "UTF-8");
        Document dom = postAsDOM("wfs", xml);

        // print(dom);
        checkSuccesfulTransaction(dom, 1, 0, 0);

        SimpleFeature first = getSingleFeature(CURVEPOLYGONS, "Circle2");
        Geometry g = (Geometry) first.getDefaultGeometry();
        assertNotNull(g);
        assertTrue(g instanceof Polygon);
        Polygon p = (Polygon) g;
        assertEquals(0, p.getNumInteriorRing());

        // exterior ring checks
        assertTrue(p.getExteriorRing() instanceof CircularRing);
        CircularRing shell = (CircularRing) p.getExteriorRing();
        assertTrue(CurvedGeometries.isCircle(shell));
        CircularArc arc = shell.getArcN(0);
        assertEquals(5, arc.getRadius(), 0d);
        assertEquals(new Coordinate(15, 50), arc.getCenter());
    }

    @Test
    public void testInsertMultiCurve() throws Exception {
        String xml =
                IOUtils.toString(getClass().getResourceAsStream("insertMultiCurve.xml"), "UTF-8");
        Document dom = postAsDOM("wfs", xml);

        // print(dom);
        checkSuccesfulTransaction(dom, 1, 0, 0);

        SimpleFeature first = getSingleFeature(CURVEMULTILINES, "MNew");
        Geometry g = (Geometry) first.getDefaultGeometry();

        assertTrue(g instanceof MultiLineString);

        MultiLineString mc = (MultiLineString) g;

        LineString ls = (LineString) mc.getGeometryN(0);
        assertEquals(2, ls.getNumPoints());
        assertEquals(new Coordinate(0, 0), ls.getCoordinateN(0));
        assertEquals(new Coordinate(5, 5), ls.getCoordinateN(1));

        CircularString cs = (CircularString) mc.getGeometryN(1);
        assertArrayEquals(new double[] {4, 0, 4, 4, 8, 4}, cs.getControlPoints(), 0d);
    }

    private SimpleFeature getSingleFeature(QName typeName, String featureName)
            throws IOException, CQLException {
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(typeName));
        SimpleFeatureSource fs = (SimpleFeatureSource) ft.getFeatureSource(null, null);
        SimpleFeature first =
                DataUtilities.first(fs.getFeatures(ECQL.toFilter("name = '" + featureName + "'")));
        assertNotNull(first);
        return first;
    }

    private String checkSuccesfulTransaction(Document dom, int inserted, int updated, int deleted)
            throws XpathException {
        assertEquals(
                String.valueOf(inserted),
                xpath.evaluate(
                        "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalInserted", dom));
        assertEquals(
                String.valueOf(updated),
                xpath.evaluate(
                        "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalUpdated", dom));
        assertEquals(
                String.valueOf(deleted),
                xpath.evaluate(
                        "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalDeleted", dom));
        return xpath.evaluate("//ogc:FeatureId/@fid", dom);
    }
}

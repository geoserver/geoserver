/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.xacml.geoxacml.test;

import junit.framework.TestCase;

import org.geotools.xacml.geoxacml.attr.GMLVersion;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.geotools.xacml.test.TestSupport;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.test.TestPolicyFinderModule;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author Christian Mueller
 * 
 *         Test for GML3 handling
 * 
 */
public class PolicyGML3Test extends TestCase {

    static String xmlTemplate = null;

    PolicyFinderModule policyFinderModule = new TestPolicyFinderModule();

    public void testPoint() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "PointPolicy.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("Point"));
            assertTrue(attr.getSrsDimension() == 2);
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            Point point = (Point) attr.getGeometry();

            assertTrue(point.getX() == 7.7);
            assertTrue(point.getY() == 8.8);
        }

    }

    private AbstractPolicy policyFromAttributeEncoding(GeometryAttribute attr, String xmlTemplate) {
        String gmlEncoded = attr.encode();
        String newXml = xmlTemplate.replaceAll("GMLDATA", gmlEncoded);
        return TestSupport.policyFromXML(newXml);
    }

    @Override
    protected void setUp() throws Exception {
        GeoXACML.initialize();
        TestSupport.initOutputDir();
        if (xmlTemplate == null) {
            xmlTemplate = TestSupport.fileContentAsString(TestSupport.getGeoXACMLFNFor("gml3",
                    "PolicyTemplate.xml"));
        }
        super.setUp();
    }

    public void testLinearRing() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "LinearRingPolicy.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("LinearRing"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            LinearRing lr = (LinearRing) attr.getGeometry();
            Coordinate[] coords = lr.getCoordinates();
            assertTrue(coords[0].x == -1.1);
            assertTrue(coords[0].y == -2.2);
            assertTrue(coords[1].x == -3.3);
            assertTrue(coords[1].y == -4.4);
            assertTrue(coords[2].x == -5.5);
            assertTrue(coords[2].y == -6.6);
            assertTrue(coords[3].x == -1.1);
            assertTrue(coords[3].y == -2.2);

        }

    }

    public void testLinearRing2() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "LinearRingPolicy2.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("LinearRing2"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            LinearRing lr = (LinearRing) attr.getGeometry();
            Coordinate[] coords = lr.getCoordinates();
            assertTrue(coords[0].x == -1.1);
            assertTrue(coords[0].y == -2.2);
            assertTrue(coords[1].x == -3.3);
            assertTrue(coords[1].y == -4.4);
            assertTrue(coords[2].x == -5.5);
            assertTrue(coords[2].y == -6.6);
            assertTrue(coords[3].x == -1.1);
            assertTrue(coords[3].y == -2.2);

        }

    }

    public void testLineString() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "LineStringPolicy.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("LineString"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            LineString ls = (LineString) attr.getGeometry();
            Coordinate[] coords = ls.getCoordinates();
            assertTrue(coords[0].x == -1.1);
            assertTrue(coords[0].y == -2.2);
            assertTrue(coords[1].x == -3.3);
            assertTrue(coords[1].y == -4.4);
            assertTrue(coords[2].x == -5.5);
            assertTrue(coords[2].y == -6.6);
        }

    }

    public void testLineString2() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "LineStringPolicy2.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("LineString2"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            LineString ls = (LineString) attr.getGeometry();
            Coordinate[] coords = ls.getCoordinates();
            assertTrue(coords[0].x == -1.1);
            assertTrue(coords[0].y == -2.2);
            assertTrue(coords[1].x == -3.3);
            assertTrue(coords[1].y == -4.4);
            assertTrue(coords[2].x == -5.5);
            assertTrue(coords[2].y == -6.6);
        }

    }

    public void testPolygon() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "PolygonPolicy.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("Polygon"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            Polygon poly = (Polygon) attr.getGeometry();
            Coordinate[] coords = poly.getExteriorRing().getCoordinates();
            assertTrue(coords[0].x == 0);
            assertTrue(coords[0].y == 0);
            assertTrue(coords[1].x == 0);
            assertTrue(coords[1].y == 100);
            assertTrue(coords[2].x == 100);
            assertTrue(coords[2].y == 100);
            assertTrue(coords[3].x == 100);
            assertTrue(coords[3].y == 0);
            assertTrue(coords[4].x == 0);
            assertTrue(coords[4].y == 0);

            coords = poly.getInteriorRingN(0).getCoordinates();
            assertTrue(coords[0].x == 10);
            assertTrue(coords[0].y == 10);
            assertTrue(coords[1].x == 10);
            assertTrue(coords[1].y == 20);
            assertTrue(coords[2].x == 20);
            assertTrue(coords[2].y == 20);
            assertTrue(coords[3].x == 20);
            assertTrue(coords[3].y == 10);
            assertTrue(coords[4].x == 10);
            assertTrue(coords[4].y == 10);

            coords = poly.getInteriorRingN(1).getCoordinates();
            assertTrue(coords[0].x == 50);
            assertTrue(coords[0].y == 50);
            assertTrue(coords[1].x == 50);
            assertTrue(coords[1].y == 60);
            assertTrue(coords[2].x == 60);
            assertTrue(coords[2].y == 60);
            assertTrue(coords[3].x == 60);
            assertTrue(coords[3].y == 50);
            assertTrue(coords[4].x == 50);
            assertTrue(coords[4].y == 50);

        }

    }

    public void testEnvelope() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "EnvelopePolicy.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid() == null);
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            Envelope env = attr.getGeometry().getEnvelopeInternal();

            assertTrue(env.getMinX() == 1.1);
            assertTrue(env.getMinY() == 0.0);
            assertTrue(env.getMaxX() == 5.5);
            assertTrue(env.getMaxY() == 4.4);
        }
    }

    public void testEnvelope2() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "EnvelopePolicy2.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid() == null);
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            Envelope env = attr.getGeometry().getEnvelopeInternal();

            assertTrue(env.getMinX() == 1.1);
            assertTrue(env.getMinY() == 0.0);
            assertTrue(env.getMaxX() == 5.5);
            assertTrue(env.getMaxY() == 4.4);
        }
    }

    public void testMultiPoint() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "MultiPointPolicy.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("MultiPoint"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            MultiPoint multipoint = (MultiPoint) attr.getGeometry();
            Point point = (Point) multipoint.getGeometryN(0);
            assertTrue(point.getX() == 5.5);
            assertTrue(point.getY() == 6.6);

            point = (Point) multipoint.getGeometryN(1);
            assertTrue(point.getX() == 7.7);
            assertTrue(point.getY() == 8.8);
        }

    }

    public void testMultiCurve() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "MultiCurvePolicy.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("MultiCurve"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            MultiLineString mls = (MultiLineString) attr.getGeometry();
            LineString ls = (LineString) mls.getGeometryN(0);
            Coordinate[] coords = ls.getCoordinates();
            assertTrue(coords[0].x == -1.1);
            assertTrue(coords[0].y == -2.2);
            assertTrue(coords[1].x == -3.3);
            assertTrue(coords[1].y == -4.4);
            assertTrue(coords[2].x == -5.5);
            assertTrue(coords[2].y == -6.6);

            ls = (LineString) mls.getGeometryN(1);
            coords = ls.getCoordinates();
            assertTrue(coords[0].x == 1.1);
            assertTrue(coords[0].y == 2.2);
            assertTrue(coords[1].x == 3.3);
            assertTrue(coords[1].y == 4.4);
            assertTrue(coords[2].x == 5.5);
            assertTrue(coords[2].y == 6.6);

        }

    }

    public void testMultiCurve2() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "MultiCurvePolicy2.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("MultiCurve"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            MultiLineString mls = (MultiLineString) attr.getGeometry();
            LineString ls = (LineString) mls.getGeometryN(0);
            Coordinate[] coords = ls.getCoordinates();
            assertTrue(coords[0].x == -1.1);
            assertTrue(coords[0].y == -2.2);
            assertTrue(coords[1].x == -3.3);
            assertTrue(coords[1].y == -4.4);
            assertTrue(coords[2].x == -5.5);
            assertTrue(coords[2].y == -6.6);

            ls = (LineString) mls.getGeometryN(1);
            coords = ls.getCoordinates();
            assertTrue(coords[0].x == 1.1);
            assertTrue(coords[0].y == 2.2);
            assertTrue(coords[1].x == 3.3);
            assertTrue(coords[1].y == 4.4);
            assertTrue(coords[2].x == 5.5);
            assertTrue(coords[2].y == 6.6);

        }

    }

    public void testMultiSurface() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "MultiSurfacePolicy.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("MultiSurface"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            MultiPolygon mpoly = (MultiPolygon) attr.getGeometry();

            Polygon poly = (Polygon) mpoly.getGeometryN(0);
            Coordinate[] coords = poly.getExteriorRing().getCoordinates();
            assertTrue(coords[0].x == 0);
            assertTrue(coords[0].y == 0);
            assertTrue(coords[1].x == 0);
            assertTrue(coords[1].y == 100);
            assertTrue(coords[2].x == 100);
            assertTrue(coords[2].y == 100);
            assertTrue(coords[3].x == 100);
            assertTrue(coords[3].y == 0);
            assertTrue(coords[4].x == 0);
            assertTrue(coords[4].y == 0);

            poly = (Polygon) mpoly.getGeometryN(1);
            coords = poly.getExteriorRing().getCoordinates();
            assertTrue(coords[0].x == 0);
            assertTrue(coords[0].y == 0);
            assertTrue(coords[1].x == 0);
            assertTrue(coords[1].y == -100);
            assertTrue(coords[2].x == -100);
            assertTrue(coords[2].y == -100);
            assertTrue(coords[3].x == -100);
            assertTrue(coords[3].y == 0);
            assertTrue(coords[4].x == 0);
            assertTrue(coords[4].y == 0);
        }

    }

    public void testMultiSurface2() {

        GeometryAttribute[] array = new GeometryAttribute[2];
        AbstractPolicy p1 = TestSupport.policyFromFile(TestSupport.getGeoXACMLFNFor("gml3",
                "MultiSurfacePolicy2.xml"));
        array[0] = TestSupport.getGeometryAttribute(p1);
        AbstractPolicy p2 = policyFromAttributeEncoding(array[0], xmlTemplate);
        array[1] = TestSupport.getGeometryAttribute(p2);

        for (GeometryAttribute attr : array) {
            assertTrue(attr.getSrsName().equals("EPSG:4326"));
            assertTrue(attr.getGid().equals("MultiSurface"));
            assertTrue(attr.getGmlVersion() == GMLVersion.Version3);

            MultiPolygon mpoly = (MultiPolygon) attr.getGeometry();

            Polygon poly = (Polygon) mpoly.getGeometryN(0);
            Coordinate[] coords = poly.getExteriorRing().getCoordinates();
            assertTrue(coords[0].x == 0);
            assertTrue(coords[0].y == 0);
            assertTrue(coords[1].x == 0);
            assertTrue(coords[1].y == 100);
            assertTrue(coords[2].x == 100);
            assertTrue(coords[2].y == 100);
            assertTrue(coords[3].x == 100);
            assertTrue(coords[3].y == 0);
            assertTrue(coords[4].x == 0);
            assertTrue(coords[4].y == 0);

            poly = (Polygon) mpoly.getGeometryN(1);
            coords = poly.getExteriorRing().getCoordinates();
            assertTrue(coords[0].x == 0);
            assertTrue(coords[0].y == 0);
            assertTrue(coords[1].x == 0);
            assertTrue(coords[1].y == -100);
            assertTrue(coords[2].x == -100);
            assertTrue(coords[2].y == -100);
            assertTrue(coords[3].x == -100);
            assertTrue(coords[3].y == 0);
            assertTrue(coords[4].x == 0);
            assertTrue(coords[4].y == 0);
        }

    }

}

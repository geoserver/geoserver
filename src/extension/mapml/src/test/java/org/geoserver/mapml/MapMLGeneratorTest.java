/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.mapml.xml.GeometryContent;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class MapMLGeneratorTest extends GeoServerTestSupport {
    Jaxb2Marshaller mapmlMarshaller;

    @Before
    public void setupMarshaller() {
        mapmlMarshaller = (Jaxb2Marshaller) applicationContext.getBean("mapmlMarshaller");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMapMLMultiPointFromJTSMultiPointGenerator() throws Exception {

        GeometryFactory jtsf = new GeometryFactory();
        Coordinate[] ca1 = {new Coordinate(-75.705338D, 45.397785D)};
        Coordinate[] ca2 = {new Coordinate(-75.702082D, 45.397847D)};
        Point[] points = {
            new Point(new CoordinateArraySequence(ca1), jtsf),
            new Point(new CoordinateArraySequence(ca2), jtsf)
        };
        MapMLGenerator featureBuilder = new MapMLGenerator();
        org.locationtech.jts.geom.MultiPoint jtsMultiPoint =
                new org.locationtech.jts.geom.MultiPoint(points, jtsf);
        JAXBElement<org.geoserver.mapml.xml.MultiPoint> mp = null;
        try {
            GeometryContent g = featureBuilder.buildGeometry(jtsMultiPoint);

            mp = (JAXBElement<org.geoserver.mapml.xml.MultiPoint>) g.getGeometryContent();
        } catch (Exception e) {
            fail("org.geoserver.mapml.xml.MultiPoint should be returned by JAXB");
        }

        StringWriter sw = new StringWriter();
        try {
            mapmlMarshaller.marshal(mp, new StreamResult(sw));
        } catch (DataBindingException ex) {
            fail("DataBindingException while reading MapML JAXB MultiPoint object");
        }
        assertTrue(
                "Coordinates of 6 digit precision (default numDecimals) should be returned",
                sw.toString()
                        .contains(
                                "<map-multipoint xmlns=\"http://www.w3.org/1999/xhtml\"><map-coordinates>-75.705338 45.397785 -75.702082 45.397847</map-coordinates>"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNumDecimals() throws Exception {

        GeometryFactory jtsf = new GeometryFactory();
        Coordinate[] ca1 = {new Coordinate(-75.705338D, 45.397785D)};
        Coordinate[] ca2 = {new Coordinate(-75.702082D, 45.397847D)};
        Point[] points = {
            new Point(new CoordinateArraySequence(ca1), jtsf),
            new Point(new CoordinateArraySequence(ca2), jtsf)
        };
        MapMLGenerator featureBuilder = new MapMLGenerator();
        featureBuilder.setNumDecimals(5);
        org.locationtech.jts.geom.MultiPoint jtsMultiPoint =
                new org.locationtech.jts.geom.MultiPoint(points, jtsf);
        JAXBElement<org.geoserver.mapml.xml.MultiPoint> mp = null;
        try {
            GeometryContent g = featureBuilder.buildGeometry(jtsMultiPoint);

            mp = (JAXBElement<org.geoserver.mapml.xml.MultiPoint>) g.getGeometryContent();
        } catch (Exception e) {
            fail("org.geoserver.mapml.xml.MultiPoint should be returned by JAXB");
        }

        StringWriter sw = new StringWriter();
        try {
            mapmlMarshaller.marshal(mp, new StreamResult(sw));
        } catch (DataBindingException ex) {
            fail("DataBindingException while reading MapML JAXB MultiPoint object");
        }
        assertTrue(
                "Coordinates should be rounded to 5 decimal places (non-default numDecimals)",
                sw.toString()
                        .contains(
                                "<map-multipoint xmlns=\"http://www.w3.org/1999/xhtml\"><map-coordinates>-75.70534 45.39779 -75.70208 45.39785</map-coordinates>"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPadWithZeros() throws Exception {

        GeometryFactory jtsf = new GeometryFactory();
        Coordinate[] ca1 = {new Coordinate(-75.705338D, 45.3977853D)};
        Coordinate[] ca2 = {new Coordinate(-75.702082D, 45.3978472D)};
        Point[] points = {
            new Point(new CoordinateArraySequence(ca1), jtsf),
            new Point(new CoordinateArraySequence(ca2), jtsf)
        };
        MapMLGenerator featureBuilder = new MapMLGenerator();
        featureBuilder.setPadWithZeros(true);
        featureBuilder.setNumDecimals(10);
        org.locationtech.jts.geom.MultiPoint jtsMultiPoint =
                new org.locationtech.jts.geom.MultiPoint(points, jtsf);
        JAXBElement<org.geoserver.mapml.xml.MultiPoint> mp = null;
        try {
            GeometryContent g = featureBuilder.buildGeometry(jtsMultiPoint);

            mp = (JAXBElement<org.geoserver.mapml.xml.MultiPoint>) g.getGeometryContent();
        } catch (Exception e) {
            fail("org.geoserver.mapml.xml.MultiPoint should be returned by JAXB");
        }

        StringWriter sw = new StringWriter();
        try {
            mapmlMarshaller.marshal(mp, new StreamResult(sw));
        } catch (DataBindingException ex) {
            fail("DataBindingException while reading MapML JAXB MultiPoint object");
        }
        assertTrue(
                "Coordinates should be rounded to 5 decimal places (non-default numDecimals)",
                sw.toString()
                        .contains(
                                "<map-multipoint xmlns=\"http://www.w3.org/1999/xhtml\"><map-coordinates>-75.7053380000 45.3977853000 -75.7020820000 45.3978472000</map-coordinates>"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForcedDecimal() throws Exception {

        GeometryFactory jtsf = new GeometryFactory();
        Coordinate[] ca1 = {new Coordinate(-1000000000.1D, 1000000000.2D)};
        Coordinate[] ca2 = {new Coordinate(-1000000001.3D, 1000000001.4D)};
        Point[] points = {
            new Point(new CoordinateArraySequence(ca1), jtsf),
            new Point(new CoordinateArraySequence(ca2), jtsf)
        };
        MapMLGenerator featureBuilder = new MapMLGenerator();
        // when encoded in scientific notation, these parameters are ignored
        featureBuilder.setNumDecimals(3);
        featureBuilder.setPadWithZeros(true);
        org.locationtech.jts.geom.MultiPoint jtsMultiPoint =
                new org.locationtech.jts.geom.MultiPoint(points, jtsf);
        JAXBElement<org.geoserver.mapml.xml.MultiPoint> mp = null;
        try {
            GeometryContent g = featureBuilder.buildGeometry(jtsMultiPoint);

            mp = (JAXBElement<org.geoserver.mapml.xml.MultiPoint>) g.getGeometryContent();
        } catch (Exception e) {
            fail("org.geoserver.mapml.xml.MultiPoint should be returned by JAXB");
        }

        StringWriter sw = new StringWriter();
        try {
            mapmlMarshaller.marshal(mp, new StreamResult(sw));
        } catch (DataBindingException ex) {
            fail("DataBindingException while reading MapML JAXB MultiPoint object");
        }
        assertTrue(
                "Coordinates should encoded in scientific notation",
                sw.toString()
                        .contains(
                                "<map-multipoint xmlns=\"http://www.w3.org/1999/xhtml\"><map-coordinates>-1.0000000001E9 1.0000000002E9 -1.0000000013E9 1.0000000014E9</map-coordinates>"));

        // when NOT encoded in scientific notation, these parameters are used
        featureBuilder.setForcedDecimal(true);
        featureBuilder.setPadWithZeros(true);
        jtsMultiPoint = new org.locationtech.jts.geom.MultiPoint(points, jtsf);
        mp = null;
        try {
            GeometryContent g = featureBuilder.buildGeometry(jtsMultiPoint);

            mp = (JAXBElement<org.geoserver.mapml.xml.MultiPoint>) g.getGeometryContent();
        } catch (Exception e) {
            fail("org.geoserver.mapml.xml.MultiPoint should be returned by JAXB");
        }

        sw = new StringWriter();
        try {
            mapmlMarshaller.marshal(mp, new StreamResult(sw));
        } catch (DataBindingException ex) {
            fail("DataBindingException while reading MapML JAXB MultiPoint object");
        }
        assertTrue(
                "Coordinates should encoded in decimal notation",
                sw.toString()
                        .contains(
                                "<map-multipoint xmlns=\"http://www.w3.org/1999/xhtml\"><map-coordinates>-1000000000.100 1000000000.200 -1000000001.300 1000000001.400</map-coordinates>"));
    }
}

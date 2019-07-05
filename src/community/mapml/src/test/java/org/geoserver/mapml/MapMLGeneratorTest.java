/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import org.geoserver.mapml.xml.GeometryContent;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

public class MapMLGeneratorTest {

    @Test
    public void testMapMLMultiPointFromJTSMultiPointGenerator() throws Exception {

        GeometryFactory jtsf = new GeometryFactory();
        Coordinate[] ca1 = {new Coordinate(-75.705338D, 45.397785D)};
        Coordinate[] ca2 = {new Coordinate(-75.702082D, 45.397847D)};
        Point[] points = {
            new Point(new CoordinateArraySequence(ca1), jtsf),
            new Point(new CoordinateArraySequence(ca2), jtsf)
        };

        org.locationtech.jts.geom.MultiPoint jtsMultiPoint =
                new org.locationtech.jts.geom.MultiPoint(points, jtsf);
        JAXBElement<org.geoserver.mapml.xml.MultiPoint> mp = null;
        try {
            GeometryContent g = MapMLGenerator.buildGeometry(jtsMultiPoint);
            mp = (JAXBElement<org.geoserver.mapml.xml.MultiPoint>) g.getGeometryContent();
        } catch (Exception e) {
            fail("org.geoserver.mapml.xml.MultiPoint should be returned by JAXB");
        }

        StringWriter sw = new StringWriter();
        try {
            JAXB.marshal(mp, sw);
        } catch (DataBindingException ex) {
            fail("DataBindingException while reading MapML JAXB MultiPoint object");
        }
        assertTrue(
                sw.toString()
                        .contains(
                                "<multipoint>\r\n   "
                                        + "<coordinates>-75.705338 45.397785 -75.702082 45.397847</coordinates>"));
    }
}

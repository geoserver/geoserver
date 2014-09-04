/*
 * Copyright (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;

public class GeoJSONBuilderTest {

    StringWriter writer;

    GeoJSONBuilder builder;

    @Before
    public void setUp() {
        writer = new StringWriter();
        builder = new GeoJSONBuilder(writer);
    }

    @Test
    public void testWriteNormal() throws Exception {
        Geometry g = new WKTReader().read("MULTILINESTRING((0 0, 1 1))");
        builder.writeGeom(g);

        assertEquals("{\"type\":\"MultiLineString\",\"coordinates\":[[[0,0],[1,1]]]}",
                writer.toString());
    }

    @Test
    public void testWriteGeometrySubclass() throws Exception {
        builder.writeGeom(new MyPoint(1, 2));

        assertEquals("{\"type\":\"Point\",\"coordinates\":[1,2]}", writer.toString());
    }

    class MyPoint extends Point {

        public MyPoint(double x, double y) {
            super(new Coordinate(x, y), new PrecisionModel(), -1);
        }
    }

    @Test
    public void testWriteDate() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);

        java.sql.Date date = new java.sql.Date(cal.getTimeInMillis());
        builder.object().key("date").value(date).endObject();
        assertEquals("{\"date\":\"2011-10-25Z\"}", writer.toString());
    }

    @Test
    public void testWriteTime() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.HOUR, 15);
        cal.set(Calendar.MINUTE, 48);
        cal.set(Calendar.SECOND, 5);

        java.sql.Time date = new java.sql.Time(cal.getTimeInMillis());
        builder.object().key("time").value(date).endObject();
        assertEquals("{\"time\":\"15:48:05Z\"}", writer.toString());
    }

    @Test
    public void testWriteDateTime() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);
        cal.set(Calendar.HOUR, 15);
        cal.set(Calendar.MINUTE, 48);
        cal.set(Calendar.SECOND, 5);

        java.sql.Timestamp date = new java.sql.Timestamp(cal.getTimeInMillis());
        builder.object().key("timestamp").value(date).endObject();
        assertEquals("{\"timestamp\":\"2011-10-25T15:48:05Z\"}", writer.toString());
    }

    @Test
    public void testWriteDateTimeMillis() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);
        cal.set(Calendar.HOUR, 15);
        cal.set(Calendar.MINUTE, 48);
        cal.set(Calendar.SECOND, 5);
        cal.set(Calendar.MILLISECOND, 223);

        java.sql.Timestamp date = new java.sql.Timestamp(cal.getTimeInMillis());
        builder.object().key("timestamp").value(date).endObject();
        assertEquals("{\"timestamp\":\"2011-10-25T15:48:05.223Z\"}", writer.toString());
    }

    @Test
    public void testWriteJavaDate() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);
        cal.set(Calendar.HOUR, 15);
        cal.set(Calendar.MINUTE, 48);
        cal.set(Calendar.SECOND, 5);
        cal.set(Calendar.MILLISECOND, 223);

        java.util.Date date = new java.util.Date(cal.getTimeInMillis());
        builder.object().key("date").value(date).endObject();
        assertEquals("{\"date\":\"2011-10-25T15:48:05.223Z\"}", writer.toString());
    }

    @Test
    public void testWriteCalendar() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);

        builder.object().key("cal").value(cal).endObject();
        assertEquals("{\"cal\":\"2011-10-25T00:00:00Z\"}", writer.toString());
    }

    @Test
    public void testWriteCalendarTZ() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-05:00"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);

        builder.object().key("cal").value(cal).endObject();
        assertEquals("{\"cal\":\"2011-10-25T00:00:00-05:00\"}", writer.toString());
    }

    @Test
    public void testWriteCalendarFull() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);
        cal.set(Calendar.HOUR, 15);
        cal.set(Calendar.MINUTE, 48);
        cal.set(Calendar.SECOND, 5);
        cal.set(Calendar.MILLISECOND, 223);

        builder.object().key("cal").value(cal).endObject();
        assertEquals("{\"cal\":\"2011-10-25T15:48:05.223Z\"}", writer.toString());
    }

    @Test
    public void testWriteGeomCollection() throws Exception {
        Geometry g = new WKTReader().read("GEOMETRYCOLLECTION(POINT(2 0),POINT(7 1))");
        builder.writeGeom(g);
        assertEquals("{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"Point\",\"coordinates\":[2,0]},{\"type\":\"Point\",\"coordinates\":[7,1]}]}",
                writer.toString());
    }
    
    @Test
    public void testWrite3DPoint() throws Exception {
        Geometry g = new WKTReader().read("POINT(2 0 20)");
        builder.writeGeom(g);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[2,0,20]}", writer.toString());
    }
    
    @Test
    public void testWrite3DLine() throws Exception {
        Geometry g = new WKTReader().read("LINESTRING(0 0 0, 0 10 1, 10 10 2, 10 0 3, 0 0 0)");
        builder.writeGeom(g);
        assertEquals("{\"type\":\"LineString\",\"coordinates\":[[0,0,0],[0,10,1],[10,10,2],[10,0,3],[0,0,0]]}", writer.toString());
    }
    
    @Test
    public void testWrite3DPolygon() throws Exception {
        Geometry g = new WKTReader().read("POLYGON((0 0 0, 0 10 1, 10 10 2, 10 0 3, 0 0 0),(1 1 4, 1 2 5, 2 2 6, 2 1 7, 1 1 4))");
        builder.writeGeom(g);
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[0,0,0],[0,10,1],[10,10,2],[10,0,3],[0,0,0]],[[1,1,4],[1,2,5],[2,2,6],[2,1,7],[1,1,4]]]}", writer.toString());
    }
}

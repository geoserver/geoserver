/*
 * Copyright (c) 2001 - 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.TimeZone;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;

import junit.framework.TestCase;

public class GeoJSONBuilderTest extends TestCase {

    StringWriter writer;
    GeoJSONBuilder builder;
    
    @Override
    protected void setUp() throws Exception {
        writer = new StringWriter();
        builder = new GeoJSONBuilder(writer);
    }
    
    public void testWriteNormal() throws Exception {
        Geometry g = new WKTReader().read("MULTILINESTRING((0 0, 1 1))");
        builder.writeGeom(g);
        
        assertEquals("{\"type\":\"MultiLineString\",\"coordinates\":[[[0,0],[1,1]]]}",
            writer.toString());
    }
    
    public void testWriteGeometrySubclass() throws Exception {
        builder.writeGeom(new MyPoint(1,2));
        
        assertEquals("{\"type\":\"Point\",\"coordinates\":[1,2]}", writer.toString());
    }
    
    class MyPoint extends Point {
        
        public MyPoint(double x, double y) {
            super(new Coordinate(x, y), new PrecisionModel(), -1);
        }
    }
    
    public void testWriteDate() throws Exception{
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);
        
        java.sql.Date date = new java.sql.Date(cal.getTimeInMillis());
        builder.object().key("date").value(date).endObject();
        assertEquals("{\"date\":\"2011-10-25Z\"}", writer.toString());
    }

    public void testWriteTime() throws Exception{
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.HOUR, 15);
        cal.set(Calendar.MINUTE, 48);
        cal.set(Calendar.SECOND, 5);
        
        java.sql.Time date = new java.sql.Time(cal.getTimeInMillis());
        builder.object().key("time").value(date).endObject();
        assertEquals("{\"time\":\"15:48:05Z\"}", writer.toString());
    }

    public void testWriteDateTime() throws Exception{
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

    public void testWriteDateTimeMillis() throws Exception{
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

    public void testWriteJavaDate() throws Exception{
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

    public void testWriteCalendar() throws Exception{
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);
        
        builder.object().key("cal").value(cal).endObject();
        assertEquals("{\"cal\":\"2011-10-25T00:00:00Z\"}", writer.toString());
    }

    public void testWriteCalendarTZ() throws Exception{
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-05:00"));
        cal.clear();
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, 9);
        cal.set(Calendar.DAY_OF_MONTH, 25);
        
        builder.object().key("cal").value(cal).endObject();
        assertEquals("{\"cal\":\"2011-10-25T00:00:00-05:00\"}", writer.toString());
    }

    public void testWriteCalendarFull() throws Exception{
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
}

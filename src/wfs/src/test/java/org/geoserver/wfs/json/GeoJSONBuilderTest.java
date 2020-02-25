/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.io.WKTReader;

public class GeoJSONBuilderTest {

    StringWriter writer;

    GeoJSONBuilder builder;

    @Before
    public void setUp() {
        System.clearProperty("json.maxDepth");
        writer = new StringWriter();
        builder = new GeoJSONBuilder(writer);
        builder.setEncodeMeasures(true);
    }

    @Test
    public void testWriteNormal() throws Exception {
        Geometry g = new WKTReader().read("MULTILINESTRING((0 0, 1 1))");
        builder.writeGeom(g);

        assertEquals(
                "{\"type\":\"MultiLineString\",\"coordinates\":[[[0,0],[1,1]]]}",
                writer.toString());
    }

    @Test
    public void testWriteGeometrySubclass() throws Exception {
        builder.writeGeom(new MyPoint(1, 2));

        assertEquals("{\"type\":\"Point\",\"coordinates\":[1,2]}", writer.toString());
    }

    class MyPoint extends Point {

        public MyPoint(double x, double y) {
            super(
                    new CoordinateArraySequence(new Coordinate[] {new Coordinate(x, y)}),
                    new GeometryFactory());
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
        assertEquals(
                "{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"Point\",\"coordinates\":[2,0]},{\"type\":\"Point\",\"coordinates\":[7,1]}]}",
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
        assertEquals(
                "{\"type\":\"LineString\",\"coordinates\":[[0,0,0],[0,10,1],[10,10,2],[10,0,3],[0,0,0]]}",
                writer.toString());
    }

    @Test
    public void testWrite3DPolygon() throws Exception {
        Geometry g =
                new WKTReader()
                        .read(
                                "POLYGON((0 0 0, 0 10 1, 10 10 2, 10 0 3, 0 0 0),(1 1 4, 1 2 5, 2 2 6, 2 1 7, 1 1 4))");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"Polygon\",\"coordinates\":[[[0,0,0],[0,10,1],[10,10,2],[10,0,3],[0,0,0]],[[1,1,4],[1,2,5],[2,2,6],[2,1,7],[1,1,4]]]}",
                writer.toString());
    }

    @Test
    public void testNumberOfDecimalsFor3dPoint() throws Exception {
        builder.setNumberOfDecimals(2);
        Geometry g = new WKTReader().read("POINT(2.1234 0.1234 20.9999)");
        builder.writeGeom(g);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[2.12,0.12,21]}", writer.toString());
    }

    @Test
    public void testNumberOfDecimalsFor3dLine() throws Exception {
        builder.setNumberOfDecimals(3);
        Geometry g =
                new WKTReader()
                        .read(
                                "LINESTRING(1E-3 1E-4 1E-5, 0 10.12312321 1.000002, 10.1 10.2 2.0, 10 0 3, 0 0 0)");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"LineString\",\"coordinates\":[[0.001,0,0],[0,10.123,1],[10.1,10.2,2],[10,0,3],[0,0,0]]}",
                writer.toString());
    }

    @Test
    public void testNumberOfDecimalsFor3dPolygon() throws Exception {
        builder.setNumberOfDecimals(0);
        Geometry g =
                new WKTReader()
                        .read(
                                "POLYGON((0.1 0.2 0.3, 0.1 10.1 1.1, 10.2 10.3 2.4, 9.5 0.4 3, 0.1 0.2 0.3),(1 1 4, 1 2 5, 2 2 6, 2 1 7, 1 1 4))");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"Polygon\",\"coordinates\":[[[0,0,0],[0,10,1],[10,10,2],[10,0,3],[0,0,0]],[[1,1,4],[1,2,5],[2,2,6],[2,1,7],[1,1,4]]]}",
                writer.toString());
    }

    @Test
    public void testWriteStrList() throws Exception {
        final List<String> list = Arrays.asList("a", "b", "c", "d");
        builder.writeList(list);
        assertEquals("[\"a\",\"b\",\"c\",\"d\"]", writer.toString());
    }

    @Test
    public void testWriteIntList() throws Exception {
        final List<Integer> list = Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE, 3, 4);
        builder.writeList(list);
        assertEquals(
                "["
                        + Integer.toString(Integer.MAX_VALUE)
                        + ","
                        + Integer.toString(Integer.MIN_VALUE)
                        + ",3,4]",
                writer.toString());
    }

    @Test
    public void testWriteLongList() throws Exception {
        final List<Long> list = Arrays.asList(Long.MAX_VALUE, Long.MIN_VALUE, 0L, -333L, 4L);
        builder.writeList(list);
        assertEquals(
                "["
                        + Long.toString(Long.MAX_VALUE)
                        + ","
                        + Long.toString(Long.MIN_VALUE)
                        + ",0,-333,4]",
                writer.toString());
    }

    @Test
    public void testWriteFloatList() throws Exception {
        final List<Float> list =
                Arrays.asList(Float.MAX_VALUE, Float.MIN_VALUE, 0f, -333.2365f, 0.23235656f);
        builder.writeList(list);
        assertEquals(
                "["
                        + Float.toString(Float.MAX_VALUE)
                        + ","
                        + Float.toString(Float.MIN_VALUE)
                        + ",0,-333.2365,0.23235656]",
                writer.toString());
    }

    @Test
    public void testWriteDoubleList() throws Exception {
        final List<Double> list =
                Arrays.asList(Double.MAX_VALUE, Double.MIN_VALUE, 0d, -333.2365d, 0.23235656d);
        builder.writeList(list);
        assertEquals(
                "["
                        + Double.toString(Double.MAX_VALUE)
                        + ","
                        + Double.toString(Double.MIN_VALUE)
                        + ",0,-333.2365,0.23235656]",
                writer.toString());
    }

    @Test
    public void testWriteUUIDList() throws Exception {
        final UUID u1 = UUID.fromString("12345678-1234-1234-1234-123456781234");
        final UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final List<UUID> list = Arrays.asList(u1, u2);
        builder.writeList(list);
        assertEquals(
                "[" + "\"" + u1.toString() + "\"" + "," + "\"" + u2.toString() + "\"" + "]",
                writer.toString());
    }

    @Test
    public void testWriteStringStringMap() throws Exception {
        final Map<String, String> map =
                new HashMap<String, String>() {
                    {
                        put("a", "1");
                        put("b", "2");
                        put("c", "3");
                    }
                };
        builder.writeMap(map);
        final JSONObject root = JSONObject.fromObject(writer.toString());
        assertEquals(3, root.size());
        assertEquals("1", root.get("a"));
        assertEquals("2", root.get("b"));
        assertEquals("3", root.get("c"));
    }

    @Test
    public void testWriteStringIntMap() throws Exception {
        final Map<String, Integer> map =
                new HashMap<String, Integer>() {
                    {
                        put("a", Integer.MAX_VALUE);
                        put("b", Integer.MIN_VALUE);
                        put("c", 3);
                    }
                };
        builder.writeMap(map);
        final JSONObject root = JSONObject.fromObject(writer.toString());
        assertEquals(3, root.size());
        assertEquals(Integer.MAX_VALUE, root.get("a"));
        assertEquals(Integer.MIN_VALUE, root.get("b"));
        assertEquals(3, root.get("c"));
    }

    @Test
    public void testWriteListOfMaps() throws Exception {
        final UUID u1 = UUID.fromString("12345678-1234-1234-1234-123456781234");
        final Map<String, Object> tuple1 =
                new HashMap<String, Object>() {
                    {
                        put("a", 1);
                        put("b", u1);
                        put("c", "object1");
                    }
                };

        final UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final Map<String, Object> tuple2 =
                new HashMap<String, Object>() {
                    {
                        put("a", 2);
                        put("b", u2);
                        put("c", "object2");
                    }
                };

        final List<Map<String, Object>> tupleList = Arrays.asList(tuple1, tuple2);
        builder.writeList(tupleList);

        final JSONArray root = JSONArray.fromObject(writer.toString());
        assertEquals(2, root.size());

        final JSONObject o1 = root.getJSONObject(0);
        assertEquals(3, o1.size());
        assertEquals(1, o1.get("a"));
        assertEquals(u1.toString(), o1.get("b"));
        assertEquals("object1", o1.get("c"));

        final JSONObject o2 = root.getJSONObject(1);
        assertEquals(3, o2.size());
        assertEquals(2, o2.get("a"));
        assertEquals(u2.toString(), o2.get("b"));
        assertEquals("object2", o2.get("c"));
    }

    @Test
    public void testWritePointZM() throws Exception {
        Geometry g = new WKTReader().read("POINT ZM (2 0 20 2)");
        builder.writeGeom(g);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[2,0,20,2]}", writer.toString());
    }

    @Test
    public void testWritePointM() throws Exception {
        Geometry g = new WKTReader().read("POINT M (2 0 20)");
        builder.writeGeom(g);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[2,0,0,20]}", writer.toString());
    }

    @Test
    public void testWriteMultiPointZM() throws Exception {
        Geometry g = new WKTReader().read("MULTIPOINT ZM (2 0 20 2, 1 1 1 1)");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"MultiPoint\",\"coordinates\":[[2,0,20,2],[1,1,1,1]]}",
                writer.toString());
    }

    @Test
    public void testWriteMultiPointM() throws Exception {
        Geometry g = new WKTReader().read("MULTIPOINT M (2 0 20, 1 1 1)");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"MultiPoint\",\"coordinates\":[[2,0,0,20],[1,1,0,1]]}",
                writer.toString());
    }

    @Test
    public void testWriteLineZM() throws Exception {
        Geometry g = new WKTReader().read("LINESTRING ZM (0 0 0 0, 0 10 1 1, 10 10 2 2)");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"LineString\",\"coordinates\":[[0,0,0,0],[0,10,1,1],[10,10,2,2]]}",
                writer.toString());
    }

    @Test
    public void testWriteMultiLineZM() throws Exception {
        Geometry g = new WKTReader().read("MULTILINESTRING ZM ((1 2 3 4,5 6 7 8))");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"MultiLineString\",\"coordinates\":[[[1,2,3,4],[5,6,7,8]]]}",
                writer.toString());
    }

    @Test
    public void testWriteMultiLineM() throws Exception {
        Geometry g = new WKTReader().read("MULTILINESTRING M ((1 2 4,5 6 8))");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"MultiLineString\",\"coordinates\":[[[1,2,0,4],[5,6,0,8]]]}",
                writer.toString());
    }

    @Test
    public void testWritePolygonZM() throws Exception {
        Geometry g =
                new WKTReader()
                        .read(
                                "POLYGON ZM "
                                        + "((0 0 0 3, 0 10 1 3, 10 10 2 3, 10 0 3 3, 0 0 0 3),"
                                        + "(1 1 4 3, 1 2 5 3, 2 2 6 3, 2 1 7 3, 1 1 4 3))");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"Polygon\",\"coordinates\":[[[0,0,0,3],[0,10,1,3],[10,10,2,3],[10,0,3,3],[0,0,0,3]]"
                        + ",[[1,1,4,3],[1,2,5,3],[2,2,6,3],[2,1,7,3],[1,1,4,3]]]}",
                writer.toString());
    }

    @Test
    public void testWriteMultiPolygonZM() throws Exception {
        Geometry g = new WKTReader().read("MULTIPOLYGON ZM (((0 0 3 1,1 1 7 2,1 0 7 3,0 0 3 1)))");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[0,0,3,1],[1,1,7,2],[1,0,7,3],[0,0,3,1]]]]}",
                writer.toString());
    }

    @Test
    public void testWriteMultiPolygonM() throws Exception {
        Geometry g = new WKTReader().read("MULTIPOLYGON M (((0 0 1,1 1 2,1 0 3,0 0 1)))");
        builder.writeGeom(g);
        assertEquals(
                "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[0,0,0,1],[1,1,0,2],[1,0,0,3],[0,0,0,1]]]]}",
                writer.toString());
    }

    /** Checks max json nested level should allow up to 100 by default. */
    @Test
    public void testMaxNestedLevel() {
        builder.object();
        addLevels(builder, 0, 99);
        builder.endObject();
    }

    /** Checks max json nested level should allow up to 120 via system property. */
    @Test
    public void testMaxNestedLevelSystemParameter() {
        try {
            System.setProperty("json.maxDepth", "120");
            final StringWriter writer = new StringWriter();
            final GeoJSONBuilder builder = new GeoJSONBuilder(writer);
            builder.object();
            addLevels(builder, 0, 119);
            builder.endObject();
        } finally {
            System.clearProperty("json.maxDepth");
        }
    }

    private void addLevels(final GeoJSONBuilder builder, int level, final int max) {
        if (level >= max) return;
        level++;
        builder.key("inner").object();
        addLevels(builder, level, max);
        builder.endObject();
    }
}

/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.logging.Logger;
import org.geoserver.mapml.TaggedPolygon.TaggedCoordinateSequence;
import org.geoserver.mapml.TaggedPolygon.TaggedLineString;
import org.geotools.util.logging.Logging;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * Checks the polygon tagger is working as expected. For the moment it's not testing the coordinate
 * values as there is no written spec of how they should appear, interactive tests have so far
 * proven inconclusive.
 */
public class MapMLGeometryClipperTest {

    static final Boolean QUIET_TESTS = Boolean.getBoolean("quietTests");

    static final Logger LOGGER = Logging.getLogger(MapMLGeometryClipperTest.class);

    private WKTReader wktReader = new WKTReader();

    private Polygon world;

    private Polygon donut;

    private Polygon w;

    @Before
    public void setupTestGeometries() throws Exception {
        world = getPolygon("POLYGON((-180 -90, -180 90, 180 90, 180 -90, -180 -90))");
        donut =
                getPolygon(
                        "POLYGON((-180 -90, -180 90, 180 90, 180 -90, -180 -90), "
                                + "(-170 -80, -170 80, 170 80, 170 -80, -170 -80))");
        w = getPolygon("POLYGON((-10 0, 0 10, 10 0, 20 20, -20 20, -10 0))");
    }

    private Polygon getPolygon(String wkt) throws ParseException {
        return (Polygon) wktReader.read(wkt);
    }

    @Test
    public void testWorldHalfWest() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(-180, 0, -90, 90), world);

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // the invisible segment is fully inside the invisible CS
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(3, coordinates.size());
        assertCoordinates(coordinates.get(0), true, -180, -90, 0, -90);
        assertCoordinates(coordinates.get(1), false, 0, -90, 0, 90);
        assertCoordinates(coordinates.get(2), true, 0, 90, -180, 90, -180, -90);
    }

    @Test
    public void testWorldHalfEast() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(0, 180, -90, 90), world);

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // the invisible segment is fully inside the invisible CS
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(2, coordinates.size());
        assertCoordinates(coordinates.get(0), true, 0, -90, 180, -90, 180, 90, 0, 90);
        assertCoordinates(coordinates.get(1), false, 0, 90, 0, -90);
    }

    @Test
    public void testWorldAllInvisible() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(-45, 45, -45, 45), world);

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // everything is invisible
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(1, coordinates.size());
        assertCoordinates(coordinates.get(0), false, -45, -45, 45, -45, 45, 45, -45, 45, -45, -45);
    }

    @Test
    public void testWorldTopLeftCorner() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(-180, 0, 0, 90), world);

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // two and two
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(2, coordinates.size());
        assertCoordinates(coordinates.get(0), false, -180, 0, 0, 0, 0, 90);
        assertCoordinates(coordinates.get(1), true, 0, 90, -180, 90, -180, 0);
    }

    @Test
    public void testClipDonutFullSide() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(-190, -160, 0, 10), donut);

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // one out, one in, one out, one in
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(4, coordinates.size());
        assertCoordinates(coordinates.get(0), false, -180, 0, -170, 0);
        assertCoordinates(coordinates.get(1), true, -170, 0, -170, 10);
        assertCoordinates(coordinates.get(2), false, -170, 10, -180, 10);
        assertCoordinates(coordinates.get(3), true, -180, 10, -180, 0);
    }

    @Test
    public void testClipDonutPartialSide() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(-175, -160, 0, 10), donut);

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // one out, one in, two out
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(3, coordinates.size());
        assertCoordinates(coordinates.get(0), false, -175, 0, -170, 0);
        assertCoordinates(coordinates.get(1), true, -170, 0, -170, 10);
        assertCoordinates(coordinates.get(2), false, -170, 10, -175, 10, -175, 0);
    }

    @Test
    public void testClipDonutCorner() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(-180, -170, 80, 90), donut);

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // one out, one in, two out
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(2, coordinates.size());
        assertCoordinates(coordinates.get(0), false, -180, 80, -170, 80, -170, 90);
        assertCoordinates(coordinates.get(1), true, -170, 90, -180, 90, -180, 80);
    }

    /** Cutting a polygon and getting two as a result */
    @Test
    public void testWMiddle() throws Exception {
        Envelope clipEnvelope = new Envelope(-20, 20, 0, 10);
        MapMLGeometryClipper tagger = new MapMLGeometryClipper(w, clipEnvelope);
        Geometry geometry = tagger.clipAndTag();
        assertThat(geometry, Matchers.instanceOf(MultiPolygon.class));
        MultiPolygon mp = (MultiPolygon) geometry;
        assertEquals(2, mp.getNumGeometries());

        Polygon p1 = (Polygon) mp.getGeometryN(0);
        TaggedPolygon tp1 = (TaggedPolygon) p1.getUserData();
        if (!QUIET_TESTS) {
            LOGGER.info("Tagged polygon 1: " + tp1);
        }

        // a triangle with no holes
        assertEquals(0, tp1.getHoles().size());
        TaggedLineString ls1 = tp1.getBoundary();
        List<TaggedCoordinateSequence> cs1 = ls1.getCoordinates();
        assertEquals(2, cs1.size());
        assertCoordinates(cs1.get(0), true, -15, 10, -10, 0, 0, 10);
        assertCoordinates(cs1.get(1), false, 0, 10, -15, 10);

        Polygon p2 = (Polygon) mp.getGeometryN(1);
        TaggedPolygon tp2 = (TaggedPolygon) p2.getUserData();
        if (!QUIET_TESTS) {
            LOGGER.info("Tagged polygon 2: " + tp2);
        }

        // a triangle with no holes
        assertEquals(0, tp2.getHoles().size());
        TaggedLineString ls2 = tp2.getBoundary();
        List<TaggedCoordinateSequence> cs2 = ls2.getCoordinates();
        assertEquals(2, cs2.size());
        assertCoordinates(cs2.get(0), true, 0, 10, 10, 0, 15, 10);
        assertCoordinates(cs2.get(1), false, 15, 10, 0, 10);
    }

    protected void assertCoordinates(
            TaggedCoordinateSequence cs, boolean visible, double... expected) {
        assertEquals(visible, cs.isVisible());
        List<Coordinate> coordinates = cs.getCoordinates();
        assertEquals(expected.length / 2, coordinates.size());
        for (int i = 0; i < expected.length; i += 2) {
            assertEquals(expected[i], coordinates.get(i / 2).getX(), 1e-6);
            assertEquals(expected[i + 1], coordinates.get(i / 2).getY(), 1e-6);
        }
    }

    private TaggedPolygon getTaggedPolygon(Envelope clipEnvelope, Polygon polygon) {
        MapMLGeometryClipper tagger = new MapMLGeometryClipper(polygon, clipEnvelope);
        Geometry geometry = tagger.clipAndTag();
        if (!(geometry.getUserData() instanceof TaggedPolygon)) return null;
        TaggedPolygon tagged = (TaggedPolygon) geometry.getUserData();
        if (!QUIET_TESTS) {
            LOGGER.info("Tagged polygon: " + tagged);
        }
        return tagged;
    }

    @Test
    public void testClipMultiLineString() throws Exception {
        MultiLineString g =
                (MultiLineString)
                        wktReader.read("MULTILINESTRING((0 0, 15 15), (20 20, 30 30), (5 5, 7 5))");
        MapMLGeometryClipper tagger = new MapMLGeometryClipper(g, new Envelope(5, 15, -5, 15));
        Geometry geometry = tagger.clipAndTag();
        assertEquals(wktReader.read("MULTILINESTRING ((5 5, 15 15), (5 5, 7 5))"), geometry);
    }

    @Test
    public void testClipMultiPoint() throws Exception {
        MultiPoint g =
                (MultiPoint) wktReader.read("MULTIPOINT((0 0), (0 10), (10 10), (10 0), (0 0))");
        MapMLGeometryClipper tagger = new MapMLGeometryClipper(g, new Envelope(5, 15, -5, 15));
        Geometry geometry = tagger.clipAndTag();
        assertEquals(wktReader.read("MULTIPOINT((10 10), (10 0))"), geometry);
    }

    @Test
    public void testClipMultiPolygon() throws Exception {
        MultiPolygon g =
                (MultiPolygon)
                        wktReader.read(
                                "MULTIPOLYGON(((0 0, 0 20, 20 20, 20 0, 0 0)), ((5 5, 5 7, 7 7, 7 5, 5 5)),  ((25 25, 25 27, 27 27, 27 25, 25 25)))");
        MapMLGeometryClipper tagger = new MapMLGeometryClipper(g, new Envelope(5, 15, -5, 15));
        Geometry geometry = tagger.clipAndTag();
        assertEquals(
                wktReader.read(
                        "MULTIPOLYGON (((5 0, 5 15, 15 15, 15 0, 5 0)), ((5 5, 5 7, 7 7, 7 5, 5 5)))"),
                geometry);
    }
}

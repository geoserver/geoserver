/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class ClipIntersectsFeatureCollectionTest {

    DefaultFeatureCollection delegateMultiPolygon;

    DefaultFeatureCollection delegateMultiLines;

    @Before
    public void setup() throws ParseException {
        WKTReader reader = new WKTReader();

        // polygon
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("MultiPoly");
        tb.add("geom", MultiPolygon.class);
        tb.add("name", String.class);
        SimpleFeatureType featureType = tb.buildFeatureType();
        delegateMultiPolygon = new DefaultFeatureCollection(null, featureType);
        SimpleFeatureBuilder bPoly = new SimpleFeatureBuilder(featureType);
        Geometry geom = reader.read("MULTIPOLYGON (((-1 0, 0 1, 1 0, 0 -1, -1 0)))");
        bPoly.add(geom);
        bPoly.add("one");
        delegateMultiPolygon.add(bPoly.buildFeature("fid.1"));
        bPoly.reset();
        Geometry geom2 = reader.read("MULTIPOLYGON (((-2 6, 1 6, 1 3, -2 3, -2 6)))");
        bPoly.add(geom2);
        bPoly.add("two");
        delegateMultiPolygon.add(bPoly.buildFeature("fid.2"));
        bPoly.reset();
        Geometry geom3 = reader.read("MULTIPOLYGON (((-1 5, 2 5, 2 2, -1 2, -1 5)))");
        bPoly.add(geom3);
        bPoly.add("three");
        delegateMultiPolygon.add(bPoly.buildFeature("fid.3"));

        // lines
        // Multi lines
        SimpleFeatureTypeBuilder tbML = new SimpleFeatureTypeBuilder();
        tbML.setName("MultiLines");
        tbML.add("geom", MultiLineString.class);
        tbML.add("name", String.class);
        SimpleFeatureType featureTypeML = tbML.buildFeatureType();
        delegateMultiLines = new DefaultFeatureCollection(null, featureTypeML);
        SimpleFeatureBuilder bML = new SimpleFeatureBuilder(featureTypeML);
        Geometry geomML =
                reader.read(
                        "MULTILINESTRING((1000 0, 1000 1000, 2000 1000, 2000 0), (1000 3000, 1000 2000, 2000 2000, 2000 3000))");
        bML.add(geomML);
        bML.add("one");
        delegateMultiLines.add(bML.buildFeature("fid.1"));
    }

    @Test
    public void testMultiPolygon() throws ParseException {
        WKTReader reader = new WKTReader();

        Geometry clip =
                reader.read(
                        "Polygon ((-1.56800000000000095 5.7651818181818193, 0.2316363636363632 5.75627272727272832, 0.20490909090909026 5.33754545454545593, -1.55018181818181944 5.36427272727272886, -1.56800000000000095 5.7651818181818193))");

        Geometry intersects =
                reader.read(
                        "Polygon ((-2.41436363636363804 1.47100000000000009, 1.77290909090909077 1.23936363636363645, 1.47890909090909028 -0.40881818181818197, -2.83309090909091044 -0.18609090909090931, -2.41436363636363804 1.47100000000000009))");

        ClipIntersectsFeatureCollection collection =
                new ClipIntersectsFeatureCollection(delegateMultiPolygon, clip, intersects);

        assertEquals(2, collection.size());

        try (SimpleFeatureIterator it = collection.features()) {
            SimpleFeature f = it.next();
            Geometry geom = (Geometry) f.getDefaultGeometry();
            SimpleFeature f2 = it.next();
            Geometry geom2 = (Geometry) f2.getDefaultGeometry();
            geom2.normalize();
            clip.normalize();
            assertTrue(intersects.intersects(geom));
            assertFalse(intersects.intersects(geom2));
            assertTrue(clip.covers(geom2));
        }
    }

    @Test
    public void testMultiPolygonClipAndIntersectOnSameGeometry() throws ParseException {

        WKTReader reader = new WKTReader();

        Geometry clip =
                reader.read(
                        "Polygon ((-1.56800000000000095 5.7651818181818193, 0.2316363636363632 5.75627272727272832, 0.20490909090909026 5.33754545454545593, -1.55018181818181944 5.36427272727272886, -1.56800000000000095 5.7651818181818193))");

        Geometry intersects =
                reader.read(
                        "Polygon ((-1.79963636363636481 4.99900000000000144, -1.22054545454545549 5.0613636363636374, -1.30072727272727384 3.49336363636363734, -1.85309090909091023 3.47554545454545538, -1.79963636363636481 4.99900000000000144))");

        ClipIntersectsFeatureCollection collection =
                new ClipIntersectsFeatureCollection(delegateMultiPolygon, clip, intersects);

        assertEquals(1, collection.size());

        try (SimpleFeatureIterator it = collection.features()) {
            SimpleFeature f = it.next();
            Geometry geom = (Geometry) f.getDefaultGeometry();
            assertFalse(intersects.intersects(clip));
            assertTrue(geom.intersects(intersects));
            assertTrue(clip.intersects(geom));
            assertFalse(clip.covers(geom));
            assertTrue(geom.covers(clip));
        }
    }

    @Test
    public void testMultiLinesClippingNotIntersecting() throws ParseException {

        Geometry clip =
                new WKTReader().read("POLYGON((900 900, 900 2100, 2100 2100, 2100 900, 900 900))");

        Geometry intersects =
                new WKTReader().read("POLYGON((-10 -10, -10 -5, -5 -5, -5 -10, -10 -10))");

        ClipIntersectsFeatureCollection collection =
                new ClipIntersectsFeatureCollection(delegateMultiLines, clip, intersects);

        assertEquals(1, collection.size());

        try (SimpleFeatureIterator it = collection.features()) {
            SimpleFeature f = it.next();
            Geometry geom = (Geometry) f.getDefaultGeometry();

            assertFalse(geom.intersects(intersects));
            assertTrue(clip.covers(geom));
        }
    }

    @Test
    public void testMultiLinesIntersectingNotClipping() throws ParseException {

        Geometry intersects =
                new WKTReader().read("POLYGON((900 900, 900 2100, 2100 2100, 2100 900, 900 900))");

        Geometry clip = new WKTReader().read("POLYGON((-10 -10, -10 -5, -5 -5, -5 -10, -10 -10))");

        ClipIntersectsFeatureCollection collection =
                new ClipIntersectsFeatureCollection(delegateMultiLines, clip, intersects);

        assertEquals(1, collection.size());

        try (SimpleFeatureIterator it = collection.features()) {
            SimpleFeature f = it.next();
            Geometry geom = (Geometry) f.getDefaultGeometry();

            assertTrue(geom.intersects(intersects));
            assertFalse(clip.intersects(geom));
        }
    }
}

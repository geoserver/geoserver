/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector.iterator;

import static org.geoserver.wms.vector.VectorTileMapOutputFormatTest.feature;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.NoSuchElementException;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.EmptyFeatureCollection;
import org.junit.Test;

public class CoalescingVTIteratorTest {

    public static final String STRING_PROP_1_1 = "StringProp1_1";
    public static final String STRING_PROP_1_2 = "StringProp1_2";
    public static final String STRING_PROP_1_3 = "StringProp1_3";
    public static final String P11 = "POINT (1 1)";
    public static final String P22 = "POINT (2 2)";
    public static final String P33 = "POINT (3 3)";
    public static final String ID_POINT_1 = "point1";
    public static final String ID_POINT_2 = "point2";
    public static final String ID_POINT_3 = "point3";
    public static final int INT1000 = 1000;
    public static final int INT2000 = 2000;
    public static final int INT3000 = 3000;
    static final SimpleFeatureType POINT_TYPE;
    static final SimpleFeatureType LINE_TYPE;

    static {
        try {
            POINT_TYPE = DataUtilities.createType("points", "sp:String,ip:Integer,geom:Point:srid=4326");
            LINE_TYPE = DataUtilities.createType("lines", "sp:String,ip:Integer,geom:LineString:srid=4326");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEmpty() {
        try (SimpleFeatureIterator fi = new EmptyFeatureCollection(POINT_TYPE).features();
                CoalescingVTIterator vti = new CoalescingVTIterator(new SimpleVTIterator(fi))) {
            assertFalse(vti.hasNext());
            assertThrows(NoSuchElementException.class, vti::next);
        }
    }

    @Test
    public void testNoMerge() throws Exception {
        MemoryDataStore ds = new MemoryDataStore();
        ds.addFeature(feature(POINT_TYPE, ID_POINT_1, STRING_PROP_1_1, INT1000, P11));
        ds.addFeature(feature(POINT_TYPE, ID_POINT_2, STRING_PROP_1_2, INT2000, P22));
        ds.addFeature(feature(POINT_TYPE, ID_POINT_3, STRING_PROP_1_3, INT3000, P33));

        try (SimpleFeatureIterator fi = ds.getFeatureSource(POINT_TYPE.getTypeName())
                        .getFeatures()
                        .features();
                CoalescingVTIterator vti = new CoalescingVTIterator(new SimpleVTIterator(fi))) {

            // first feature
            assertTrue(vti.hasNext());
            VTFeature f1 = vti.next();
            assertEquals(ID_POINT_1, f1.getFeatureId());
            assertEquals(P11, f1.getGeometry().toText());
            assertEquals(Map.of("sp", STRING_PROP_1_1, "ip", INT1000), f1.getProperties());
            // second feature
            assertTrue(vti.hasNext());
            VTFeature f2 = vti.next();
            assertEquals(ID_POINT_2, f2.getFeatureId());
            assertEquals(P22, f2.getGeometry().toText());
            assertEquals(Map.of("sp", STRING_PROP_1_2, "ip", INT2000), f2.getProperties());
            // third feature
            assertTrue(vti.hasNext());
            VTFeature f3 = vti.next();
            assertEquals(ID_POINT_3, f3.getFeatureId());
            assertEquals(P33, f3.getGeometry().toText());
            assertEquals(Map.of("sp", STRING_PROP_1_3, "ip", INT3000), f3.getProperties());
            // no more
            assertFalse(vti.hasNext());
            assertThrows(NoSuchElementException.class, vti::next);
        }
    }

    @Test
    public void testTailMerge() throws Exception {
        MemoryDataStore ds = new MemoryDataStore();
        ds.addFeature(feature(POINT_TYPE, ID_POINT_1, STRING_PROP_1_1, INT1000, P11));
        ds.addFeature(feature(POINT_TYPE, ID_POINT_2, STRING_PROP_1_2, INT2000, P22));
        ds.addFeature(feature(POINT_TYPE, ID_POINT_3, STRING_PROP_1_2, INT2000, P33));

        try (SimpleFeatureIterator fi = ds.getFeatureSource(POINT_TYPE.getTypeName())
                        .getFeatures()
                        .features();
                CoalescingVTIterator vti = new CoalescingVTIterator(new SimpleVTIterator(fi))) {
            // first feature
            assertTrue(vti.hasNext());
            VTFeature f1 = vti.next();
            assertEquals(ID_POINT_1, f1.getFeatureId());
            assertEquals(P11, f1.getGeometry().toText());
            assertEquals(Map.of("sp", STRING_PROP_1_1, "ip", INT1000), f1.getProperties());
            // second feature (merged)
            assertTrue(vti.hasNext());
            VTFeature f2 = vti.next();
            assertEquals(ID_POINT_2, f2.getFeatureId());
            assertEquals("MULTIPOINT ((2 2), (3 3))", f2.getGeometry().toText());
            assertEquals(Map.of("sp", STRING_PROP_1_2, "ip", INT2000), f2.getProperties());
            // no more
            assertFalse(vti.hasNext());
            assertThrows(NoSuchElementException.class, vti::next);
        }
    }

    @Test
    public void testHeadMerge() throws Exception {
        MemoryDataStore ds = new MemoryDataStore();
        ds.addFeature(feature(POINT_TYPE, ID_POINT_1, STRING_PROP_1_1, INT1000, P11));
        ds.addFeature(feature(POINT_TYPE, ID_POINT_2, STRING_PROP_1_1, INT1000, P22));
        ds.addFeature(feature(POINT_TYPE, ID_POINT_3, STRING_PROP_1_3, INT3000, P33));

        try (SimpleFeatureIterator fi = ds.getFeatureSource(POINT_TYPE.getTypeName())
                        .getFeatures()
                        .features();
                CoalescingVTIterator vti = new CoalescingVTIterator(new SimpleVTIterator(fi))) {
            // first feature (merged)
            assertTrue(vti.hasNext());
            VTFeature f1 = vti.next();
            assertEquals(ID_POINT_1, f1.getFeatureId());
            assertEquals("MULTIPOINT ((1 1), (2 2))", f1.getGeometry().toText());
            assertEquals(Map.of("sp", STRING_PROP_1_1, "ip", INT1000), f1.getProperties());
            // second feature
            assertTrue(vti.hasNext());
            VTFeature f2 = vti.next();
            assertEquals(ID_POINT_3, f2.getFeatureId());
            assertEquals(P33, f2.getGeometry().toText());
            assertEquals(Map.of("sp", STRING_PROP_1_3, "ip", INT3000), f2.getProperties());
            // no more
            assertFalse(vti.hasNext());
            assertThrows(NoSuchElementException.class, vti::next);
        }
    }

    @Test
    public void testMergeAll() throws Exception {
        MemoryDataStore ds = new MemoryDataStore();
        ds.addFeature(feature(POINT_TYPE, ID_POINT_1, STRING_PROP_1_1, INT1000, P11));
        ds.addFeature(feature(POINT_TYPE, ID_POINT_2, STRING_PROP_1_1, INT1000, P22));
        ds.addFeature(feature(POINT_TYPE, ID_POINT_3, STRING_PROP_1_1, INT1000, P33));

        try (SimpleFeatureIterator fi = ds.getFeatureSource(POINT_TYPE.getTypeName())
                        .getFeatures()
                        .features();
                CoalescingVTIterator vti = new CoalescingVTIterator(new SimpleVTIterator(fi))) {
            // all features merged into one
            assertTrue(vti.hasNext());
            VTFeature f = vti.next();
            assertEquals(ID_POINT_1, f.getFeatureId());
            assertEquals("MULTIPOINT ((1 1), (2 2), (3 3))", f.getGeometry().toText());
            assertEquals(Map.of("sp", STRING_PROP_1_1, "ip", INT1000), f.getProperties());
            // no more
            assertFalse(vti.hasNext());
            assertThrows(NoSuchElementException.class, vti::next);
        }
    }

    @Test
    public void testMergeLines() throws Exception {
        MemoryDataStore ds = new MemoryDataStore();
        ds.addFeature(feature(LINE_TYPE, "line1", STRING_PROP_1_1, INT1000, "LINESTRING(0 0, 1 1)"));
        ds.addFeature(feature(LINE_TYPE, "line2", STRING_PROP_1_1, INT1000, "LINESTRING(1 1, 2 2)"));
        ds.addFeature(feature(LINE_TYPE, "line3", STRING_PROP_1_1, INT1000, "LINESTRING(3 3, 4 4)"));

        try (SimpleFeatureIterator fi = ds.getFeatureSource(LINE_TYPE.getTypeName())
                        .getFeatures()
                        .features();
                CoalescingVTIterator vti = new CoalescingVTIterator(new SimpleVTIterator(fi))) {
            // all features merged into one
            assertTrue(vti.hasNext());
            VTFeature f = vti.next();
            assertEquals("line1", f.getFeatureId());
            assertEquals(
                    "MULTILINESTRING ((0 0, 1 1, 2 2), (3 3, 4 4))",
                    f.getGeometry().toText());
            assertEquals(Map.of("sp", STRING_PROP_1_1, "ip", INT1000), f.getProperties());
            // no more
            assertFalse(vti.hasNext());
            assertThrows(NoSuchElementException.class, vti::next);
        }
    }
}

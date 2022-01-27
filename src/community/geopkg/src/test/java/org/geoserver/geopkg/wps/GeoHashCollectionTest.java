/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import static org.junit.Assert.*;

import org.geotools.data.DataTestCase;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

public class GeoHashCollectionTest extends DataTestCase {

    @BeforeClass
    public static void setup() {
        System.setProperty("org.geotools.referencing.forceXY", "true");
        CRS.reset("all");
    }

    @AfterClass
    public static void teardown() {
        System.clearProperty("org.geotools.referencing.forceXY");
        CRS.reset("all");
    }

    @Test
    public void testSimpleDecorationSchema() {
        SimpleFeatureCollection roads = DataUtilities.collection(roadFeatures);
        GeoHashCollection ghc = new GeoHashCollection(roads);
        SimpleFeatureType roadsType = roads.getSchema();
        SimpleFeatureType ghType = ghc.getSchema();
        assertEquals(roadsType.getAttributeCount() + 1, ghType.getAttributeCount());
        for (int i = 0; i < roadsType.getAttributeCount(); i++) {
            assertEquals(roadsType.getDescriptor(i), ghType.getDescriptor(i));
        }
        AttributeDescriptor ghDescriptor = ghType.getDescriptor(roadsType.getAttributeCount());
        assertEquals("geohash", ghDescriptor.getLocalName());
        assertEquals(String.class, ghDescriptor.getType().getBinding());
        assertEquals("geohash", ghc.getGeoHashFieldName());
    }

    @Test
    public void testDecoratedFeatures() {
        SimpleFeatureCollection roads = DataUtilities.collection(roadFeatures);
        GeoHashCollection ghc = new GeoHashCollection(roads);
        SimpleFeature[] features = ghc.toArray(new SimpleFeature[5]);
        assertEquals("s06", features[0].getAttribute("geohash"));
        assertEquals("s065kk0dc540", features[1].getAttribute("geohash")); // vertical line
        assertEquals("s06", features[2].getAttribute("geohash"));
    }

    @Test
    public void testConflictingAttribute() {
        SimpleFeatureCollection roads = DataUtilities.collection(roadFeatures);
        GeoHashCollection ghc = new GeoHashCollection(roads);
        SimpleFeatureType roadsType = roads.getSchema();
        SimpleFeatureType ghType = ghc.getSchema();
        assertEquals(roadsType.getAttributeCount() + 1, ghType.getAttributeCount());
        for (int i = 0; i < roadsType.getAttributeCount(); i++) {
            assertEquals(roadsType.getDescriptor(i), ghType.getDescriptor(i));
        }
        AttributeDescriptor ghDescriptor = ghType.getDescriptor(roadsType.getAttributeCount());
        assertEquals("geohash", ghDescriptor.getLocalName());
        assertEquals(String.class, ghDescriptor.getType().getBinding());
        assertEquals("geohash", ghc.getGeoHashFieldName());
    }
}

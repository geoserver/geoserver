/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

public class ReprojectingFeatureCollectionTest {

    @Test
    public void testPerserveUserData() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("foo");
        tb.setSRS("epsg:4326");
        tb.add("geom", Point.class);

        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());
        b.add(new WKTReader().read("POINT(1 1)"));
        SimpleFeature f = b.buildFeature(null);
        f.getUserData().put("foo", "bar");

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        features.add(f);

        try (FeatureIterator it = features.features()) {
            assertEquals("bar", it.next().getUserData().get("foo"));
        }

        ReprojectingFeatureCollection reprojected =
                new ReprojectingFeatureCollection(features, CRS.decode("EPSG:3005"));
        try (FeatureIterator it = reprojected.features()) {
            assertEquals("bar", it.next().getUserData().get("foo"));
        }
    }

    /** Per-feature reprojection between two 3D CRSs must not throw ClassCastException (GEOS-12159). */
    @Test
    public void testReprojectPerFeatureCRSWithGenuine3DCRS() throws Exception {
        CoordinateReferenceSystem geometryCrs = CRS.decode("EPSG:4979"); // WGS84 3D
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:4937"); // ETRS89 3D - different datum

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("threeDimensional");
        // Schema CRS matches the target, not the per-feature geometry CRS, so the per-feature
        // transform is built fresh (cache miss) rather than reusing the constructor's transform.
        tb.setSRS("EPSG:4937");
        tb.add("geom", Point.class);

        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());
        Point point = (Point) new WKTReader().read("POINT(10 20 123)");
        point.setUserData(geometryCrs);
        b.add(point);
        SimpleFeature f = b.buildFeature("f1");

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        features.add(f);

        ReprojectingFeatureCollection reprojected = new ReprojectingFeatureCollection(features, targetCrs);
        try (FeatureIterator it = reprojected.features()) {
            assertTrue(it.hasNext());
            it.next();
        }
    }
}

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

    /**
     * reproject(SimpleFeature) resolves a per-feature transform (via the geometry's own user data CRS, not the
     * collection's cached default-source transform) through a code path that casts the result to MathTransform2D. That
     * cast is safe whenever the transform happens to be 2-dimensional, but throws ClassCastException for a genuine
     * 3D-to-3D coordinate operation - e.g. between two different Geographic 3D CRSs (EPSG:4979 WGS84 3D and EPSG:4937
     * ETRS89 3D here), where latitude, longitude and ellipsoidal height are all axes of the CRS itself, not a 2D CRS
     * with a Z ordinate merely riding along on the geometry.
     *
     * <p>This is exactly what happens in practice when a WFS-T feature's geometry carries an explicit CRS (e.g. one a
     * client resolved from a GetCapabilities document) that differs from the feature type's own declared CRS, for a
     * genuinely 3-dimensional CRS pair - the per-feature transformer has to be built for the first time (a cache miss),
     * taking this cast path, rather than reusing the constructor's own already-cached, correctly uncast transformer for
     * the feature type's declared CRS.
     */
    @Test
    public void testReprojectPerFeatureCRSWithGenuine3DCRS() throws Exception {
        CoordinateReferenceSystem geometryCrs = CRS.decode("EPSG:4979"); // WGS84 3D
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:4937"); // ETRS89 3D - different datum

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("threeDimensional");
        // Deliberately matches the *target* CRS, not the per-feature geometry CRS below: the
        // constructor primes its transformer cache keyed by this schema CRS, using the safe
        // (uncast) MathTransform path. If this matched the geometry's own CRS instead, that
        // priming step would satisfy reproject()'s cache lookup before ever reaching its own,
        // separately-cast, per-feature transformer construction - masking the bug entirely.
        tb.setSRS("EPSG:4937");
        tb.add("geom", Point.class);

        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());
        Point point = (Point) new WKTReader().read("POINT(10 20 123)");
        // The per-feature CRS on the geometry's own user data - not the feature type's CRS -
        // is what reproject() actually consults first (see its own javadoc).
        point.setUserData(geometryCrs);
        b.add(point);
        SimpleFeature f = b.buildFeature("f1");

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        features.add(f);

        ReprojectingFeatureCollection reprojected = new ReprojectingFeatureCollection(features, targetCrs);
        try (FeatureIterator it = reprojected.features()) {
            assertTrue(it.hasNext());
            // Must not throw - previously failed with:
            // java.io.IOException: Could not transform for crs: ...
            // Caused by: java.lang.ClassCastException: class
            // org.geotools.referencing.operation.transform.ConcatenatedTransformDirect cannot be cast to class
            // org.geotools.api.referencing.operation.MathTransform2D
            it.next();
        }
    }
}

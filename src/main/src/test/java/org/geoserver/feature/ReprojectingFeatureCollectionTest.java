/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import static org.junit.Assert.*;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;

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

        FeatureIterator it = features.features();

        try {
            assertEquals("bar", it.next().getUserData().get("foo"));
        } finally {
            it.close();
        }

        ReprojectingFeatureCollection reprojected =
                new ReprojectingFeatureCollection(features, CRS.decode("EPSG:3005"));
        it = reprojected.features();

        try {
            assertEquals("bar", it.next().getUserData().get("foo"));
        } finally {
            it.close();
        }
    }
}

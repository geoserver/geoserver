/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.vector.InclusionFeatureCollection;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;

public class InclusionFeatureCollectionTest extends WPSTestSupport {

    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
    GeometryFactory gf = new GeometryFactory();

    @Test
    public void testExecute() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        DefaultFeatureCollection secondFeatures =
                new DefaultFeatureCollection(null, b.getFeatureType());

        Coordinate firstArray[] = new Coordinate[5];
        for (int numFeatures = 0; numFeatures < 1; numFeatures++) {
            firstArray[0] = new Coordinate(0, 0);
            firstArray[1] = new Coordinate(1, 0);
            firstArray[2] = new Coordinate(1, 1);
            firstArray[3] = new Coordinate(0, 1);
            firstArray[4] = new Coordinate(0, 0);
            LinearRing shell = gf.createLinearRing(firstArray);
            b.add(gf.createPolygon(shell, null));
            b.add(0);

            features.add(b.buildFeature(numFeatures + ""));
        }
        for (int numFeatures = 0; numFeatures < 1; numFeatures++) {
            Coordinate array[] = new Coordinate[5];
            array[0] = new Coordinate(firstArray[0].x - 1, firstArray[0].y - 1);
            array[1] = new Coordinate(firstArray[1].x + 1, firstArray[1].y - 1);
            array[2] = new Coordinate(firstArray[2].x + 1, firstArray[2].y + 1);
            array[3] = new Coordinate(firstArray[3].x - 1, firstArray[3].y + 1);
            array[4] = new Coordinate(firstArray[0].x - 1, firstArray[0].y - 1);
            LinearRing shell = gf.createLinearRing(array);
            b.add(gf.createPolygon(shell, null));
            b.add(0);

            secondFeatures.add(b.buildFeature(numFeatures + ""));
        }
        InclusionFeatureCollection process = new InclusionFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, secondFeatures);
        assertEquals(1, output.size());
        SimpleFeatureIterator iterator = output.features();

        Geometry expected = (Geometry) features.features().next().getDefaultGeometry();
        SimpleFeature sf = iterator.next();
        assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
    }

    @Test
    public void testExecute1() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        DefaultFeatureCollection secondFeatures =
                new DefaultFeatureCollection(null, b.getFeatureType());

        Coordinate firstArray[] = new Coordinate[5];
        for (int numFeatures = 0; numFeatures < 1; numFeatures++) {
            firstArray[0] = new Coordinate(0, 0);
            firstArray[1] = new Coordinate(1, 0);
            firstArray[2] = new Coordinate(1, 1);
            firstArray[3] = new Coordinate(0, 1);
            firstArray[4] = new Coordinate(0, 0);
            LinearRing shell = gf.createLinearRing(firstArray);
            b.add(gf.createPolygon(shell, null));
            b.add(0);

            secondFeatures.add(b.buildFeature(numFeatures + ""));
        }

        Coordinate centre =
                ((Polygon) secondFeatures.features().next().getDefaultGeometry())
                        .getCentroid()
                        .getCoordinate();
        Point p = gf.createPoint(centre);
        b.add(p);
        b.add(0);

        features.add(b.buildFeature(0 + ""));

        InclusionFeatureCollection process = new InclusionFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, secondFeatures);
        assertEquals(1, output.size());
        SimpleFeatureIterator iterator = output.features();

        Geometry expected = (Geometry) features.features().next().getDefaultGeometry();
        SimpleFeature sf = iterator.next();
        assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
    }
}

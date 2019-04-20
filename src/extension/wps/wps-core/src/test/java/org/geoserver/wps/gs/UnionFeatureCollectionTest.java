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
import org.geotools.process.vector.UnionFeatureCollection;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;

public class UnionFeatureCollectionTest extends WPSTestSupport {

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
        Geometry[] firstArrayGeometry = new Geometry[5];
        Geometry[] secondArrayGeometry = new Geometry[5];
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate firstArray[] = new Coordinate[5];
            for (int j = 0; j < 4; j++) {
                firstArray[j] = new Coordinate(j + numFeatures, j + numFeatures);
            }
            firstArray[4] = new Coordinate(0 + numFeatures, 0 + numFeatures);
            LinearRing shell = gf.createLinearRing(new CoordinateArraySequence(firstArray));
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            firstArrayGeometry[numFeatures] = gf.createPolygon(shell, null);
            features.add(b.buildFeature(numFeatures + ""));
        }
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate array[] = new Coordinate[5];
            for (int j = 0; j < 4; j++) {
                array[j] = new Coordinate(j + numFeatures + 50, j + numFeatures + 50);
            }
            array[4] = new Coordinate(0 + numFeatures + 50, 0 + numFeatures + 50);
            LinearRing shell = gf.createLinearRing(new CoordinateArraySequence(array));
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            secondArrayGeometry[numFeatures] = gf.createPolygon(shell, null);
            secondFeatures.add(b.buildFeature(numFeatures + ""));
        }
        UnionFeatureCollection process = new UnionFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, secondFeatures);
        assertEquals(5, output.size());

        Geometry[] union = new Geometry[10];
        for (int i = 0; i < firstArrayGeometry.length; i++) {
            union[i] = firstArrayGeometry[i];
        }
        for (int i = 0; i < secondArrayGeometry.length; i++) {
            union[i + 5] = secondArrayGeometry[i];
        }
        GeometryCollection unionCollection = new GeometryCollection(union, new GeometryFactory());
        SimpleFeatureIterator iterator = output.features();

        for (int h = 0; h < unionCollection.getNumGeometries(); h++) {
            Geometry expected = (Geometry) unionCollection.getGeometryN(h);
            SimpleFeature sf = iterator.next();
            assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
        }
    }
}

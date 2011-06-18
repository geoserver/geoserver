package org.geoserver.wps.gs;

import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class IntersectionFeatureCollectionTest extends WPSTestSupport {

    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    public void testExecute() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        DefaultFeatureCollection secondFeatures = new DefaultFeatureCollection(null, b
                .getFeatureType());
        Geometry[] firstArrayGeometry = new Geometry[1];
        Geometry[] secondArrayGeometry = new Geometry[1];
        for (int numFeatures = 0; numFeatures < 1; numFeatures++) {
            Coordinate array[] = new Coordinate[5];
            array[0] = new Coordinate(0, 0);
            array[1] = new Coordinate(1, 0);
            array[2] = new Coordinate(1, 1);
            array[3] = new Coordinate(0, 1);
            array[4] = new Coordinate(0, 0);
            LinearRing shell = new LinearRing(array, new PrecisionModel(), 0);
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            firstArrayGeometry[0] = gf.createPolygon(shell, null);
            features.add(b.buildFeature(numFeatures + ""));

        }
        for (int numFeatures = 0; numFeatures < 1; numFeatures++) {
            Coordinate array[] = new Coordinate[5];
            Coordinate centre = ((Polygon) features.features().next().getDefaultGeometry())
                    .getCentroid().getCoordinate();
            array[0] = new Coordinate(centre.x, centre.y);
            array[1] = new Coordinate(centre.x + 1, centre.y);
            array[2] = new Coordinate(centre.x + 1, centre.y + 1);
            array[3] = new Coordinate(centre.x, centre.y + 1);
            array[4] = new Coordinate(centre.x, centre.y);
            LinearRing shell = new LinearRing(array, new PrecisionModel(), 0);
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            secondArrayGeometry[0] = gf.createPolygon(shell, null);
            secondFeatures.add(b.buildFeature(numFeatures + ""));
        }
        IntersectionFeatureCollection process = new IntersectionFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, secondFeatures);
        assertEquals(1, output.size());
        SimpleFeatureIterator iterator = output.features();

        GeometryCollection firstCollection = null;
        GeometryCollection secondCollection = null;
        firstCollection = new GeometryCollection(firstArrayGeometry, new GeometryFactory());
        secondCollection = new GeometryCollection(secondArrayGeometry, new GeometryFactory());

        for (int i = 0; i < firstCollection.getNumGeometries(); i++) {
            Geometry expected = (Geometry) firstCollection.getGeometryN(i).intersection(
                    secondCollection.getGeometryN(i));
            SimpleFeature sf = iterator.next();
            assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
        }
    }

}

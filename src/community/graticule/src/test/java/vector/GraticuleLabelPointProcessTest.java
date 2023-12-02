package vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.data.DataUtilities;
import org.geotools.data.graticule.GraticuleDataStore;
import org.geotools.data.graticule.GraticuleDataStoreFactory;
import org.geotools.data.graticule.gridsupport.LineFeatureBuilder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.function.StaticGeometry;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.vector.GraticuleLabelPointProcess;
import org.geotools.process.vector.ProcessTestUtilities;
import org.geotools.process.vector.VectorProcess;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraticuleLabelPointProcessTest {

    @Test
    public void testLabelGrid() throws Exception {
        ArrayList<Double> steps = new ArrayList<>();
        steps.add(10.0);
        steps.add(30.0);
        ReferencedEnvelope bounds = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        bounds.expandToInclude(-180, -90);
        bounds.expandToInclude(180, 90);
        GraticuleDataStore store = new GraticuleDataStore(bounds, steps);

        SimpleFeatureCollection features = store.getFeatureSource("10.0").getFeatures();

        GraticuleLabelPointProcess process = new GraticuleLabelPointProcess();

        SimpleFeatureCollection results = process.execute(features, bounds, GraticuleLabelPointProcess.PositionEnum.TOPLEFT);
        assertEquals(features.size(), results.size());
        try(SimpleFeatureIterator iterator = results.features()){
            while (iterator.hasNext()){
                SimpleFeature feature = iterator.next();
                Point p = (Point) feature.getAttribute("element");
                System.out.println(p);
                assertTrue(Math.floor(p.getX())==-180 || Math.floor(p.getY()) == 90);

            }
        }
    }
}

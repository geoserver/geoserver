package vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.graticule.GraticuleDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.vector.GraticuleLabelPointProcess;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Point;

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

        SimpleFeatureCollection features = store.getFeatureSource("10_0").getFeatures();

        GraticuleLabelPointProcess process = new GraticuleLabelPointProcess();

        SimpleFeatureCollection results =
                process.execute(features, bounds, GraticuleLabelPointProcess.PositionEnum.TOPLEFT);
        assertEquals(features.size(), results.size());
        try (SimpleFeatureIterator iterator = results.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Point p = (Point) feature.getAttribute("element");
                System.out.println(p);
                assertTrue(Math.floor(p.getX()) == -180 || Math.floor(p.getY()) == 90);
            }
        }
    }
}

package org.geotools.process.vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.graticule.GraticuleDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Point;

public class GraticuleLabelPointProcessTest {
    DataStore store;
    ReferencedEnvelope bounds;

    @Before
    public void setup() throws IOException {
        HashMap<String, Object> params = new HashMap<>();
        ArrayList<Double> steps = new ArrayList<>();
        steps.add(10.0);
        steps.add(30.0);
        params.put(GraticuleDataStoreFactory.STEPS.key, steps);
        bounds = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        bounds.expandToInclude(-180, -90);
        bounds.expandToInclude(180, 90);
        params.put(GraticuleDataStoreFactory.BOUNDS.key, bounds);
        params.put(GraticuleDataStoreFactory.TYPE.key, GraticuleDataStoreFactory.TYPE.sample);
        store = DataStoreFinder.getDataStore(params);
        assertNotNull(store);
    }

    @Test
    public void testBothLabelGrid() throws Exception {

        GraticuleLabelPointProcess.PositionEnum pos = GraticuleLabelPointProcess.PositionEnum.BOTH;
        ReferencedEnvelope box = bounds;

        runLabels(box, pos);
    }

    @Test
    public void testBigBBox() throws Exception {
        ReferencedEnvelope bbox =
                new ReferencedEnvelope(
                        -260.15625, 279.84375, -97.734375, 172.265625, DefaultGeographicCRS.WGS84);
        runLabels(bbox, GraticuleLabelPointProcess.PositionEnum.BOTH);
    }

    @Test
    public void testSmallBBox() throws Exception {
        ReferencedEnvelope bbox = new ReferencedEnvelope(-92.724609375,-25.224609375,-0.615234375,33.134765625,DefaultGeographicCRS.WGS84);
        runLabels(bbox, GraticuleLabelPointProcess.PositionEnum.BOTH);
    }
    private void runLabels(ReferencedEnvelope box, GraticuleLabelPointProcess.PositionEnum pos)
            throws IOException {
        SimpleFeatureCollection features = store.getFeatureSource("10_0").getFeatures();

        GraticuleLabelPointProcess process = new GraticuleLabelPointProcess();

        SimpleFeatureCollection results = process.execute(features, box, pos);
        //assertEquals(features.size() * 2, results.size());
        try (SimpleFeatureIterator iterator = results.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Point p = (Point) feature.getAttribute("element");
                System.out.println(feature);
                boolean top = (boolean) feature.getAttribute("top");
                boolean left = (boolean) feature.getAttribute("left");
                boolean horizontal = (boolean) feature.getAttribute("horizontal");

                if(horizontal) {
                    if (left) {
                        assertEquals("wrong left", Math.floor(Math.max(bounds.getMinimum(0), box.getMinimum(0))), Math.floor(p.getX() ), 0.1);
                    }
                    if (!left) {
                        assertEquals("wrong right", Math.ceil(Math.min(bounds.getMaximum(0), box.getMaximum(0))), Math.ceil(p.getX() + GraticuleLabelPointProcess.DELTA), 0.1);
                    }
                } else {
                    if (top) {
                        assertEquals("wrong top", Math.floor(Math.min(bounds.getMaximum(1), box.getMaximum(1))), Math.floor(p.getY() ), 0.1);
                    }
                    if (!top) {
                        assertEquals("wrong bottom", Math.ceil(Math.max(bounds.getMinimum(1), box.getMinimum(1))), Math.ceil(p.getY() + GraticuleLabelPointProcess.DELTA), 0.1);
                    }
                }
            }
        }
    }
}

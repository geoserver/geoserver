package org.geotools.data.graticule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GraticuleFeatureReaderTest {
    private DataStore datastore;
    ArrayList<Double> steps = new ArrayList<>();

    @Before
    public void setup() throws IOException {
        HashMap<String, Object> params = new HashMap<>();
        steps = new ArrayList<>();
        steps.add(10.0);
        steps.add(25.0);
        steps.add(30.0);
        params.put(GraticuleDataStoreFactory.STEPS.key, steps);
        ReferencedEnvelope bounds = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        bounds.expandToInclude(-180, -90);
        bounds.expandToInclude(180, 90);
        params.put(GraticuleDataStoreFactory.BOUNDS.key, bounds);
        params.put(GraticuleDataStoreFactory.TYPE.key, GraticuleDataStoreFactory.TYPE.sample);
        datastore = DataStoreFinder.getDataStore(params);
        Assert.assertNotNull(datastore);
    }

    @Test
    public void testReader() throws Exception {
        FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                datastore.getFeatureReader(new Query(), null);
        Assert.assertNotNull(reader);
        double[] counts = new double[steps.size()];
        while (reader.hasNext()) {
            SimpleFeature f = reader.next();
            int level = (int) f.getAttribute("level");
            System.out.println(f.getAttribute("label") + " " + f.getAttribute("level"));
            counts[level]++;
        }
        for (int i = 0; i < steps.size(); i++) {
            System.out.println("" + i + ": " + counts[i]);
        }
    }
}

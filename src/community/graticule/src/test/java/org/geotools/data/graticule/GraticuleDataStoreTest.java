package org.geotools.data.graticule;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class GraticuleDataStoreTest {

    @Test
    public void testCreation() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        ArrayList<Double> steps = new ArrayList<>();
        steps.add(10.0);
        steps.add(30.0);
        params.put(GraticuleDataStoreFactory.STEPS.key, steps);
        ReferencedEnvelope bounds = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        bounds.expandToInclude(-180, -90);
        bounds.expandToInclude(180, 90);
        params.put(GraticuleDataStoreFactory.BOUNDS.key, bounds);
        DataStore datastore = DataStoreFinder.getDataStore(params);
        String[] names = datastore.getTypeNames();
        for (String name : names) {
            System.out.println(name);
        }
        SimpleFeatureType schema = datastore.getSchema("10.0");
        assertNotNull(schema);
        System.out.println(schema);
    }
}

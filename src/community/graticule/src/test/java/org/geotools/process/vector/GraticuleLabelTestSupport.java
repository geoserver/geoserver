/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.process.vector;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.data.graticule.GraticuleDataStoreFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;

public class GraticuleLabelTestSupport {
    public DataStore store;
    protected ReferencedEnvelope bounds;
    protected ArrayList<Double> steps = new ArrayList<>();

    Logger log = Logger.getLogger("GraticuleLabelTest");

    @Before
    public void setup() throws IOException {
        HashMap<String, Object> params = new HashMap<>();

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
}

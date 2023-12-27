/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import static org.geotools.data.graticule.GraticuleDataStoreFactory.BOUNDS;
import static org.geotools.data.graticule.GraticuleDataStoreFactory.STEPS;
import static org.geotools.data.graticule.GraticuleDataStoreFactory.TYPE;
import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class GraticuleDataStoreFactoryTest {

    @Test
    public void testWrongType() throws Throwable {
        HashMap<String, Object> params = getGraticuleParamsMap();
        params.put(TYPE.key, "foobar");
        assertNull(DataStoreFinder.getDataStore(params));
    }

    @Test
    public void testWrongSteps() throws Throwable {
        HashMap<String, Object> params = getGraticuleParamsMap();
        params.put(STEPS.key, "a,b,c");
        assertNull(DataStoreFinder.getDataStore(params));
    }

    @Test
    public void testMissingBounds() throws Throwable {
        HashMap<String, Object> params = getGraticuleParamsMap();
        params.remove(BOUNDS.key);
        assertNull(DataStoreFinder.getDataStore(params));
    }

    @Test
    public void testValid() throws Throwable {
        HashMap<String, Object> params = getGraticuleParamsMap();
        assertThat(
                DataStoreFinder.getDataStore(params),
                CoreMatchers.instanceOf(GraticuleDataStore.class));
    }

    private static HashMap<String, Object> getGraticuleParamsMap() {
        HashMap<String, Object> params = new HashMap<>();
        params.put(STEPS.key, List.of(10.0, 30.0));
        ReferencedEnvelope bounds = new ReferencedEnvelope(-180, 180, -90, 90, WGS84);
        params.put(BOUNDS.key, bounds);
        params.put(TYPE.key, TYPE.sample);
        return params;
    }

    @Test
    public void testParamParserWGS84() throws Throwable {
        ReferencedEnvelope bounds = new ReferencedEnvelope(WGS84);
        bounds.expandToInclude(-180, -90);
        bounds.expandToInclude(180, 90);
        checkBounds(bounds);
    }

    @Test
    public void testParamParserOSGB() throws Throwable {
        ReferencedEnvelope bounds = new ReferencedEnvelope(CRS.decode("EPSG:27700"));
        bounds.expandToInclude(0, 0);
        bounds.expandToInclude(700000, 1300000);
        checkBounds(bounds);
    }

    private static void checkBounds(ReferencedEnvelope bounds) throws Throwable {
        String text = BOUNDS.text(bounds);
        Object obs = BOUNDS.parse(text);
        assertEquals(obs, bounds);
    }
}

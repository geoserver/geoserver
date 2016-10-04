/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wmts;

import org.geoserver.gwc.GWC;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

import static org.geoserver.inspire.wmts.InspireGridSetLoader.INSPIRE_GRID_SET_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Testing that INSPIRE grid set is correctly loaded and configured.
 */
public class InspireGridSetLoaderTest extends GeoServerSystemTestSupport {

    @Test
    public void testGridSetLoading() throws Exception {
        // let's wait a max of 30 seconds for the grid set registration
        GWC gwc = GWC.get();
        int waited = 0;
        while(waited <= 30000 && GWC.get().getGridSetBroker().get(INSPIRE_GRID_SET_NAME) == null) {
            // let' wait 100 milliseconds
            Thread.sleep(100);
            waited += 100;
        }
        // let's see if the inspire grid set has been correctly registered
        assertThat(gwc.getGridSetBroker().get(INSPIRE_GRID_SET_NAME), notNullValue());
        assertThat(gwc.isInternalGridSet(INSPIRE_GRID_SET_NAME), is(true));
    }
}
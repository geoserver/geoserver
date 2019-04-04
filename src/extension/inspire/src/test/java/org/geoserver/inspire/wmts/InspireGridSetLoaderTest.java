/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wmts;

import static org.geoserver.inspire.wmts.InspireGridSetLoader.INSPIRE_GRID_SET_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.geoserver.gwc.GWC;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/** Testing that INSPIRE grid set is correctly loaded and configured. */
public class InspireGridSetLoaderTest extends GeoServerSystemTestSupport {

    @Test
    public void testGridSetLoading() throws Exception {
        GWC gwc = GWC.get();
        // let's see if the inspire grid set has been correctly registered
        assertThat(gwc.getGridSetBroker().get(INSPIRE_GRID_SET_NAME), notNullValue());
        assertThat(gwc.isInternalGridSet(INSPIRE_GRID_SET_NAME), is(true));
    }
}

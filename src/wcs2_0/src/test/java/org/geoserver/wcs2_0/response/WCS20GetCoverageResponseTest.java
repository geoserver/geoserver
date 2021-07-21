/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import static org.hamcrest.MatcherAssert.assertThat;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matchers;
import org.junit.Test;

public class WCS20GetCoverageResponseTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
    }

    @Test
    public void testOutputFormats() {
        WCS20GetCoverageResponse response =
                GeoServerExtensions.bean(WCS20GetCoverageResponse.class, applicationContext);
        assertThat(
                response.getOutputFormats(),
                Matchers.hasItems("image/geotiff", "text/debug", "application/gml+xml"));
    }
}

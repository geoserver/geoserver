/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

package org.geoserver.security.oauth2;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class SpringWiringTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data to setup, this is a smoke test
    }

    @Test
    public void testStartup() {
        // nothing to do, just checks the beans are properly wired
    }
}

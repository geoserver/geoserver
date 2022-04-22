/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.junit.Assert.assertNotNull;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class OSEOXStreamLoaderTest extends GeoServerSystemTestSupport {

    @Test
    public void testInit() throws Exception {
        OSEOXStreamLoader loader = GeoServerExtensions.bean(OSEOXStreamLoader.class);
        OSEOInfo oseo = new OSEOInfoImpl();
        loader.initializeService(oseo);
        assertNotNull(oseo.getGlobalQueryables());
    }
}

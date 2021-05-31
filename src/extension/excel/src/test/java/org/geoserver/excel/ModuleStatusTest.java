/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.excel;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class ModuleStatusTest extends GeoServerSystemTestSupport {
    @Test
    public void test() {
        assertModuleStatus("gs-excel", "Excel Output Extension");
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.web;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class ModuleStatusTest extends GeoServerSystemTestSupport {

    @Test
    public void ModuleStatusTest() {
        assertModuleStatus("gs-web-csw", "GeoServer Catalog Service for the Web (CSW) Web UI");
    }
}

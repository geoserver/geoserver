/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import org.junit.Test;

public class ModuleStatusTest extends CSWTestSupport {

    @Test
    public void ModuleStatusTest() {
        assertModuleStatus(
                "gs-csw-core", "GeoServer Catalog Service for the Web (CSW) 2.0.2 Core Service");
    }
}

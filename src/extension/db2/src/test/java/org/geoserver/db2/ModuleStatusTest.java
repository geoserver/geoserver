/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.db2;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.db2.DB2NGDataStoreFactory;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ModuleStatusTest extends GeoServerSystemTestSupport {
    @Test
    public void testStatus() {
        assertModuleStatus("gs-db2", "DB2 Extension");
    }

    @Test
    public void test() {

        DB2NGDataStoreFactory fac = new DB2NGDataStoreFactory();
        Boolean expect;
        if (fac.isAvailable()) {
            expect = Boolean.TRUE;
        } else {
            expect = Boolean.FALSE;
        }
        try (ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("applicationContext.xml")) {

            Optional<ModuleStatus> status =
                    GeoServerExtensions.extensions(ModuleStatus.class, context).stream()
                            .filter(s -> s.getModule().equalsIgnoreCase("gs-db2"))
                            .findFirst();
            assertEquals(expect, status.isPresent());
        }
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sqlserver;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geotools.data.sqlserver.SQLServerDataStoreFactory;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ModuleStatusTest {
    @Test
    public void test() {
        SQLServerDataStoreFactory fac = new SQLServerDataStoreFactory();
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
                            .filter(s -> s.getModule().equalsIgnoreCase("gs-sqlserver"))
                            .findFirst();
            assertEquals(expect, status.isPresent());
        }
    }
}

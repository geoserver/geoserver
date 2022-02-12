/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ModuleStatusTest {
    @Test
    @Ignore("as it breaks the other tests, something to do with the application contexts!")
    public void test() {
        try (ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("testApplicationContext.xml")) {
            assertNotNull(context);

            Optional<ModuleStatus> status =
                    GeoServerExtensions.extensions(ModuleStatus.class, context).stream()
                            .filter(s -> s.getModule().equalsIgnoreCase("gs-params-extractor"))
                            .findFirst();
            assertTrue(status.isPresent());
        }
    }
}

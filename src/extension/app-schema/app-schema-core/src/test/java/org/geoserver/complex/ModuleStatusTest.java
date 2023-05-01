package org.geoserver.complex;

import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ModuleStatusTest {
    @Test
    public void test() {

        try (ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("applicationContext.xml")) {

            Optional<ModuleStatus> status =
                    GeoServerExtensions.extensions(ModuleStatus.class, context).stream()
                            .filter(s -> s.getModule().equalsIgnoreCase("gs-app-schema-core"))
                            .findFirst();
            assertTrue(status.isPresent());
        }
    }
}

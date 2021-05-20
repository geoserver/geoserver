package org.geoserver.terradata;

import static org.junit.Assert.assertTrue;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geotools.util.URLs;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ModuleStatusTest {
    @Test
    public void test() {

        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        Optional<ModuleStatus> status = GeoServerExtensions.extensions(ModuleStatus.class, context)
                .stream().filter(s -> s.getModule().equalsIgnoreCase("gs-terradata")).findFirst();
        assertTrue(status.isPresent());
    }
}

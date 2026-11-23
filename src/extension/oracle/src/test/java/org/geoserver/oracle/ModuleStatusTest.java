package org.geoserver.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geotools.data.oracle.OracleNGDataStoreFactory;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ModuleStatusTest {
    @Test
    public void test() {

        OracleNGDataStoreFactory fac = new OracleNGDataStoreFactory();
        Boolean expect;
        if (fac.isAvailable()) {
            expect = Boolean.TRUE;
        } else {
            expect = Boolean.FALSE;
        }
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml")) {

            Optional<ModuleStatus> status = GeoServerExtensions.extensions(ModuleStatus.class, context).stream()
                    .filter(s -> s.getModule().equalsIgnoreCase("gs-oracle"))
                    .findFirst();
            assertEquals(expect, status.isPresent());
            if (status.isPresent()) {
                String message = status.get().getMessage().orElseThrow();
                assertTrue(message, message.startsWith("Oracle JDBC driver version: "));
            }
        }
    }
}

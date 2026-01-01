/* Copyright (C) 2025 - Open Source Geospatial Foundation. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.arcgrid;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geotools.gce.arcgrid.ArcGridFormatFactory;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ModuleStatusTest {
    @Test
    public void test() {

        ArcGridFormatFactory fac = new ArcGridFormatFactory();
        Boolean expect;
        if (fac.isAvailable()) {
            expect = Boolean.TRUE;
        } else {
            expect = Boolean.FALSE;
        }
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml")) {

            Optional<ModuleStatus> status = GeoServerExtensions.extensions(ModuleStatus.class, context).stream()
                    .filter(s -> s.getModule().equalsIgnoreCase("gs-arcgrid"))
                    .findFirst();
            assertEquals(expect, status.isPresent());
        }
    }
}

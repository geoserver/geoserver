/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.Parameter;
import org.junit.Test;

public class IgnorableParameterRegistryTest {

    @Test
    public void testBuiltIns() {
        IgnorableParameterRegistry reg = new IgnorableParameterRegistry();
        assertTrue(reg.isIgnorable(new Parameter<>(ImageMosaicFormat.ALLOW_MULTITHREADING)));
        assertTrue(reg.isIgnorable(new Parameter<>(ImageMosaicFormat.MAX_ALLOWED_TILES)));
        assertTrue(reg.isIgnorable(new Parameter<>(AbstractGridFormat.SUGGESTED_TILE_SIZE)));
    }

    @Test
    public void testNotIgnorable() {
        IgnorableParameterRegistry reg = new IgnorableParameterRegistry();
        assertFalse(reg.isIgnorable(new Parameter<>(ImageMosaicFormat.FILTER)));
    }

    @Test
    public void testRegister() {
        DefaultParameterDescriptor<Boolean> custom =
                new DefaultParameterDescriptor<>("MyCustomParam", Boolean.class, null, null);
        IgnorableParameterRegistry reg = new IgnorableParameterRegistry();
        reg.registerIgnorable(custom);
        assertTrue(reg.isIgnorable(new Parameter<>(custom)));
        // built-ins still present
        assertTrue(reg.isIgnorable(new Parameter<>(ImageMosaicFormat.ALLOW_MULTITHREADING)));
    }

    @Test
    public void testSystemProperty() {
        System.setProperty(IgnorableParameterRegistry.SYSTEM_PROPERTY, "MY_CUSTOM_PARAM, ANOTHER_PARAM");
        try {
            IgnorableParameterRegistry reg = new IgnorableParameterRegistry();
            assertTrue(reg.isIgnorable(param("MY_CUSTOM_PARAM")));
            assertTrue(reg.isIgnorable(param("ANOTHER_PARAM")));
            // built-ins still present
            assertTrue(reg.isIgnorable(new Parameter<>(ImageMosaicFormat.ALLOW_MULTITHREADING)));
        } finally {
            System.clearProperty(IgnorableParameterRegistry.SYSTEM_PROPERTY);
        }
    }

    private static Parameter<String> param(String name) {
        return new Parameter<>(new DefaultParameterDescriptor<>(name, String.class, null, null));
    }
}

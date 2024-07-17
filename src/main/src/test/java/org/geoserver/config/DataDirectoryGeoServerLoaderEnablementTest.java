/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.geoserver.config.DataDirectoryGeoServerLoader.ENABLED_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.Environment;

public class DataDirectoryGeoServerLoaderEnablementTest {

    @Before
    public void setup() {
        GeoServerExtensionsHelper.setIsSpringContext(false);
    }

    @After
    public void destroyAppContext() {
        GeoServerExtensionsHelper.init(null);
    }

    @AfterClass
    public static void clearContexAfterAll() {
        GeoServerExtensionsHelper.init(null);
    }

    @Test
    public void testEnabledByDefault() {
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testEnabled() {
        GeoServerExtensionsHelper.property(ENABLED_PROPERTY, "true");
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testDisabledWithSystemProperty() {
        GeoServerExtensionsHelper.property(ENABLED_PROPERTY, "false");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    /**
     * Check the config property {@literal datadir.loader.enabled} also works when set up as an
     * environment variable in upper-case with underscores format ({@literal
     * DATADIR_LOADER_ENABLED}. This is more commonly used in Spring Boot environments, though it
     * works the same in regular Spring applications as long as {@code
     * Environment#getProperty(String)} is called.
     */
    @Test
    public void testDisabledWithEnvVariable() throws Exception {
        Class<?> classOfMap = System.getenv().getClass();
        Field field = FieldUtils.getDeclaredField(classOfMap, "m", true);
        @SuppressWarnings("unchecked")
        Map<String, String> writeableEnvironmentVariables =
                (Map<String, String>) field.get(System.getenv());

        writeableEnvironmentVariables.put("DATADIR_LOADER_ENABLED", "false");
        assertEquals("false", System.getenv().get("DATADIR_LOADER_ENABLED"));
        try (StaticApplicationContext context = new StaticApplicationContext()) {
            assertFalse(DataDirectoryGeoServerLoader.isEnabled(context));
        } finally {
            writeableEnvironmentVariables.remove("DATADIR_LOADER_ENABLED");
        }
    }

    @Test
    public void testDisabledWithAppContextEnvironment() {
        ApplicationContext appContext = mock(ApplicationContext.class);
        Environment env = mock(Environment.class);
        when(appContext.getEnvironment()).thenReturn(env);
        when(env.getProperty(ENABLED_PROPERTY)).thenReturn("false");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(appContext));
    }
}

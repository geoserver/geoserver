/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.geoserver.config.DataDirectoryGeoServerLoader.ENABLED_PROPERTY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
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

    @Test
    public void testDisabledWithEnvVariable() {
        // Simulate having set up the config property as an environment variable picked up by
        // GeoServerExtensions.getProperty()
        String envVarKey = "DATADIR_LOADER_ENABLED";
        GeoServerExtensionsHelper.property(envVarKey, "false");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(null));
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

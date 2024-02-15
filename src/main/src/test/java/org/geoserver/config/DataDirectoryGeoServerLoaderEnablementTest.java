/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.geoserver.config.DataDirectoryGeoServerLoader.ENVVAR_KEY;
import static org.geoserver.config.DataDirectoryGeoServerLoader.SYSPROP_KEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
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

    @Test
    public void testEnabledByDefault() {
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testEnabled() {
        GeoServerExtensionsHelper.property(SYSPROP_KEY, "true");
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testDisabledWithSystemProperty() {
        GeoServerExtensionsHelper.property(SYSPROP_KEY, "false");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testDisabledWithEnvVariable() {
        GeoServerExtensionsHelper.property(ENVVAR_KEY, "false");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testDisabledWithAppContextEnvironment() {
        ApplicationContext appContext = mock(ApplicationContext.class);
        Environment env = mock(Environment.class);
        when(appContext.getEnvironment()).thenReturn(env);
        when(env.getProperty(ENVVAR_KEY)).thenReturn("false");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(appContext));
    }
}

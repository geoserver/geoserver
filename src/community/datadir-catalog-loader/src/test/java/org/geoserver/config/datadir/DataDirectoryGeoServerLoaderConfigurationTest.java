/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static org.geoserver.config.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_ENABLED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;
import org.geoserver.config.DataDirectoryGeoServerLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.springframework.web.context.WebApplicationContext;

public class DataDirectoryGeoServerLoaderConfigurationTest {

    /** Allows to set environment variables for each individual test */
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setUp() {
        // preflight: no sys prop nor env var set
        assertNull(System.getProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED));
        assertNull(System.getenv(GEOSERVER_DATA_DIR_LOADER_ENABLED));
    }

    @After
    public void cleanUp() {
        System.clearProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED);
    }

    @Test
    public void testEnabledByDefault() {
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testEnabledWithSystemProperty() {
        System.setProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED, "true");
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testDisabledWithSystemProperty() {
        System.setProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED, "false");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testEnvironmentVariable() {
        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_ENABLED, "false");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(null));

        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_ENABLED, "non-true");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(null));

        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_ENABLED, "");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(null));

        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_ENABLED, "true");
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(null));

        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_ENABLED, "TRUE");
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(null));
    }

    @Test
    public void testServletContext() {
        WebApplicationContext appContext = mock(WebApplicationContext.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(appContext.getServletContext()).thenReturn(servletContext);

        when(servletContext.getInitParameter(GEOSERVER_DATA_DIR_LOADER_ENABLED)).thenReturn("false");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(appContext));

        when(servletContext.getInitParameter(GEOSERVER_DATA_DIR_LOADER_ENABLED)).thenReturn("non-true");
        assertFalse(DataDirectoryGeoServerLoader.isEnabled(appContext));

        when(servletContext.getInitParameter(GEOSERVER_DATA_DIR_LOADER_ENABLED)).thenReturn("true");
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(appContext));

        when(servletContext.getInitParameter(GEOSERVER_DATA_DIR_LOADER_ENABLED)).thenReturn("TRUE");
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(appContext));
    }
}

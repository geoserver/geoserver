/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static org.geoserver.config.datadir.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_ENABLED;
import static org.geoserver.config.datadir.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_THREADS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletContext;
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
        // clean up also before in case the env variables are set when running the tests (e.g. during development)
        cleanUp();
    }

    @After
    public void tearDown() {
        System.clearProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED);
        System.clearProperty(GEOSERVER_DATA_DIR_LOADER_THREADS);
        environmentVariables.clear(GEOSERVER_DATA_DIR_LOADER_ENABLED, GEOSERVER_DATA_DIR_LOADER_THREADS);
    }

    private void cleanUp() {
        environmentVariables.clear(GEOSERVER_DATA_DIR_LOADER_ENABLED, GEOSERVER_DATA_DIR_LOADER_THREADS);
        System.clearProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED);
        System.clearProperty(GEOSERVER_DATA_DIR_LOADER_THREADS);
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

    @Test
    public void testDetermineParallelismDefault() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int expected = Math.min(availableProcessors, 16);
        assertEquals(expected, ExecutorFactory.determineParallelism());
    }

    @Test
    public void testDetermineParallelismBadArgument() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int expected = Math.min(availableProcessors, 16);

        System.setProperty(GEOSERVER_DATA_DIR_LOADER_THREADS, "N.a.N.");
        assertEquals(expected, ExecutorFactory.determineParallelism());

        System.setProperty(GEOSERVER_DATA_DIR_LOADER_THREADS, "0");
        assertEquals(expected, ExecutorFactory.determineParallelism());

        System.clearProperty(GEOSERVER_DATA_DIR_LOADER_THREADS);

        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_THREADS, "not an int");
        assertEquals(expected, ExecutorFactory.determineParallelism());

        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_THREADS, "0");
        assertEquals(expected, ExecutorFactory.determineParallelism());
    }

    @Test
    public void testDetermineParallelism() {
        final int max = Runtime.getRuntime().availableProcessors();

        System.setProperty(GEOSERVER_DATA_DIR_LOADER_THREADS, "1");
        assertEquals(1, ExecutorFactory.determineParallelism());

        System.setProperty(GEOSERVER_DATA_DIR_LOADER_THREADS, String.valueOf(max));
        assertEquals(max, ExecutorFactory.determineParallelism());

        System.clearProperty(GEOSERVER_DATA_DIR_LOADER_THREADS);

        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_THREADS, "1");
        assertEquals(1, ExecutorFactory.determineParallelism());

        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_THREADS, String.valueOf(max));
        assertEquals(max, ExecutorFactory.determineParallelism());
    }

    @Test
    public void testDetermineParallellismTooMany() {
        final int max = Runtime.getRuntime().availableProcessors();
        final int tooMany = 1 + max;

        System.setProperty(GEOSERVER_DATA_DIR_LOADER_THREADS, String.valueOf(tooMany));
        assertEquals(max, ExecutorFactory.determineParallelism());
    }
}

/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static org.geoserver.config.datadir.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_ENABLED;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.datadir.config.DataDirectoryGeoServerLoaderConfiguration;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class DataDirectoryGeoServerLoaderConfigurationTest {

    /** Allows to set environment variables for each individual test */
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    public static final @ClassRule TemporaryFolder tmp = new TemporaryFolder();

    @Configuration(proxyBeanMethods = true)
    static class MockDependenciesConfiguration {

        @Bean
        public ResourceStore resourceStoreImpl() {
            return new FileSystemResourceStore(tmp.getRoot());
        }

        @Bean
        public GeoServerResourceLoader geoServerResourceLoader() {
            ResourceStore store = resourceStoreImpl();
            return new GeoServerResourceLoader(store);
        }

        @Bean
        public GeoServerDataDirectory geoServerDataDirectory() {
            return new GeoServerDataDirectory(geoServerResourceLoader());
        }

        @Bean
        public GeoServerSecurityManager geoServerSecurityManager() {
            return Mockito.mock(GeoServerSecurityManager.class);
        }

        @Bean
        public XStreamPersisterFactory xStreamPersisterFactory() {
            return new XStreamPersisterFactory();
        }
    }

    private ApplicationContext lodAppContext() {
        return new AnnotationConfigApplicationContext(
                MockDependenciesConfiguration.class, DataDirectoryGeoServerLoaderConfiguration.class);
    }

    public @Before void setup() {
        // preflight: no sys prop nor env var set
        assertNull(System.getProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED));
        assertNull(System.getenv(GEOSERVER_DATA_DIR_LOADER_ENABLED));
    }

    @After
    public void cleanup() {
        System.clearProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED);
    }

    @Test
    public void testEnabledByDefault() {
        assertEnabled();
    }

    @Test
    public void testEnabledWithSystemProperty() {
        System.setProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED, "true");
        assertEnabled();
    }

    @Test
    public void testDisabledWithSystemProperty() {
        System.setProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED, "false");
        ApplicationContext context = lodAppContext();
        assertFalse(context.containsBean("dataDirectoryGeoServerLoader"));
    }

    @Test
    public void testDisabledExplicitylyWithEnvVariable() {
        environmentVariables.set(GEOSERVER_DATA_DIR_LOADER_ENABLED, "false");

        ApplicationContext context = lodAppContext();
        assertFalse(context.containsBean("dataDirectoryGeoServerLoader"));
    }

    private void assertEnabled() {
        ApplicationContext context = lodAppContext();
        assertTrue(context.containsBean("dataDirectoryGeoServerLoader"));
        assertThat(context.getBean("dataDirectoryGeoServerLoader"), instanceOf(DataDirectoryGeoServerLoader.class));
    }
}

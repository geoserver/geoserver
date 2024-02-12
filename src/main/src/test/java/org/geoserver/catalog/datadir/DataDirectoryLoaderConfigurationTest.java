/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir;

import static org.geoserver.catalog.datadir.config.DataDirectoryLoaderConfiguration.DataDirLoaderEnabledCondition.ENVVAR_KEY;
import static org.geoserver.catalog.datadir.config.DataDirectoryLoaderConfiguration.DataDirLoaderEnabledCondition.SYSPROP_KEY;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import org.geoserver.catalog.datadir.config.DataDirectoryLoaderConfiguration;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

public class DataDirectoryLoaderConfigurationTest {

    public static @ClassRule TemporaryFolder tmp = new TemporaryFolder();

    /** Not marked as @Configuration so it doesn't get component-scanned */
    static class MockDependenciesConfiguration {

        @Bean
        FileSystemResourceStore resourceStore() throws IOException {
            return new FileSystemResourceStore(tmp.getRoot());
        }

        @Bean
        GeoServerResourceLoader geoServerResourceLoader() throws IOException {
            FileSystemResourceStore store = resourceStore();
            return new GeoServerResourceLoader(store);
        }

        @Bean
        GeoServerSecurityManager geoServerSecurityManager() {
            return Mockito.mock(GeoServerSecurityManager.class);
        }

        @Bean
        XStreamPersisterFactory xStreamPersisterFactory() {
            return new XStreamPersisterFactory();
        }
    }

    private ApplicationContext lodAppContext() {
        return new AnnotationConfigApplicationContext(
                MockDependenciesConfiguration.class, DataDirectoryLoaderConfiguration.class);
    }

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
        assertEnabled();
    }

    @Test
    public void testEnabled() {
        GeoServerExtensionsHelper.property(SYSPROP_KEY, "true");
        assertEnabled();
    }

    private void assertEnabled() {
        ApplicationContext context = lodAppContext();
        assertThat(
                context.getBean("dataDirectoryGeoServerLoader"),
                instanceOf(DataDirectoryGeoServerLoader.class));
    }

    @Test
    public void testDisabledExplicitylyWithSystemProperty() {
        GeoServerExtensionsHelper.property(SYSPROP_KEY, "false");
        ApplicationContext context = lodAppContext();
        assertFalse(context.containsBean("dataDirectoryGeoServerLoader"));
    }

    @Test
    public void testDisabledExplicitylyWithEnvVariable() throws Exception {
        GeoServerExtensionsHelper.property(ENVVAR_KEY, "false");
        ApplicationContext context = lodAppContext();
        assertFalse(context.containsBean("dataDirectoryGeoServerLoader"));
    }
}

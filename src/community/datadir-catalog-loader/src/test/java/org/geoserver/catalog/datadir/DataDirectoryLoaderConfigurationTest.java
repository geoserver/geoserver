/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.Collections;
import java.util.Map;
import org.geoserver.catalog.datadir.config.DataDirectoryLoaderConfiguration;
import org.geoserver.catalog.datadir.config.DataDirectoryLoaderConfiguration.DataDirLoaderEnabledCondition;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class DataDirectoryLoaderConfigurationTest {

    public static @ClassRule TemporaryFolder tmp = new TemporaryFolder();

    @Configuration(proxyBeanMethods = true)
    static class MockDependenciesConfiguration {

        public @Bean FileSystemResourceStore resourceStore() throws IOException {
            return new FileSystemResourceStore(tmp.getRoot());
        }

        public @Bean GeoServerResourceLoader geoServerResourceLoader() throws IOException {
            FileSystemResourceStore store = resourceStore();
            return new GeoServerResourceLoader(store);
        }

        public @Bean GeoServerSecurityManager geoServerSecurityManager() {
            return Mockito.mock(GeoServerSecurityManager.class);
        }
    }

    private ApplicationContext lodAppContext() {
        return new AnnotationConfigApplicationContext(
                MockDependenciesConfiguration.class, DataDirectoryLoaderConfiguration.class);
    }

    public @Before void setup() {
        System.clearProperty(DataDirLoaderEnabledCondition.KEY);
    }

    @Test
    public void testEnabledByDefault() {
        assertNull(System.getProperty(DataDirLoaderEnabledCondition.KEY));
        assertNull(System.getenv("DATADIR_LOADER_ENABLED"));
        assertEnabled();
    }

    @Test
    public void testEnabled() {
        System.setProperty(DataDirLoaderEnabledCondition.KEY, "true");
        assertEnabled();
    }

    private void assertEnabled() {
        ApplicationContext context = lodAppContext();
        assertTrue(context.containsBean("dataDirectoryGeoServerLoader"));
        assertThat(
                context.getBean("dataDirectoryGeoServerLoader"),
                instanceOf(DataDirectoryGeoServerLoader.class));
    }

    @Test
    public void testDisabledExplicitylyWithSystemProperty() {
        System.setProperty(DataDirLoaderEnabledCondition.KEY, "false");
        ApplicationContext context = lodAppContext();
        assertFalse(context.containsBean("dataDirectoryGeoServerLoader"));
    }

    @Test
    public void testDisabledExplicitylyWithEnvVariable() throws Exception {
        try {
            setEnv(Map.of("DATADIR_LOADER_ENABLED", "false"));
        } catch (InaccessibleObjectException e) {
            // running on Java 17
            return;
        }
        assertNull(System.getProperty(DataDirLoaderEnabledCondition.KEY));
        assertEquals("false", System.getenv("DATADIR_LOADER_ENABLED"));
        ApplicationContext context = lodAppContext();
        assertFalse(context.containsBean("dataDirectoryGeoServerLoader"));
    }

    // got it from
    // https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
    @SuppressWarnings("unchecked")
    protected static void setEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> envClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = envClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField =
                    envClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv =
                    (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }
}

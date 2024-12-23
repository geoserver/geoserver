/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.easymock.EasyMock;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

public class SystemEnvironmentTest {

    /** logger */
    protected static final Logger LOGGER = Logging.getLogger("org.geoserver.platform");

    @Test
    public void testSystemPropertiesStatus() {
        String key = System.getenv().keySet().iterator().next();
        String value = System.getenv(key);

        SystemEnvironmentStatus status = new SystemEnvironmentStatus() {
            @Override
            String getEnvironmentVariable(String envVar) {
                return "true";
            }
        };

        assertTrue(status.getMessage().isPresent());
        assertTrue(status.getMessage().get().contains(key));
        assertTrue(status.getMessage().get().contains(value));
    }

    /**
     * Tests the SystemEnvironmentStatusEnabledEnvironmentVar so it turns on/off the message (list of environment vars).
     */
    @Test
    public void testEnabled() {
        final var VALUE = new ArrayList<String>();

        // create subclass of SystemEnvironmentStatus so we can change the value of the environment
        // variable.
        // VALUE empty -> null
        // otherwise its the first item in the VALUE
        // if the request is for a different environment var -> throw
        SystemEnvironmentStatus status = new SystemEnvironmentStatus() {
            @Override
            String getEnvironmentVariable(String envVar) {
                if (envVar.equals(SystemEnvironmentStatus.SystemEnvironmentStatusEnabledEnvironmentVar)) {
                    if (VALUE.isEmpty()) {
                        return null;
                    }
                    return VALUE.get(0);
                }
                throw new RuntimeException("bad var");
            }
        };

        VALUE.clear();
        VALUE.add("true");
        assertTrue(status.isShow());
        assertFalse(status.getMessage().isEmpty());

        VALUE.clear();
        VALUE.add("TRUE");
        assertTrue(status.isShow());
        assertFalse(status.getMessage().isEmpty());

        VALUE.clear();
        VALUE.add("FALSE");
        assertFalse(status.isShow());
        assertTrue(status.getMessage().get().startsWith("Environment variables hidden for security reasons."));

        VALUE.clear();
        VALUE.add("false");
        assertFalse(status.isShow());
        assertTrue(status.getMessage().get().startsWith("Environment variables hidden for security reasons."));

        // default -> false
        VALUE.clear();
        assertFalse(status.isShow());
        assertTrue(status.getMessage().get().startsWith("Environment variables hidden for security reasons."));

        // bad value -> false
        VALUE.clear();
        VALUE.add("maybe");
        assertFalse(status.isShow());
        assertTrue(status.getMessage().get().startsWith("Environment variables hidden for security reasons."));
    }

    @Test
    public void testGeoServerEnvironmentDefaultValue() {
        System.clearProperty("ALLOW_ENV_PARAMETRIZATION");
        String sysProperty = System.getProperty("ALLOW_ENV_PARAMETRIZATION");

        GeoServerResourceLoader loader = EasyMock.createMockBuilder(GeoServerResourceLoader.class)
                .withConstructor()
                .createMock();

        ApplicationContext appContext = EasyMock.createMock(ApplicationContext.class);
        EasyMock.expect(appContext.getBeanNamesForType(ExtensionFilter.class))
                .andReturn(new String[] {})
                .anyTimes();
        EasyMock.expect(appContext.getBeanNamesForType(ExtensionProvider.class))
                .andReturn(new String[] {})
                .anyTimes();
        EasyMock.expect(appContext.getBeanNamesForType(GeoServerResourceLoader.class))
                .andReturn(new String[] {"geoServerLoader"})
                .anyTimes();
        Map<String, GeoServerResourceLoader> genvMap = new HashMap<>();
        genvMap.put("geoServerLoader", loader);
        EasyMock.expect(appContext.getBeansOfType(GeoServerResourceLoader.class))
                .andReturn(genvMap)
                .anyTimes();
        EasyMock.expect(appContext.getBean("geoServerLoader")).andReturn(loader).anyTimes();
        EasyMock.expect(appContext.isSingleton("geoServerLoader"))
                .andReturn(true)
                .anyTimes();

        EasyMock.replay(appContext);
        GeoServerExtensions gsext = new GeoServerExtensions();
        gsext.setApplicationContext(appContext);

        // By default ALLOW_ENV_PARAMETRIZATION flag is set to FALSE
        // LOGGER.info("ALLOW_ENV_PARAMETRIZATION = " + sysProperty);
        if (sysProperty == null || !Boolean.valueOf(sysProperty)) {
            // instantiation has side effects
            new GeoServerEnvironment();
            // LOGGER.info("GeoServerEnvironment = " +
            // GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION);
            assertFalse(GeoServerEnvironment.allowEnvParametrization());
        }
    }
}

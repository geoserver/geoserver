/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import static org.junit.Assert.assertTrue;

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

        SystemEnvironmentStatus status = new SystemEnvironmentStatus();
        assertTrue(status.getMessage().isPresent());
        assertTrue(status.getMessage().get().contains(key));
        assertTrue(status.getMessage().get().contains(value));
    }

    @Test
    public void testGeoServerEnvironmentDefaultValue() {
        System.clearProperty("ALLOW_ENV_PARAMETRIZATION");
        String sysProperty = System.getProperty("ALLOW_ENV_PARAMETRIZATION");

        GeoServerResourceLoader loader =
                EasyMock.createMockBuilder(GeoServerResourceLoader.class)
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
        EasyMock.expect(appContext.isSingleton("geoServerLoader")).andReturn(true).anyTimes();

        EasyMock.replay(appContext);
        GeoServerExtensions gsext = new GeoServerExtensions();
        gsext.setApplicationContext(appContext);

        // By default ALLOW_ENV_PARAMETRIZATION flag is set to FALSE
        // LOGGER.info("ALLOW_ENV_PARAMETRIZATION = " + sysProperty);
        if (sysProperty == null || !Boolean.valueOf(sysProperty)) {
            GeoServerEnvironment genv = new GeoServerEnvironment();
            // LOGGER.info("GeoServerEnvironment = " +
            // GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION);
            assertTrue(!GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION);
        }
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.geotools.util.logging.Logging;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * Unit test suite for {@link GeoServerEnvironment}
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class GeoServerEnvironmentTest extends TestCase {

    /** logger */
    protected static final Logger LOGGER = Logging.getLogger("org.geoserver.platform");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("TEST_SYS_PROPERTY", "ABC");
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "false");

        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        System.setProperty("GEOSERVER_REQUIRE_FILE", "pom.xml");
        try {
            Assert.assertEquals(
                    "data", GeoServerResourceLoader.lookupGeoServerDataDirectory(context));
        } finally {
            System.clearProperty("GEOSERVER_REQUIRE_FILE");
        }

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
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.clearProperty("TEST_SYS_PROPERTY");
        System.clearProperty("ALLOW_ENV_PARAMETRIZATION");
    }

    @Test
    public void testSystemProperty() {
        // check for a property we did set up in the setUp
        GeoServerEnvironment genv = new GeoServerEnvironment();
        LOGGER.info("GeoServerEnvironment = " + GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION);

        assertEquals("ABC", genv.resolveValue("${TEST_SYS_PROPERTY}"));
        assertEquals("${TEST_PROPERTY}", genv.resolveValue("${TEST_PROPERTY}"));
    }
}

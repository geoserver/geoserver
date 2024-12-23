/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Properties;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PrintingServletWrappingControllerTest {

    private PrintingServletWrappingController controller;

    private GeoServerResourceLoader loader;
    private Resource resource;
    File mockFile = new File("/tmp/test-print-config/config.yaml");

    @Before
    public void setUp() throws Exception {
        // Set the system property without a leading slash so the path matches our mock
        System.setProperty("GEOSERVER_PRINT_CONFIG_DIR", "tmp/test-print-config");

        loader = Mockito.mock(GeoServerResourceLoader.class);
        resource = Mockito.mock(Resource.class);

        // The controller will request: "tmp/test-print-config/config.yaml"
        when(loader.get("tmp/test-print-config/config.yaml")).thenReturn(resource);

        // Simulate a file resource
        when(resource.getType()).thenReturn(Resource.Type.RESOURCE);

        // The code tries to create a default config if it doesn't exist
        when(resource.out()).thenReturn(new ByteArrayOutputStream());

        // When checking canRead, Resources will call resource.in()
        when(resource.in()).thenReturn(new ByteArrayInputStream(new byte[0]));

        // Mock a file-backed resource
        when(resource.getType()).thenReturn(Resource.Type.RESOURCE);
        when(resource.file()).thenReturn(mockFile);

        // Initialize GeoServerExtensionsHelper so that GeoServerExtensions finds our mock loader
        GeoServerExtensionsHelper.clear();
        GeoServerExtensionsHelper.init(null);
        GeoServerExtensionsHelper.singleton("geoserverResourceLoader", loader, GeoServerResourceLoader.class);

        controller = new PrintingServletWrappingController();
    }

    @After
    public void tearDown() {
        System.clearProperty("GEOSERVER_PRINT_CONFIG_DIR");
        GeoServerExtensionsHelper.clear();
    }

    @Test
    public void testSetInitParametersFromSystemProperty() throws Exception {
        Properties initParameters = new Properties();
        initParameters.setProperty("config", "config.yaml");

        controller.setInitParameters(initParameters);

        String updatedConfig = initParameters.getProperty("config");
        assertEquals("Expected the config path to match our mocked file", updatedConfig, mockFile.getAbsolutePath());
    }
}

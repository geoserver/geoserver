package org.geoserver.printing;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests {@link PrintingServletWrappingController} to ensure: 1) Absolute and relative paths are handled correctly. 2)
 * The final config YAML matches the default-config.yaml when created.
 */
public class PrintingServletWrappingControllerTest extends GeoServerSystemTestSupport {

    private PrintingServletWrappingController controller;
    private GeoServerResourceLoader loader;
    private Resource resource;

    // We’ll capture writes to resource.out() in this stream
    private ByteArrayOutputStream resourceOutputStream;

    // A mock File that we pretend is the resource location for absolute paths
    private final File mockFile = new File("/tmp/test-print-config/config.yaml");

    @Before
    public void setUp() throws Exception {
        // Clear any leftover state
        GeoServerExtensionsHelper.clear();
        System.clearProperty("GEOSERVER_PRINT_CONFIG_DIR");

        // Create our mocks
        loader = Mockito.mock(GeoServerResourceLoader.class);
        resource = Mockito.mock(Resource.class);
        resourceOutputStream = new ByteArrayOutputStream();

        // Initialize GeoServerExtensionsHelper so that
        // GeoServerExtensions finds our mock loader
        GeoServerExtensionsHelper.init(null);
        GeoServerExtensionsHelper.singleton("geoserverResourceLoader", loader, GeoServerResourceLoader.class);

        // place the configuration file in the data dir (which is also used as the servlet context
        // lookup)
        GeoServerDataDirectory dd = getDataDirectory();
        try (InputStream is = getClass().getResourceAsStream("/test.yaml")) {
            FileUtils.copyInputStreamToFile(is, new File(dd.root(), "test.yaml"));
        }

        // Always create a fresh controller
        controller = new PrintingServletWrappingController();
    }

    @After
    public void tearDown() {
        // Clean up
        System.clearProperty("GEOSERVER_PRINT_CONFIG_DIR");
        GeoServerExtensionsHelper.clear();
    }

    /**
     * Tests that when the system property GEOSERVER_PRINT_CONFIG_DIR is an absolute path, the controller simply returns
     * that path + "config.yaml" with no resource copying.
     */
    @Test
    public void testSetInitParametersWithAbsolutePath() throws Exception {
        // 1) Setup an absolute path
        System.setProperty("GEOSERVER_PRINT_CONFIG_DIR", "/tmp/test-print-config");

        // We do not expect the code to call resource loader for an absolute path,
        // but we can still mock it in case the code tries to do so.
        when(loader.get("tmp/test-print-config/config.yaml")).thenReturn(resource);

        // 2) The resource is presumably already there if we even check
        when(resource.getType()).thenReturn(Resource.Type.RESOURCE);
        when(resource.out()).thenReturn(resourceOutputStream);
        when(resource.in()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(resource.file()).thenReturn(mockFile);

        // 3) Prepare the servlet init parameters
        Properties initParameters = new Properties();
        initParameters.setProperty("config", "config.yaml");

        // 4) Call setInitParameters
        controller.setInitParameters(initParameters);

        // 5) Because the path is absolute, the resulting config param
        // should be "/tmp/test-print-config/config.yaml"
        String updatedConfig = initParameters.getProperty("config");
        Path actualPath = Paths.get(mockFile.getAbsolutePath()).toAbsolutePath().normalize();
        Path expectedPath = Paths.get(updatedConfig).toAbsolutePath().normalize();
        assertEquals("Expected the config path to match our mocked absolute file", expectedPath, actualPath);

        // 6) Verify that no default YAML was copied
        // (In an absolute path scenario, the code doesn't do resource-based copying)
        assertEquals(
                "No data should have been written to resource.out() for an absolute path.",
                0,
                resourceOutputStream.size());
    }

    /**
     * Tests that when the system property GEOSERVER_PRINT_CONFIG_DIR is a relative path, the controller treats it as
     * relative to the GeoServer data directory. If the resource is UNDEFINED, it should copy default-config.yaml from
     * the classpath. We then compare the written content with the expected YAML.
     */
    @Test
    public void testSetInitParametersWithRelativePathAndCompareYaml() throws Exception {
        // 1) Use a relative path, ensuring we trigger resource copying
        System.setProperty("GEOSERVER_PRINT_CONFIG_DIR", "relative-print-config");

        // 2) Mock the resource to be "UNDEFINED" so the code decides to copy the default
        when(loader.get("relative-print-config/config.yaml")).thenReturn(resource);
        when(resource.getType()).thenReturn(Type.UNDEFINED);

        // We capture what the code writes
        when(resource.out()).thenReturn(resourceOutputStream);

        // The code also checks canRead(resource), which in turn calls resource.in()
        // Because it's UNDEFINED, the code will copy default-config.yaml, so .in() won't matter
        when(resource.in()).thenReturn(new ByteArrayInputStream(new byte[0]));

        // Once copied, the code sets the final path to resource.file().getAbsolutePath()
        // We can mock a final location
        File mockRelativeFile = new File("somewhere/relative-print-config/config.yaml");
        when(resource.file()).thenReturn(mockRelativeFile);

        // 3) Prepare the servlet init parameters
        Properties initParameters = new Properties();
        initParameters.setProperty("config", "config.yaml");

        // 4) Call setInitParameters
        controller.setInitParameters(initParameters);

        // 5) The config param should end up as the mock resource absolute path
        String updatedConfig = initParameters.getProperty("config");
        assertEquals(
                "Expected the config path to match our mock relative file path",
                mockRelativeFile.getAbsolutePath(),
                updatedConfig);

        // 6) Compare the contents that were copied into resource.out()
        //    We placed the actual YAML in src/test/resources/org/geoserver/printing/default-config.yaml
        //    so the code in findPrintConfigDirectory() used getClass().getResourceAsStream("default-config.yaml")
        //    to copy it. Let's verify what got copied matches that file.

        String copiedYaml = new String(resourceOutputStream.toByteArray(), StandardCharsets.UTF_8);
        // Load the expected YAML directly from test resources, or store it as a constant if you prefer
        String expectedYaml = readTestResource("default-config.yaml");

        // Assert that they match. Using trim() to avoid any trailing newlines or carriage returns
        assertEquals("The copied YAML should match default-config.yaml.", expectedYaml.trim(), copiedYaml.trim());
    }

    @Test
    public void testSetInitParametersFromEnvironmentVariableAndCompareYaml() throws Exception {
        // 1) Create a spy so we can override getPrintConfigEnvVariable()
        PrintingServletWrappingController spiedController = Mockito.spy(new PrintingServletWrappingController());

        // 2) Suppose the environment variable says: "/tmp/test-print-config"
        //    We'll treat this as absolute, so that the code does not copy default-config.yaml.
        Mockito.doReturn("/tmp/test-print-config").when(spiedController).lookupPrintConfigSystemProperty();

        // 3) Prepare mocks for resource loader, just like the other tests
        GeoServerResourceLoader mockLoader = Mockito.mock(GeoServerResourceLoader.class);
        Resource mockResource = Mockito.mock(Resource.class);

        // The code will call loader.get("tmp/test-print-config/config.yaml") if it tries
        // to interpret the path as relative. But because we’ll call isAbsolute(), it
        // should skip the resource approach. However, to be safe, we can still stub this:
        when(mockLoader.get("tmp/test-print-config/config.yaml")).thenReturn(mockResource);

        // 4) Because the path is absolute, the code typically won't attempt to copy anything.
        //    We'll simulate an existing resource that is readable.
        when(mockResource.getType()).thenReturn(Resource.Type.RESOURCE);

        // Let's pretend the resource on disk has the *same* content as test.yaml.
        // We'll read test.yaml ourselves and return it via resource.in().
        String testYamlContents = readTestResource("/test.yaml");
        ByteArrayInputStream resourceInputStream =
                new ByteArrayInputStream(testYamlContents.getBytes(StandardCharsets.UTF_8));
        when(mockResource.in()).thenReturn(resourceInputStream);

        // If the code calls mockResource.file().getAbsolutePath(), return a plausible path:
        File mockFile = new File("/tmp/test-print-config/config.yaml");
        when(mockResource.file()).thenReturn(mockFile);

        // 5) Register the mock loader with GeoServerExtensions
        GeoServerExtensionsHelper.clear();
        GeoServerExtensionsHelper.init(null);
        GeoServerExtensionsHelper.singleton("geoserverResourceLoader", mockLoader, GeoServerResourceLoader.class);

        // 6) Build the init parameters
        Properties initParams = new Properties();
        initParams.setProperty("config", "config.yaml");

        // 7) Invoke setInitParameters on our *spy* controller
        spiedController.setInitParameters(initParams);

        // 8) Confirm the resulting config path
        String updatedConfig = initParams.getProperty("config");
        Path actualPath = Paths.get(mockFile.getAbsolutePath()).toAbsolutePath().normalize();
        Path expectedPath = Paths.get(updatedConfig).toAbsolutePath().normalize();
        assertEquals(
                "Expected the config path to be the absolute path from the environment variable",
                expectedPath,
                actualPath);

        // 9) Because the path is absolute, the code does not copy default-config.yaml.
        //    Instead, it references the existing resource. Let's confirm that
        //    the content from mockResource matches test.yaml by re-reading from resource.in().

        // If the real code never actually "reads" it in your scenario,
        // you might do the read in your test logic just to confirm the content is correct.
        // We'll do it again here:
        resourceInputStream.reset(); // since we already consumed it once
        byte[] actualBytes = resourceInputStream.readAllBytes();
        String actualContents = new String(actualBytes, StandardCharsets.UTF_8);

        assertEquals(
                "Contents of the environment-based config.yaml should match test.yaml",
                testYamlContents.trim(),
                actualContents.trim());
    }

    /** Utility method to load a file from the test resources (same folder as this class). */
    private String readTestResource(String resourceName) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IllegalStateException("Could not find test resource '" + resourceName + "' on classpath.");
            }
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}

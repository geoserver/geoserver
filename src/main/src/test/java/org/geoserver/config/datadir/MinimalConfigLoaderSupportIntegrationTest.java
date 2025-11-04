/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.awaitility.Awaitility;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.datadir.DataDirectoryLoaderTestSupport.TestService1;
import org.geoserver.config.datadir.DataDirectoryLoaderTestSupport.TestService1.TestService1Impl;
import org.geoserver.config.datadir.DataDirectoryLoaderTestSupport.TestService2;
import org.geoserver.config.datadir.DataDirectoryLoaderTestSupport.TestService2.TestService2Impl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.MemoryLockProvider;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Integration tests for {@link MinimalConfigLoaderSupport} that validate the thread-safety and consistency mechanisms
 * across restarts or when multiple GeoServer instances start simultaneously and share the same data directory.
 *
 * <p>These tests focus specifically on scenarios where:
 *
 * <ul>
 *   <li>Multiple instances attempt to initialize an empty or incomplete data directory
 *   <li>Instances need to coordinate using file locks to prevent race conditions
 *   <li>Missing configuration elements need to be created consistently across instances
 *   <li>Authentication needs to be properly propagated to worker threads
 * </ul>
 *
 * <p>The tests use {@link XmlWebApplicationContext} directly instead of {@link GeoServerSystemTestSupport} to have
 * precise control over the initialization sequence and avoid side effects from the test support class. This allows
 * testing edge cases around empty data directories and concurrent initialization that would otherwise be difficult to
 * simulate.
 */
public class MinimalConfigLoaderSupportIntegrationTest {

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private File dataDirectory;

    private DataDirectoryLoaderTestSupport support;

    @Before
    public void setUp() throws IOException {
        dataDirectory = temp.newFolder("datadir");
        support = DataDirectoryLoaderTestSupport.withPersistence(dataDirectory);
        System.setProperty("GEOSERVER_DATA_DIR", dataDirectory.getAbsolutePath());
        System.setProperty("GEOSERVER_DATA_DIR_LOADER_ENABLED", "true");
    }

    @After
    public void tearDown() {
        System.clearProperty("GEOSERVER_DATA_DIR");
        System.clearProperty("GEOSERVER_DATA_DIR_LOADER_ENABLED");
        support.tearDown();
    }

    @Test
    public void testEmptyGlobalXml() throws IOException {
        Files.createFile(dataDirectory.toPath().resolve("global.xml"));
        assertThrows(BeanCreationException.class, this::initContext);
    }

    /** Tests that when no missing configuration, initializeEmptyConfig() exits early */
    @Test
    public void testNoMissingConfigs() throws IOException {
        // Pre-setup complete configuration
        GeoServerInfo global = support.getGeoServer().getFactory().createGlobal();
        support.setUpServiceLoaders();
        global.getSettings().setTitle("pre-existing");

        LoggingInfo logging = support.getGeoServer().getFactory().createLogging();
        logging.setLocation("logs/gs2.log");

        TestService1Impl service1 = new TestService1Impl();
        service1.setTitle("pre-existing service 1");

        TestService2Impl service2 = new TestService2Impl();
        service2.setTitle("pre-existing service 2");

        support.getGeoServer().setGlobal(global);
        support.getGeoServer().setLogging(logging);
        support.getGeoServer().add(service1);
        support.getGeoServer().add(service2);
        support.cleanUp();

        // Get hold of the InitialConfigLoaderSupport through context initialization
        ApplicationContext context = initContext();

        // Verify all configs are present (directly test isMissingDefaultConfigs
        // returning false)
        // This is an implicit verification since if configs were missing,
        // the test would have side effects visible in other tests
        GeoServer gs = context.getBean(GeoServer.class);
        assertEquals(global, gs.getGlobal());
        assertEquals(logging, gs.getLogging());
        assertEquals(service1, ModificationProxy.unwrap(gs.getService(TestService1.class)));
        assertEquals(service2, ModificationProxy.unwrap(gs.getService(TestService2.class)));
    }

    /** Tests that missing global config is created */
    @Test
    public void testAddMissingGlobalConfig() {
        // Setup existing logging and services but missing global
        support.getGeoServer().setLogging(support.getGeoServer().getFactory().createLogging());
        support.getGeoServer().add(new TestService1Impl());
        support.getGeoServer().add(new TestService2Impl());
        support.cleanUp();

        // Initialize context which will trigger InitialConfigLoaderSupport
        ApplicationContext context = initContext();

        // Verify global config was created
        GeoServerInfo global = context.getBean(GeoServer.class).getGlobal();
        assertNotNull(global);

        // Verify the global.xml file was created
        assertTrue(Files.exists(dataDirectory.toPath().resolve("global.xml")));
    }

    /** Tests that missing logging config is created */
    @Test
    public void testAddMissingLoggingConfig() {
        // Setup existing global and services but missing logging
        support.getGeoServer().setGlobal(support.getGeoServer().getFactory().createGlobal());
        support.getGeoServer().add(new TestService1Impl());
        support.getGeoServer().add(new TestService2Impl());
        support.cleanUp();

        assertFalse(Files.exists(dataDirectory.toPath().resolve("logging.xml")));

        // Initialize context which will trigger InitialConfigLoaderSupport
        ApplicationContext context = initContext();

        // Verify the logging.xml file was created
        assertTrue(Files.exists(dataDirectory.toPath().resolve("logging.xml")));

        // Verify logging config was created
        LoggingInfo logging = context.getBean(GeoServer.class).getLogging();
        assertNotNull(logging);
        assertEquals("DEFAULT_LOGGING", logging.getLevel());
        assertEquals("logs/geoserver.log", logging.getLocation());
        assertTrue(logging.isStdOutLogging());
    }

    /** Tests that missing root services are created */
    @Test
    public void testAddMissingRootServices() {
        // Setup existing global and logging but missing services
        support.getGeoServer().setGlobal(support.getGeoServer().getFactory().createGlobal());
        support.getGeoServer().setLogging(support.getGeoServer().getFactory().createLogging());
        support.cleanUp();

        // Initialize context which will trigger InitialConfigLoaderSupport
        ApplicationContext context = initContext();

        // Verify service configs were created
        GeoServer gs = context.getBean(GeoServer.class);
        assertNotNull(gs.getService(TestService1.class));
        assertNotNull(gs.getService(TestService2.class));

        // Verify the service xml files were created
        assertTrue(Files.exists(dataDirectory.toPath().resolve("service1.xml")));
        assertTrue(Files.exists(dataDirectory.toPath().resolve("service2.xml")));
    }

    /** Tests that a pre-existing service is preserved when loading services */
    @Test
    public void testPreExistingService() {
        // Setup existing global and logging
        support.getGeoServer().setGlobal(support.getGeoServer().getFactory().createGlobal());
        support.getGeoServer().setLogging(support.getGeoServer().getFactory().createLogging());
        support.setUpServiceLoaders();
        // Add specific service with a custom property that we can check later
        TestService1Impl customService = new TestService1Impl();
        customService.setName("customName");
        support.getGeoServer().add(customService);
        support.cleanUp();

        // Initialize context which will trigger InitialConfigLoaderSupport
        ApplicationContext context = initContext();

        // Verify our custom service was preserved
        GeoServer gs = context.getBean(GeoServer.class);
        TestService1 service1 = gs.getService(TestService1.class);
        assertNotNull(service1);
        assertEquals("customName", service1.getName());

        // Verify service2 was created as normal
        assertNotNull(gs.getService(TestService2.class));
    }

    /**
     * Tests the case where the configuration is loaded from multiple processes from an empty data directory
     *
     * <p>This should cover all code paths in {@link MinimalConfigLoaderSupport}
     */
    @Test
    public void testConcurrentLoadFromEmptyDirectoryCreatesMinimalConfig() {

        // we can't really launch multiple context loads because they'll share the same
        // GeoServerExtensions static stuff, but we can run multiple loaders
        List<ConfigLoader> loaders = createConfigLoaders(8);
        loadConcurrently(loaders);

        // Verify the global.xml file was created
        assertTrue(Files.exists(dataDirectory.toPath().resolve("global.xml")));
        // Verify the logging.xml file was created
        assertTrue(Files.exists(dataDirectory.toPath().resolve("logging.xml")));
        // Verify the root services have been created
        assertTrue(Files.exists(dataDirectory.toPath().resolve("service1.xml")));
        assertTrue(Files.exists(dataDirectory.toPath().resolve("service2.xml")));

        for (ConfigLoader cl : loaders) {
            GeoServer gs = cl.geoServer;
            // Verify our externally created logging config was loaded
            LoggingInfo logging = gs.getLogging();
            assertEquals("DEFAULT_LOGGING", logging.getLevel());
            assertEquals("logs/geoserver.log", logging.getLocation());
            assertTrue(logging.isStdOutLogging());
        }

        // verify all loaders got root services of the same type with the same id. If not, it means the
        // XStreamServiceLoader on each "instance" created a default ServiceInfo but they didn't conflate to a common
        // one
        final String id =
                loaders.get(0).geoServer.getService(TestService1.class).getId();
        assertNotNull(id);

        Set<String> service1Ids = loaders.stream()
                .map(cl -> cl.geoServer)
                .map(l -> l.getService(TestService1.class))
                .map(ServiceInfo::getId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(id), service1Ids);
    }

    @Test
    public void testNesterLayerGroups() throws IOException {
        File testDatadir = Path.of("src", "test", "resources", "data_dir", "nested_layer_groups")
                .toFile();
        assertTrue(testDatadir.isDirectory());
        IOUtils.deepCopy(testDatadir, dataDirectory);

        try (XmlWebApplicationContext context = initNewContext()) {

            Catalog catalog = context.getBean("rawCatalog", Catalog.class);

            assertNotNull(catalog.getStyleByName("style"));
            assertNotNull(catalog.getWorkspaceByName("topp"));
            assertNotNull(catalog.getLayerByName("topp:layer1"));
            assertNotNull(catalog.getLayerByName("topp:layer2"));

            assertNotNull(catalog.getLayerGroupByName("simplegroup"));
            assertNotNull(catalog.getLayerGroupByName("nestedgroup"));
        }
    }

    @Test
    public void testStyleGroup() throws IOException {
        // prepare datadir
        File testDatadir = Path.of("src", "test", "resources", "data_dir", "nested_layer_groups")
                .toFile();
        assertTrue(testDatadir.isDirectory());
        IOUtils.deepCopy(testDatadir, dataDirectory);

        try (XmlWebApplicationContext intialContext = initNewContext()) {

            Catalog catalog = intialContext.getBean("rawCatalog", Catalog.class);
            StyleInfo styleGroup = catalog.getStyleByName("namedstyle");
            assertNotNull(styleGroup);

            LayerGroupInfo lg = catalog.getLayerGroupByName("simplegroup");
            lg.getLayers().clear();
            lg.getStyles().clear();
            lg.setMode(Mode.SINGLE);
            lg.getLayers().add(null);
            lg.getStyles().add(styleGroup);
            catalog.save(lg);

            support.cleanUp(); // clean up GeoServerExtensions
        }

        Awaitility.await().atMost(5, SECONDS).untilAsserted(() -> {
            try (XmlWebApplicationContext context = initNewContext()) {
                Catalog catalog = context.getBean("rawCatalog", Catalog.class);
                LayerGroupInfo lg = catalog.getLayerGroupByName("simplegroup");
                assertNotNull(lg);
                assertEquals(Mode.SINGLE, lg.getMode());
                assertEquals(1, lg.getLayers().size());
                assertEquals(null, lg.getLayers().get(0));

                StyleInfo styleGroup = catalog.getStyleByName("namedstyle");
                assertNotNull(styleGroup);
                assertEquals(styleGroup, lg.getStyles().get(0));
            }
        });
    }

    private void loadConcurrently(List<ConfigLoader> loaders) {
        List<Callable<ConfigLoader>> tasks = loaders.stream()
                .map(l -> new Callable<ConfigLoader>() {
                    @Override
                    public ConfigLoader call() throws Exception {
                        l.loadGeoServer();
                        return l;
                    }
                })
                .collect(Collectors.toList());
        List<Future<ConfigLoader>> futures = ForkJoinPool.commonPool().invokeAll(tasks);
        futures.forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<ConfigLoader> createConfigLoaders(int count) {
        // they share the configLock to simulate a cluster set up
        final GeoServerConfigurationLock configLock = new GeoServerConfigurationLock();
        LockProvider lockProvider = new MemoryLockProvider();
        return IntStream.range(0, count)
                .mapToObj(i -> createConfigLoader(configLock, lockProvider))
                .collect(Collectors.toList());
    }

    private ConfigLoader createConfigLoader(GeoServerConfigurationLock configLock, LockProvider lockProvider) {

        // create without persistence listeners. On the regular call chain,
        // GeoServerLoader would have removed them before.
        DataDirectoryLoaderTestSupport ddlts = DataDirectoryLoaderTestSupport.withNoPersistence(dataDirectory);
        Catalog catalog = ddlts.getCatalog();
        GeoServer gs = ddlts.getGeoServer();

        GeoServerResourceLoader resourceLoader = catalog.getResourceLoader();
        FileSystemResourceStore resourceStore = (FileSystemResourceStore) resourceLoader.getResourceStore();
        resourceStore.setLockProvider(lockProvider);

        GeoServerDataDirectory dd = new GeoServerDataDirectory(dataDirectory);
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        @SuppressWarnings("rawtypes")
        List<XStreamServiceLoader> serviceLoaders = new ArrayList<>();
        serviceLoaders.add(support.serviceLoader1);
        serviceLoaders.add(support.serviceLoader2);

        DataDirectoryWalker fileWalk = new DataDirectoryWalker(dd, xpf, configLock, serviceLoaders);

        return new ConfigLoader(gs, fileWalk);
    }

    private XmlWebApplicationContext initContext() {
        return initNewContext();
    }

    private XmlWebApplicationContext initNewContext() {
        XmlWebApplicationContext webAppContext = createContext();
        webAppContext.refresh();
        return webAppContext;
    }

    private XmlWebApplicationContext createContext() {
        // Simulate the servlet container
        ServletContext servletContext = new MockServletContext();

        // Load a WebApplicationContext instead of a plain ApplicationContext
        XmlWebApplicationContext webAppContext = new XmlWebApplicationContext();
        webAppContext.setConfigLocations(
                "classpath*:/applicationContext.xml",
                "classpath*:/applicationSecurityContext.xml",
                "classpath:/org/geoserver/config/datadir/minimalConfigLoaderSupportIntegrationTestContext.xml");
        webAppContext.setServletContext(servletContext);

        return webAppContext;
    }
}

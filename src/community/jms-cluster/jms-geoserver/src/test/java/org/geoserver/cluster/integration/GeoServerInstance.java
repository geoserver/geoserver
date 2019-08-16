/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.integration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cluster.client.JMSQueueListener;
import org.geoserver.cluster.events.ToggleType;
import org.geoserver.cluster.impl.rest.Controller;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.DirectoryResourceLoader;
import org.geoserver.test.GeoServerTestApplicationContext;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Log4JLoggerFactory;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.XSD;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * Creates a GeoServer instance that can be used to tests JMS synchronizations. Note that only the
 * bare minimum required elements will be instance.
 */
public final class GeoServerInstance {

    // this test data directory will be used to instantiate all GeoServe instances
    private static final SystemTestData BASE_TEST_DATA = createTestData();

    // cluster name for the interaction tests
    private static final String CLUSTER_NAME = UUID.randomUUID().toString();

    /** Helper method that creates a system test data directory. */
    private static SystemTestData createTestData() {
        SystemTestData testData;
        // instantiate a test data directory
        try {
            // initiate the test data
            testData = new SystemTestData();
            testData.setUp();
            // add layers and styles
            testData.setUpDefault();
            // add GeoServer default styles, this will allow us to have the same ids
            addDefaultStyles(testData.getDataDirectoryRoot());
        } catch (Exception exception) {
            throw new RuntimeException("Error creating base test directory.", exception);
        }
        // add a JVM shutdown for removing the test data directory
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    try {
                                        IOUtils.delete(BASE_TEST_DATA.getDataDirectoryRoot());
                                    } catch (Exception exception) {
                                        throw new RuntimeException(
                                                String.format(
                                                        "Error deleting base test data directory '%s'.",
                                                        BASE_TEST_DATA
                                                                .getDataDirectoryRoot()
                                                                .getAbsolutePath()),
                                                exception);
                                    }
                                }));
        return testData;
    }

    /** Helper method that adds GeoServer default styles to a dt directory. */
    private static void addDefaultStyles(File dataDirectory) throws IOException {
        // prepare all the necessary GeoServer objects
        File stylesDirectory = new File(dataDirectory, "styles");
        GeoServerResourceLoader loader = new GeoServerResourceLoader(dataDirectory);
        Catalog catalog = new CatalogImpl();
        catalog.setResourceLoader(loader);
        XStreamPersister xstreamPersister = new XStreamPersisterFactory().createXMLPersister();
        GeoServerConfigPersister geoserverPersister =
                new GeoServerConfigPersister(loader, xstreamPersister);
        catalog.addListener(geoserverPersister);
        catalog.addListener(new GeoServerResourcePersister(catalog));
        // create default styles
        createDefaultStyle(catalog, stylesDirectory, "point", "default_point.sld");
        createDefaultStyle(catalog, stylesDirectory, "line", "default_line.sld");
        createDefaultStyle(catalog, stylesDirectory, "polygon", "default_polygon.sld");
        createDefaultStyle(catalog, stylesDirectory, "raster", "default_raster.sld");
        createDefaultStyle(catalog, stylesDirectory, "generic", "default_generic.sld");
    }

    /** Helper method that adds a GeoServer default style to the provided styles directory. */
    private static void createDefaultStyle(
            Catalog catalog, File stylesDirectory, String styleName, String fileName) {
        // copy style from classpath to styles directory
        try {
            IOUtils.copy(
                    GeoServerLoader.class.getResourceAsStream(fileName),
                    new File(stylesDirectory, fileName));
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error copying default style '%s' to directory '%s'.",
                            fileName, stylesDirectory.getAbsolutePath()),
                    exception);
        }
        // create GeoServer style object
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(styleName);
        style.setFilename(fileName);
        // add the style to the catalog, GeoServer persister will take of writing the style
        // description
        catalog.add(style);
    }

    // this instance data directory
    private final File dataDirectory;
    // this instance application context
    private final GeoServerTestApplicationContext applicationContext;

    // will be used to set JMS options
    private final Controller jmsController;
    // will be used to listen on consumed events
    private final JMSQueueListener jmsQueueListener;

    public GeoServerInstance() {
        this(null);
    }

    public GeoServerInstance(String instanceName) {
        try {
            // create this instance base data directory by copying the base test data
            dataDirectory = createTempDirectory(instanceName == null ? "INSTANCE" : instanceName);
            IOUtils.deepCopy(BASE_TEST_DATA.getDataDirectoryRoot(), dataDirectory);
            // disable security manager to speed up tests
            System.setSecurityManager(null);
            // take control of the logging
            Logging.ALL.setLoggerFactory(Log4JLoggerFactory.getInstance());
            System.setProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, "true");
            // initialize Spring application context
            applicationContext = initInstance();
            // get some  JMS util beans
            jmsController = applicationContext.getBean(Controller.class);
            jmsQueueListener = applicationContext.getBean(JMSQueueListener.class);
            // set integration tests cluster name
            jmsController.setGroup(CLUSTER_NAME);
            saveJmsConfiguration();
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error instantiating GeoServer instance '%s'.", instanceName),
                    exception);
        }
    }

    /** Helper method that just creates a temporary directory using the provide prefix. */
    private static File createTempDirectory(String prefix) {
        try {
            // creates a temporary directory using the provided prefix
            return IOUtils.createTempDirectory(prefix + "-");
        } catch (Exception exception) {
            throw new RuntimeException("Error creating temporary directory.", exception);
        }
    }

    /**
     * Instantiates this GeoServer instance, i.e. a resource loader based on this instance data
     * directory is instantiated, a mocked servlet context is created and an application context is
     * initiated.
     */
    private GeoServerTestApplicationContext initInstance() throws Exception {
        // instantiate GeoServer loader
        GeoServerResourceLoader loader = new GeoServerResourceLoader(dataDirectory);
        // setting logging level
        LoggingUtils.configureGeoServerLogging(
                loader,
                this.getClass().getResourceAsStream("/TEST_LOGGING.properties"),
                false,
                true,
                null);
        // create a mocked servlet context and instantiate the application context
        MockServletContext servletContext = createServletContext();
        GeoServerTestApplicationContext applicationContext =
                new GeoServerTestApplicationContext(
                        new String[] {
                            "classpath*:/applicationContext.xml",
                            "classpath*:/applicationSecurityContext.xml"
                        },
                        servletContext);
        applicationContext.setUseLegacyGeoServerLoader(false);
        applicationContext.refresh();
        applicationContext.publishEvent(new ContextLoadedEvent(applicationContext));
        servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
        return applicationContext;
    }

    /** Helper method that creates a mocked servlet context. */
    private MockServletContext createServletContext() {
        // set up a fake WEB-INF directory
        ResourceLoader loader;
        if (dataDirectory.canWrite()) {
            // make sure we have a WEB-INF directory
            new File(dataDirectory, "WEB-INF").mkdirs();
            loader = new DirectoryResourceLoader(dataDirectory);
        } else {
            // use the default loader
            loader = new DefaultResourceLoader();
        }
        // create a mocked servlet context and set some options
        MockServletContext servletContext = new MockServletContext(loader);
        servletContext.setMinorVersion(4);
        servletContext.setInitParameter("GEOSERVER_DATA_DIR", dataDirectory.getPath());
        return servletContext;
    }

    /** Returns this GeoServer instance catalog. */
    public Catalog getCatalog() {
        return (Catalog) applicationContext.getBean("catalog");
    }

    /** Returns this GeoServer instance configuration accessor. */
    public GeoServer getGeoServer() {
        return (GeoServer) applicationContext.getBean("geoServer");
    }

    /** Returns this GeoServer instance resource loader. */
    public GeoServerResourceLoader getResourceLoader() {
        return (GeoServerResourceLoader) applicationContext.getBean("resourceLoader");
    }

    /** Returns this GeoServer instance data directory. */
    public GeoServerDataDirectory getDataDirectory() {
        return new GeoServerDataDirectory(getResourceLoader());
    }

    /** This GeoServer instance will stop propagating JMS events. */
    public void disableJmsMaster() {
        jmsController.toggle(false, ToggleType.MASTER);
        saveJmsConfiguration();
    }

    /** This GeoServer instance will propagate JMS events. */
    public void enableJmsMaster() {
        jmsController.toggle(true, ToggleType.MASTER);
        saveJmsConfiguration();
    }

    /** This GeoServer instance will ignore JMS events. */
    public void disableJmsSlave() {
        jmsController.toggle(false, ToggleType.SLAVE);
        saveJmsConfiguration();
    }

    /** This GeoServer instance will consume JMS events. */
    public void enableJmsSlave() {
        jmsController.toggle(true, ToggleType.SLAVE);
        saveJmsConfiguration();
    }

    /**
     * Makes this GeoServer instance belong to the default JMS cluster, propagate JMS events and
     * consume JMS events.
     */
    public void setJmsDefaultConfiguration() {
        jmsController.setBrokerURL("");
        jmsController.setGroup("geoserver-cluster");
        enableJmsMaster();
        enableJmsSlave();
    }

    /** Will wait until the expected number of events was consumed or the timeout is reached. */
    public void waitEvents(int number, int timeoutMs) {
        int loops = timeoutMs / 25;
        for (int i = 0; i <= loops && jmsQueueListener.getConsumedEvents() < number; i++) {
            try {
                // wait for 25 milliseconds
                Thread.sleep(25);
            } catch (InterruptedException exception) {
                // well we got interrupted
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Returns the total number of JMS events consumed by this GeoServer instance. */
    public int getConsumedEventsCount() {
        return (int) jmsQueueListener.getConsumedEvents();
    }

    /** Resets the total number of JMS events consumed by this GeoServer instance. */
    public void resetConsumedEventsCount() {
        jmsQueueListener.resetconsumedevents();
    }

    /** Helper method tht just saves the current JMS configuration */
    private void saveJmsConfiguration() {
        try {
            jmsController.save();
        } catch (Exception exception) {
            throw new RuntimeException("Error saving JMS configuration.", exception);
        }
    }

    /** Destroy everything related with this instance. */
    public void destroy() {
        // dispose XSD schema, this is important for WFS schemas
        applicationContext.getBeansOfType(XSD.class).values().forEach(XSD::dispose);
        // destroy Spring application context
        applicationContext.destroy();
        // remove the data directory
        try {
            IOUtils.delete(dataDirectory);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error deleting test directory '%s'.", dataDirectory.getAbsolutePath()),
                    exception);
        }
    }
}

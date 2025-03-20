/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import com.google.common.collect.Lists;
import it.geosolutions.concurrent.ConcurrentTileCacheMultiMap;
import it.geosolutions.jaiext.ConcurrentOperationRegistry;
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.IIOServiceProvider;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.RegistryMode;
import javax.media.jai.remote.SerializableRenderedImage;
import javax.media.jai.util.ImagingListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.logging.LoggingUtils.GeoToolsLoggingRedirection;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.api.data.DataAccessFinder;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.referencing.AuthorityFactory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.image.io.ImageIOExt;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.AbstractAuthorityFactory;
import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * Listens for GeoServer startup and tries to configure axis order, logging redirection, and a few other things that
 * really need to be set up before anything else starts up
 */
public class GeoserverInitStartupListener implements ServletContextListener {
    static final String COM_SUN_JPEG2000_PACKAGE = "com.sun.media.imageioimpl.plugins.jpeg2000";

    private static Logger LOGGER;

    boolean relinquishLoggingControl;

    private static final String COMPARISON_TOLERANCE_PROPERTY = "COMPARISON_TOLERANCE";

    private static final double DEFAULT_COMPARISON_TOLERANCE = 1e-8;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // enable JTS overlay-ng unless otherwise set (first thing, before JTS has a chance
        // to initialize itself)
        if (System.getProperty("jts.overlay") == null) {
            System.setProperty("jts.overlay", "ng");
        }

        // establish logging redirection
        GeoToolsLoggingRedirection policy = establishLoggingRedirectionPolicy(sce.getServletContext());

        LOGGER = Logging.getLogger("org.geoserver.logging");
        LOGGER.config("Logging policy: " + policy);
        GeoTools.init((Hints) null);

        initJAIDefaultInstance();

        // setup concurrent operation registry
        JAI jaiDef = JAI.getDefaultInstance();
        if (!(jaiDef.getOperationRegistry() instanceof ConcurrentOperationRegistry)) {
            jaiDef.setOperationRegistry(ConcurrentOperationRegistry.initializeRegistry());
        }

        // setup the concurrent tile cache (has proper memory limit handling also for small tiles)
        if (!(jaiDef.getTileCache() instanceof ConcurrentTileCacheMultiMap)) {
            jaiDef.setTileCache(new ConcurrentTileCacheMultiMap());
        }

        // make sure we remember if GeoServer controls logging or not
        String strValue =
                GeoServerExtensions.getProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, sce.getServletContext());
        relinquishLoggingControl = Boolean.valueOf(strValue);

        // if the server admin did not set it up otherwise, force X/Y axis
        // ordering
        // This one is a good place because we need to initialize this property
        // before any other opeation can trigger the initialization of the CRS
        // subsystem
        if (System.getProperty("org.geotools.referencing.forceXY") == null) {
            System.setProperty("org.geotools.referencing.forceXY", "true");
        }
        if (Boolean.TRUE.equals(Hints.getSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER))) {
            Hints.putSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING, "http");
        }
        Hints.putSystemDefault(Hints.LENIENT_DATUM_SHIFT, true);

        // setup the referencing tolerance to make it more tolerant to tiny differences
        // between projections (increases the chance of matching a random prj file content
        // to an actual EPSG code
        String comparisonToleranceProperty = GeoServerExtensions.getProperty(COMPARISON_TOLERANCE_PROPERTY);
        double comparisonTolerance = DEFAULT_COMPARISON_TOLERANCE;
        if (comparisonToleranceProperty != null) {
            try {
                comparisonTolerance = Double.parseDouble(comparisonToleranceProperty);
            } catch (NumberFormatException nfe) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("Unable to parse the specified COMPARISON_TOLERANCE "
                            + "system property: "
                            + comparisonToleranceProperty
                            + " which should be a number. Using Default: "
                            + DEFAULT_COMPARISON_TOLERANCE);
                }
            }
        }
        Hints.putSystemDefault(Hints.COMPARISON_TOLERANCE, comparisonTolerance);

        final Hints defHints = GeoTools.getDefaultHints();

        // Initialize GridCoverageFactory so that we don't make a lookup every time a factory is
        // needed
        Hints.putSystemDefault(Hints.GRID_COVERAGE_FACTORY, CoverageFactoryFinder.getGridCoverageFactory(defHints));

        // don't allow the connection to the EPSG database to time out. This is a server app,
        // we can afford keeping the EPSG db always on
        System.setProperty("org.geotools.epsg.factory.timeout", "-1");

        // HACK: java.util.prefs are awful. See
        // http://www.allaboutbalance.com/disableprefs. When the site comes
        // back up we should implement their better way of fixing the problem.
        System.setProperty("java.util.prefs.syncInterval", "5000000");

        // Fix issue with tomcat and JreMemoryLeakPreventionListener causing issues with
        // IIORegistry leading to imageio plugins not being properly initialized
        ImageIO.scanForPlugins();

        // in any case, the native png reader is worse than the pure java ones, so
        // let's disable it (the native png writer is on the other side faster)...
        ImageIOExt.allowNativeCodec("png", ImageReaderSpi.class, false);
        ImageIOExt.allowNativeCodec("png", ImageWriterSpi.class, true);

        // remove the ImageIO JPEG200 readers/writes, they are outdated and not quite working
        // GeoTools has the GDAL and Kakadu ones which do work, removing these avoids the
        // registry russian roulette (one never knows which one comes first, and
        // to re-order/unregister correctly the registry scan has to be completed
        unregisterImageIOJpeg2000Support(ImageReaderSpi.class);
        unregisterImageIOJpeg2000Support(ImageWriterSpi.class);

        // initialize GeoTools factories so that we don't make a SPI lookup every time a factory is
        // needed
        Hints.putSystemDefault(Hints.FILTER_FACTORY, CommonFactoryFinder.getFilterFactory(null));
        Hints.putSystemDefault(Hints.STYLE_FACTORY, CommonFactoryFinder.getStyleFactory(null));
        Hints.putSystemDefault(Hints.FEATURE_FACTORY, CommonFactoryFinder.getFeatureFactory(null));

        // initialize the default executor service
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CoverageAccessInfoImpl.DEFAULT_CorePoolSize,
                CoverageAccessInfoImpl.DEFAULT_MaxPoolSize,
                CoverageAccessInfoImpl.DEFAULT_KeepAliveTime,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        Hints.putSystemDefault(Hints.EXECUTOR_SERVICE, executor);
    }

    /** Sets a custom ImagingListener used to ignore common warnings */
    public static void initJAIDefaultInstance() {
        JAI jaiDef = JAI.getDefaultInstance();
        if (!(jaiDef.getImagingListener() instanceof GeoServerImagingListener)) {
            jaiDef.setImagingListener(new GeoServerImagingListener());
        }
    }

    private static final class GeoServerImagingListener implements ImagingListener {
        private static final Logger LOGGER = Logging.getLogger("javax.media.jai");

        @Override
        public boolean errorOccurred(String message, Throwable thrown, Object where, boolean isRetryable)
                throws RuntimeException {
            if (isSerializableRenderedImageFinalization(where, thrown)) {
                LOGGER.log(Level.FINEST, message, thrown);
            } else if (message.contains("Continuing in pure Java mode")) {
                LOGGER.log(Level.FINE, message, thrown);
            } else if (thrown instanceof RuntimeException && !(where instanceof OperationRegistry)) {
                throw (RuntimeException) thrown;
            } else {
                LOGGER.log(Level.INFO, message, thrown);
            }
            return false; // we are not trying to recover
        }

        private boolean isSerializableRenderedImageFinalization(Object where, Throwable t) {
            if (!(where instanceof SerializableRenderedImage)) {
                return false;
            }

            // check if it's the finalizer
            StackTraceElement[] elements = t.getStackTrace();
            for (StackTraceElement element : elements) {
                if (element.getMethodName().equals("finalize")
                        && element.getClassName().endsWith("SerializableRenderedImage")) return true;
            }

            return false;
        }
    }

    /**
     * Unregisters providers in the "https://github.com/geosolutions-it/evo-odas/issues/102" for a given category
     * (reader, writer). ImageIO contains a pure java reader and a writer, but also a couple based on native libs (if
     * present).
     */
    private <T extends ImageReaderWriterSpi> void unregisterImageIOJpeg2000Support(Class<T> category) {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        Iterator<T> it = registry.getServiceProviders(category, false);
        ArrayList<T> providers = Lists.newArrayList(it);
        for (T spi : providers) {
            if (COM_SUN_JPEG2000_PACKAGE.equals(spi.getClass().getPackage().getName())) {
                registry.deregisterServiceProvider(spi);
            }
        }
    }

    /**
     * This method tries hard to stop all threads and remove all references to classes in GeoServer so that we can avoid
     * permgen leaks on application undeploy. What happes is that, if any JDK class references to one of the classes
     * loaded by the webapp classloader, then the classloader cannot be collected and neither can all the classes loaded
     * by it (since each class keeps a back reference to the classloader that loaded it). The same happens for any
     * residual thread launched by the web app.
     */
    @Override
    @SuppressWarnings("PMD.ForLoopCanBeForeach")
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            LOGGER.info("Beginning GeoServer cleanup sequence");

            // the dreaded classloader
            ClassLoader webappClassLoader = getClass().getClassLoader();

            // unload all of the jdbc drivers we have loaded. We need to store them and unregister
            // later to avoid concurrent modification exceptions
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            Set<Driver> driversToUnload = new HashSet<>();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                try {
                    // the driver class loader can be null if the driver comes from the JDK, such as
                    // the
                    // sun.jdbc.odbc.JdbcOdbcDriver
                    ClassLoader driverClassLoader = driver.getClass().getClassLoader();
                    if (driverClassLoader != null && driverClassLoader.equals(webappClassLoader)) {
                        driversToUnload.add(driver);
                    }
                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE, "", t);
                }
            }
            for (Driver driver : driversToUnload) {
                try {
                    DriverManager.deregisterDriver(driver);
                    LOGGER.fine("Unregistered JDBC driver " + driver);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Could now unload driver " + driver.getClass(), e);
                }
            }
            try {
                Class<?> h2Driver = Class.forName("org.h2.Driver");
                Method m = h2Driver.getMethod("unload");
                m.invoke(null);
            } catch (java.lang.ClassNotFoundException notIncluded) {
                if ("org.h2.Driver".equalsIgnoreCase(notIncluded.getMessage())) {
                    LOGGER.log(Level.FINE, "H2 driver not included, skipping unload");
                } else {
                    LOGGER.log(Level.WARNING, "Failed to unload the H2 driver", notIncluded);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to unload the H2 driver", e);
            }

            // unload all deferred authority factories so that we get rid of the timer tasks in them
            try {
                disposeAuthorityFactories(ReferencingFactoryFinder.getCoordinateOperationAuthorityFactories(null));
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Error occurred trying to dispose authority factories", e);
            }
            try {
                disposeAuthorityFactories(ReferencingFactoryFinder.getCRSAuthorityFactories(null));
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Error occurred trying to dispose authority factories", e);
            }
            try {
                disposeAuthorityFactories(ReferencingFactoryFinder.getCSAuthorityFactories(null));
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Error occurred trying to dispose authority factories", e);
            }

            // kill the threads created by referencing
            WeakCollectionCleaner.DEFAULT.exit();
            DeferredAuthorityFactory.exit();
            CRS.reset("all");
            LOGGER.fine("Shut down GT referencing threads ");
            // reset
            ReferencingFactoryFinder.reset();
            CommonFactoryFinder.reset();
            DataStoreFinder.reset();
            DataAccessFinder.reset();
            LOGGER.fine("Shut down GT  SPI ");

            LOGGER.fine("Shut down coverage thread pool ");
            Object o = Hints.getSystemDefault(Hints.EXECUTOR_SERVICE);
            if (o != null && o instanceof ExecutorService) {
                final ThreadPoolExecutor executor = (ThreadPoolExecutor) o;
                try {
                    executor.shutdown();
                } finally {
                    executor.shutdownNow();
                }
            }

            // unload everything that JAI ImageIO can still refer to
            // We need to store them and unregister later to avoid concurrent modification
            // exceptions
            final IIORegistry ioRegistry = IIORegistry.getDefaultInstance();
            Set<IIOServiceProvider> providersToUnload = new HashSet<>();
            for (Iterator<Class<?>> cats = ioRegistry.getCategories(); cats.hasNext(); ) {
                Class<?> category = cats.next();
                for (Iterator it = ioRegistry.getServiceProviders(category, false); it.hasNext(); ) {
                    final IIOServiceProvider provider = (IIOServiceProvider) it.next();
                    if (webappClassLoader.equals(provider.getClass().getClassLoader())) {
                        providersToUnload.add(provider);
                    }
                }
            }
            for (IIOServiceProvider provider : providersToUnload) {
                ioRegistry.deregisterServiceProvider(provider);
                LOGGER.fine("Unregistering Image I/O provider " + provider);
            }

            // unload everything that JAI can still refer to
            final OperationRegistry opRegistry = JAI.getDefaultInstance().getOperationRegistry();
            for (String mode : RegistryMode.getModeNames()) {
                for (Iterator descriptors = opRegistry.getDescriptors(mode).iterator();
                        descriptors != null && descriptors.hasNext(); ) {
                    RegistryElementDescriptor red = (RegistryElementDescriptor) descriptors.next();
                    int factoryCount = 0;
                    int unregisteredCount = 0;
                    // look for all the factories for that operation
                    for (Iterator factories = opRegistry.getFactoryIterator(mode, red.getName());
                            factories != null && factories.hasNext(); ) {
                        Object factory = factories.next();
                        if (factory == null) {
                            continue;
                        }
                        factoryCount++;
                        if (webappClassLoader.equals(factory.getClass().getClassLoader())) {
                            boolean unregistered = false;
                            // we need to scan against all "products" to unregister the factory
                            List orderedProductList = opRegistry.getOrderedProductList(mode, red.getName());
                            if (orderedProductList != null) {
                                for (Iterator products = orderedProductList.iterator();
                                        products != null && products.hasNext(); ) {
                                    String product = (String) products.next();
                                    try {
                                        opRegistry.unregisterFactory(mode, red.getName(), product, factory);
                                        LOGGER.fine("Unregistering JAI factory " + factory.getClass());
                                    } catch (Throwable t) {
                                        // may fail due to the factory not being registered against
                                        // that product
                                    }
                                }
                            }
                            if (unregistered) {
                                unregisteredCount++;
                            }
                        }
                    }

                    // if all the factories were unregistered, get rid of the descriptor as well
                    if (factoryCount > 0 && unregisteredCount == factoryCount) {
                        opRegistry.unregisterDescriptor(red);
                    }
                }
            }

            // flush all javabean introspection caches as this too can keep a webapp classloader
            // from being unloaded
            Introspector.flushCaches();
            LOGGER.fine("Cleaned up javabean caches");

            // unload the logging framework
            if (!relinquishLoggingControl) LogManager.shutdown();
            LogFactory.release(Thread.currentThread().getContextClassLoader());

            // GeoTools/GeoServer have a lot of finalizers and until they are run the JVM
            // itself wil keepup the class loader...
            try {
                System.gc();
                System.runFinalization();
                System.gc();
                System.runFinalization();
                System.gc();
                System.runFinalization();
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to perform closing up finalization", t);
            }
        } catch (Throwable t) {
            // if anything goes south during the cleanup procedures I want to know what it is
            LOGGER.log(Level.SEVERE, "", t);
        }
    }

    /**
     * Looks up for a named string property into the following contexts (in order):
     *
     * <ul>
     *   <li>System Property
     *   <li>web.xml init parameters
     *   <li>Environment variable
     * </ul>
     *
     * and returns the first non null, non empty value found.
     *
     * @param propertyName The property name to be searched
     * @param context The servlet context used to look into web.xml (may be null)
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName, ServletContext context) {
        // This logic is the same as GeoServerExtensions.getProperty(name, context)
        // and data directory lookup (and it's useful).
        // Cannot use those implementations as logging is not yet configured
        String property = System.getProperty(propertyName);
        if (context != null && property == null) {
            property = context.getInitParameter(propertyName);
        }
        if (property == null) {
            property = System.getenv(propertyName);
        }
        return property;
    }

    /**
     * Establish logging redirection policy based on GT2_LOGGING_REDIRECTION property.
     *
     * @param context The servlet context used to look into web.xml (may be null)
     * @return logging redirection policy
     */
    GeoToolsLoggingRedirection establishLoggingRedirectionPolicy(ServletContext context) {
        GeoToolsLoggingRedirection policy =
                GeoToolsLoggingRedirection.findValue(getProperty(LoggingUtils.GT2_LOGGING_REDIRECTION, context));
        try {
            // Use string to reference logger factory to protect from init failure
            switch (policy) {
                case JavaLogging:
                    Logging.ALL.setLoggerFactory((org.geotools.util.logging.LoggerFactory) null);
                    break;
                case Logback:
                    Logging.ALL.setLoggerFactory("org.geotools.util.logging.LogbackLoggerFactory");
                    break;
                case Log4J2:
                    Logging.ALL.setLoggerFactory("org.geotools.util.logging.Log4J2LoggerFactory");
                    break;
                case CommonsLogging:
                    Logging.ALL.setLoggerFactory("org.geotools.util.logging.CommonsLoggerFactory");
                    break;
                case Log4J:
                    Logging.ALL.setLoggerFactory("org.geotools.util.logging.Log4JLoggerFactory");
            }
        } catch (Exception e) {
            Logging.ALL.setLoggerFactory((org.geotools.util.logging.LoggerFactory) null);
            Logging.getLogger("org.geoserver.logging")
                    .log(Level.SEVERE, "Could not configure log4j logging redirection: '" + policy + "'", e);
            return null;
        }
        return policy;
    }

    private void disposeAuthorityFactories(Set<? extends AuthorityFactory> factories) throws FactoryException {
        for (AuthorityFactory af : factories) {
            if (af instanceof AbstractAuthorityFactory) {
                LOGGER.fine("Disposing referencing factory " + af);
                ((AbstractAuthorityFactory) af).dispose();
            }
        }
    }
}

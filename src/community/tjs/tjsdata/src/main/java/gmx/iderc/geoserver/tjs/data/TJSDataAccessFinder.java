/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2003-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package gmx.iderc.geoserver.tjs.data;

import org.geotools.data.DataAccessFactory;
import org.geotools.factory.FactoryCreator;
import org.geotools.factory.FactoryRegistry;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enable programs to find all available DataAccess implementations, including
 * the DataStore ones.
 * <p/>
 * <p>
 * In order to be located by this finder datasources must provide an
 * implementation of the {@link DataAccessFactory} interface.
 * </p>
 * <p/>
 * <p>
 * In addition to implementing this interface datasouces should have a services
 * file:<br/><code>META-INF/services/org.geotools.data.DataAccessFactory</code>
 * </p>
 * <p/>
 * <p>
 * The file should contain a single line which gives the full name of the
 * implementing class.
 * </p>
 * <p/>
 * <p>
 * Example:<br/><code>org.geotools.data.mytype.MyTypeDataAccessFacotry</code>
 * </p>
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/main/src/main/java/org/geotools/data/DataAccessFinder.java $
 */
public final class TJSDataAccessFinder {
    /**
     * The logger for the filter module.
     */
    protected static final Logger LOGGER = org.geotools.util.logging.Logging
                                                   .getLogger("org.geotools.data");

    /**
     * The service registry for this manager. Will be initialized only when
     * first needed.
     */
    private static FactoryRegistry registry;

    // Singleton pattern
    private TJSDataAccessFinder() {
    }

    /**
     * Checks each available datasource implementation in turn and returns the
     * first one which claims to support the resource identified by the params
     * object.
     *
     * @param params A Map object which contains a defenition of the resource to
     *               connect to. for file based resources the property 'url' should
     *               be set within this Map.
     * @return The first datasource which claims to process the required
     *         resource, returns null if none can be found.
     * @throws IOException If a suitable loader can be found, but it can not be attached
     *                     to the specified resource without errors.
     */
    public static synchronized TJSDataStore getDataStore(
                                                                Map<String, Serializable> params) throws IOException {
        Iterator<TJSDataAccessFactory> ps = getAvailableDataStores();
        return (TJSDataStore) getDataStore(params, ps);
    }

    static TJSDataStore getDataStore(
                                            Map<String, Serializable> params, Iterator<? extends TJSDataAccessFactory> ps)
            throws IOException {
        TJSDataAccessFactory fac;

        IOException canProcessButNotAvailable = null;
        while (ps.hasNext()) {
            fac = (TJSDataAccessFactory) ps.next();
            boolean canProcess = false;
            try {
                canProcess = fac.canProcess(params);
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Problem asking " + fac.getDisplayName()
                                                  + " if it can process request:" + t, t);
                // Protect against DataStores that don't carefully code
                // canProcess
                continue;
            }
            if (canProcess) {
                boolean isAvailable = false;
                try {
                    isAvailable = fac.isAvailable();
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING, "Difficulity checking if " + fac.getDisplayName()
                                                      + " is available:" + t, t);
                    // Protect against DataStores that don't carefully code
                    // isAvailable
                    continue;
                }
                if (isAvailable) {
                    try {
                        return fac.createDataStore(params);
                    } catch (IOException couldNotConnect) {
                        canProcessButNotAvailable = couldNotConnect;
                        LOGGER.log(Level.WARNING, fac.getDisplayName()
                                                          + " should be used, but could not connect", couldNotConnect);
                    }
                } else {
                    canProcessButNotAvailable = new IOException(
                                                                       fac.getDisplayName()
                                                                               + " should be used, but is not availble. Have you installed the required drivers or jar files?");
                    LOGGER.log(Level.WARNING, fac.getDisplayName()
                                                      + " should be used, but is not availble", canProcessButNotAvailable);
                }
            }
        }
        if (canProcessButNotAvailable != null) {
            throw canProcessButNotAvailable;
        }
        return null;
    }

    /**
     * Finds all implemtaions of DataAccessFactory which have registered using
     * the services mechanism, regardless weather it has the appropriate
     * libraries on the classpath.
     *
     * @return An iterator over all discovered datastores which have registered
     *         factories
     */
    public static synchronized Iterator<TJSDataAccessFactory> getAllDataStores() {
        Set<TJSDataAccessFactory> all = new HashSet<TJSDataAccessFactory>();
        Iterator<TJSDataStoreFactorySpi> allDataStores = TJSDataStoreFinder.getAllDataStores();
        Iterator<TJSDataAccessFactory> allDataAccess = getAllDataStores(getServiceRegistry(),
                                                                               TJSDataAccessFactory.class);
        while (allDataStores.hasNext()) {
            TJSDataStoreFactorySpi next = allDataStores.next();
            all.add(next);
        }

        while (allDataAccess.hasNext()) {
            all.add(allDataAccess.next());
        }

        return all.iterator();
    }

    static synchronized <T extends TJSDataAccessFactory> Iterator<T> getAllDataStores(
                                                                                             FactoryRegistry registry, Class<T> category) {
        return registry.getServiceProviders(category, null, null);
    }

    /**
     * Finds all implemtaions of DataAccessFactory which have registered using
     * the services mechanism, and that have the appropriate libraries on the
     * classpath.
     *
     * @return An iterator over all discovered datastores which have registered
     *         factories, and whose available method returns true.
     */
    public static synchronized Iterator<TJSDataAccessFactory> getAvailableDataStores() {

        FactoryRegistry serviceRegistry = getServiceRegistry();
        Set<TJSDataAccessFactory> availableDS = getAvailableDataStores(serviceRegistry,
                                                                              TJSDataAccessFactory.class);

        Iterator<TJSDataStoreFactorySpi> availableDataStores = TJSDataStoreFinder
                                                                       .getAvailableDataStores();
        while (availableDataStores.hasNext()) {
            availableDS.add(availableDataStores.next());
        }

        return availableDS.iterator();
    }

    static synchronized <T extends TJSDataAccessFactory> Set<T> getAvailableDataStores(
                                                                                              FactoryRegistry registry, Class<T> targetClass) {
        Set<T> availableDS = new HashSet<T>(5);
        Iterator<T> it = registry.getServiceProviders(targetClass, null, null);
        T dsFactory;
        while (it.hasNext()) {
            dsFactory = it.next();

            if (dsFactory.isAvailable()) {
                availableDS.add(dsFactory);
            }
        }

        return availableDS;
    }

    /**
     * Returns the service registry. The registry will be created the first time
     * this method is invoked.
     */
    private static FactoryRegistry getServiceRegistry() {
        assert Thread.holdsLock(TJSDataAccessFinder.class);
        if (registry == null) {
            registry = new FactoryCreator(Arrays.asList(new Class<?>[]{TJSDataAccessFactory.class}));
        }
        return registry;
    }

    /**
     * Scans for factory plug-ins on the application class path. This method is
     * needed because the application class path can theoretically change, or
     * additional plug-ins may become available. Rather than re-scanning the
     * classpath on every invocation of the API, the class path is scanned
     * automatically only on the first invocation. Clients can call this method
     * to prompt a re-scan. Thus this method need only be invoked by
     * sophisticated applications which dynamically make new plug-ins available
     * at runtime.
     */
    public static synchronized void scanForPlugins() {
        TJSDataStoreFinder.scanForPlugins();
        getServiceRegistry().scanForPlugins();
    }

    /**
     * Resets the factory finder and prepares for a new full scan of the SPI subsystems
     */
    public static void reset() {
        FactoryRegistry copy = registry;
        registry = null;
        if (copy != null) {
            copy.deregisterAll();
        }
    }
}

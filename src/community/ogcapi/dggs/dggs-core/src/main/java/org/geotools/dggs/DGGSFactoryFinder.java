/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.dggs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.geotools.util.factory.FactoryCreator;
import org.geotools.util.factory.FactoryRegistry;

/** Locates the available {@link org.geotools.dggs.DGGSFactory} instances */
public class DGGSFactoryFinder {

    protected static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(DGGSFactoryFinder.class);

    /** The service registry for this manager. Will be initialized only when first needed. */
    private static volatile FactoryRegistry registry;

    // Singleton pattern
    private DGGSFactoryFinder() {}

    /**
     * Finds all implementations of {@link DGGSFactory} which have registered using the services
     * mechanism.
     *
     * @return An iterator over all discovered DGGSFactory.
     */
    public static synchronized Stream<DGGSFactory> getExtensionFactories() {
        return getServiceRegistry()
                .getFactories(DGGSFactory.class, null, null)
                .filter(DGGSFactory::isAvailable);
    }

    /**
     * Returns a DGGSFactory by the given identifier, if present and available
     *
     * @param factoryId
     * @return
     */
    public static synchronized Optional<DGGSFactory> getFactory(String factoryId) {
        return getExtensionFactories().filter(f -> factoryId.equals(f.getId())).findFirst();
    }

    /**
     * Returns the identifiers of all available factories
     *
     * @return
     */
    public static synchronized Stream<String> getFactoryIdentifiers() {
        return getExtensionFactories().map(f -> f.getId());
    }

    /**
     * Returns the service registry. The registry will be created the first time this method is
     * invoked.
     */
    private static FactoryRegistry getServiceRegistry() {
        assert Thread.holdsLock(DGGSFactoryFinder.class);
        if (registry == null) {
            registry =
                    new FactoryCreator(
                            Arrays.asList(
                                    new Class<?>[] {
                                        DGGSFactory.class,
                                    }));
        }
        return registry;
    }

    /**
     * Scans for factory plug-ins on the application class path. This method is needed because the
     * application class path can theoretically change, or additional plug-ins may become available.
     * Rather than re-scanning the classpath on every invocation of the API, the class path is
     * scanned automatically only on the first invocation. Clients can call this method to prompt a
     * re-scan. Thus this method need only be invoked by sophisticated applications which
     * dynamically make new plug-ins available at runtime.
     */
    public static synchronized void scanForPlugins() {
        getServiceRegistry().scanForPlugins();
    }

    /**
     * Returns a factory instance based on the factory id and its configuration parameters.
     *
     * @param factoryId
     * @param params
     * @return
     * @throws IOException
     */
    public static DGGSInstance createInstance(String factoryId, Map<String, ?> params)
            throws IOException {
        if (factoryId == null)
            throw new IllegalArgumentException("Cannot create a store with a missing factory id");

        DGGSFactory factory =
                DGGSFactoryFinder.getExtensionFactories()
                        .filter(f -> factoryId.equals(f.getId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Cannot find DGGS factory for id " + factoryId));
        return factory.createInstance(params);
    }
}

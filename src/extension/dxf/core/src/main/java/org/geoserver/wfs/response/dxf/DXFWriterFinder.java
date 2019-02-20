/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.dxf;

import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.factory.FactoryCreator;
import org.geotools.util.factory.FactoryRegistry;

/**
 * Enable programs to find all available DWFWriter implementations.
 *
 * <p>In order to be located by this finder writer must provide an implementation of the {@link
 * DXFWriter} interface.
 *
 * <p>In addition to implementing this interface writers should have a services file:<br>
 * <code>META-INF/services/org.geoserver.wfs.response.dxf.DXFWriter</code>
 *
 * <p>The file should contain a single line which gives the full name of the implementing class.
 *
 * <p>
 *
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 */
public final class DXFWriterFinder {
    protected static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(
                    "org.geoserver.wfs.response.dxf.DXFWriterFinder");

    /** The service registry for this manager. Will be initialized only when first needed. */
    private static FactoryRegistry registry;

    /**
     * Create a new DXFWriter instance for the requested version (null => any available version)
     *
     * @param version requested version compatibility
     */
    public static DXFWriter getWriter(String version, Writer writer) {
        FactoryRegistry writerRegistry = getServiceRegistry();
        Iterator<DXFWriter> it =
                writerRegistry.getFactories(DXFWriter.class, null, null).iterator();
        DXFWriter candidate;
        while (it.hasNext()) {
            candidate = it.next();
            LOGGER.log(Level.FINE, "Evaluating candidate: " + candidate.getDescription());
            if (candidate.supportsVersion(version)) {
                LOGGER.log(Level.FINE, "Chosen candidate: " + candidate.getDescription());
                return candidate.newInstance(writer);
            }
        }
        return null;
    }

    /**
     * Returns the service registry. The registry will be created the first time this method is
     * invoked.
     */
    private static FactoryRegistry getServiceRegistry() {
        if (registry == null) {
            registry = new FactoryCreator(Arrays.asList(new Class<?>[] {DXFWriter.class}));
        }
        return registry;
    }
}

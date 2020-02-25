/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;

/**
 * Saves/retrieves the importer configuration from System, Environment, Servlet context variables,
 * or a property file
 */
public class ImporterInfoDAO {

    static final Logger LOGGER = Logging.getLogger(ImporterInfoDAO.class);

    public static final String UPLOAD_ROOT_KEY = "importer.upload_root";

    public static final String MAX_ASYNCH_KEY = "importer.maxAsynch";

    public static final String MAX_SYNCH_KEY = "importer.maxSynch";

    /**
     * Reads the importer configuration from the specified resource, or returns a default
     *
     * @param resource The resource to read, or null in case
     */
    ImporterInfo read(Resource resource) throws IOException {
        ImporterInfoImpl info = new ImporterInfoImpl();
        read(resource, info);
        return info;
    }

    public void read(Resource resource, ImporterInfo info) throws IOException {
        // load the propery file
        Properties props = new Properties();
        if (resource != null && resource.getType() == Resource.Type.RESOURCE) {
            try (InputStream is = resource.in()) {
                props.load(is);
            }
        }
        // configure based on available CPUs
        int processors = Runtime.getRuntime().availableProcessors();
        info.setUploadRoot(getConfig(props, UPLOAD_ROOT_KEY, String.class, () -> null));
        info.setMaxAsynchronousImports(
                getConfig(props, MAX_ASYNCH_KEY, Integer.class, () -> processors));
        info.setMaxSynchronousImports(
                getConfig(props, MAX_SYNCH_KEY, Integer.class, () -> processors));
    }

    private <T> T getConfig(
            Properties props, String name, Class<T> clazz, Supplier<T> defaultValueSupplier) {
        String value = null;
        // check env/sys var overrides first
        try {
            value = GeoServerExtensions.getProperty(name);
        } catch (Throwable ex) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Could not access system property '" + name + "': " + ex);
            }
        }
        // check from property file if not found in env/sys
        if (props != null && value == null) {
            value = props.getProperty(name);
        }
        // convert to target type
        if (value != null) {
            T converted = Converters.convert(value, clazz);
            if (converted != null) {
                return converted;
            } else {
                LOGGER.log(
                        Level.FINEST,
                        "Could not parse the value  '" + value + "' for property " + name,
                        ", will use a default");
            }
        }
        // if all fails, use a default
        return defaultValueSupplier.get();
    }

    /** Writes the importer configuration form the specified resource */
    void write(ImporterInfo configuration, Resource resource) throws IOException {
        Properties props = new Properties();
        if (configuration.getUploadRoot() != null) {
            props.setProperty(UPLOAD_ROOT_KEY, configuration.getUploadRoot());
        }
        props.setProperty(MAX_SYNCH_KEY, String.valueOf(configuration.getMaxSynchronousImports()));
        props.setProperty(
                MAX_ASYNCH_KEY, String.valueOf(configuration.getMaxAsynchronousImports()));
        try (OutputStream os = resource.out()) {
            props.store(os, null);
        }
    }
}

/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.netcdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.imageio.netcdf.NetCDFUnitFormat;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.logging.Logging;

/** Re-configures the {@link NetCDFUnitFormat} on config reload */
public class NetCDFUnitsConfigurator implements GeoServerLifecycleHandler {

    static final Logger LOGGER = Logging.getLogger(NetCDFUnitsConfigurator.class);

    public static String NETCDF_UNIT_ALIASES = "NETCDF_UNIT_ALIASES";

    public static String NETCDF_UNIT_REPLACEMENTS = "NETCDF_UNIT_REPLACEMENTS";
    private final GeoServerResourceLoader resourceLoader;

    public NetCDFUnitsConfigurator(GeoServerResourceLoader resourceLoader) throws IOException {
        this.resourceLoader = resourceLoader;
        configure();
    }

    private void configure() {
        try {
            LinkedHashMap<String, String> aliases =
                    getMapResource(NETCDF_UNIT_ALIASES, NetCDFUnitFormat.NETCDF_UNIT_ALIASES);
            if (aliases != null) {
                NetCDFUnitFormat.setAliases(aliases);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load NetCDF unit aliases", e);
        }

        try {
            LinkedHashMap<String, String> replacements =
                    getMapResource(
                            NETCDF_UNIT_REPLACEMENTS, NetCDFUnitFormat.NETCDF_UNIT_REPLACEMENTS);
            if (replacements != null) {
                NetCDFUnitFormat.setReplacements(replacements);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load NetCDF unit replacements", e);
        }
    }

    /**
     * Searches for a config file with an absolute path, or inside the NetCDF data dir, or inside
     * the GeoServer data dir. Will return a map with the contents of the property file, with the
     * same order as the file contents.
     */
    private LinkedHashMap<String, String> getMapResource(
            String absolutePathProperty, String defaultFileName) throws IOException {
        Resource aliasResource = getResource(absolutePathProperty, defaultFileName);
        if (aliasResource != null) {
            try (InputStream is = aliasResource.in()) {
                return NetCDFUnitFormat.loadPropertiesOrdered(is);
            }
        }

        return null;
    }

    /**
     * Searches for a config file with an absolute path, or inside the NetCDF data dir, or inside
     * the GeoServer data dir. Will return a resource for the searched file, but only if it was
     * found, null otherwise.
     */
    private Resource getResource(String absolutePathProperty, String defaultFileName) {
        String source = GeoServerExtensions.getProperty(absolutePathProperty);
        // check the in path provided by the user, if any (the method called is null safe)
        Resource resource = getResourceForPath(source);
        // if not found search the NetCDF Data Directory
        if (resource == null && NetCDFUtilities.EXTERNAL_DATA_DIR != null) {
            source = new File(NetCDFUtilities.EXTERNAL_DATA_DIR, defaultFileName).getPath();
            resource = getResourceForPath(source);
        }
        // if still not found search the GeoServer Data Directory
        if (resource == null) {
            resource = resourceLoader.get(NetCDFUnitFormat.NETCDF_UNIT_ALIASES);
            if (resource.getType() != Resource.Type.RESOURCE) {
                resource = null;
            }
        }

        return resource;
    }

    /**
     * Gets a Resource from file system building the necessary wrappers (if the file is found), or
     * returns null instead.
     */
    private Resource getResourceForPath(String path) {
        Resource resource = null;
        if (path != null) {
            File resourceFile = new File(path);
            if (resourceFile.exists()) {
                FileSystemResourceStore store =
                        new FileSystemResourceStore(resourceFile.getParentFile());
                resource = store.get(resourceFile.getName());
            } else {
                LOGGER.fine("Could not locate " + path + ", moving on");
            }
        }

        return resource;
    }

    @Override
    public void onReset() {
        configure();
    }

    @Override
    public void onDispose() {
        // nothing to do
    }

    @Override
    public void beforeReload() {
        // better reload the unit config before reloading the catalog
        configure();
    }

    @Override
    public void onReload() {
        // nothing to do
    }
}

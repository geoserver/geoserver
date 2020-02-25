/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.IOException;
import java.util.List;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;

/**
 * A common interface which must be used by all GeoServer plugins. This will allow
 * GeoServerExtensions to retrieve all the available configurators and let GeoServer to be aware of
 * their extra-configuration files (usually properties files customized and specific for each plugin
 * - see control-flow, monitoring and so on...).
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public interface GeoServerPluginConfigurator {

    /** Get the list of Resources used by the plugin. */
    public List<Resource> getFileLocations() throws IOException;

    /**
     * Allows the plugin to store its configuration on the target {@link GeoServerResourceLoader}.
     * This way we delegate the plugin to save its configuration since it is the only on who knows
     * how to do it better.
     */
    public void saveConfiguration(GeoServerResourceLoader resourceLoader) throws IOException;

    /**
     * Allows the plugin to reload its configuration from the target {@link
     * GeoServerResourceLoader}. This way we delegate the plugin to load its configuration since it
     * is the only on who knows how to do it better.
     */
    public void loadConfiguration(GeoServerResourceLoader resourceLoader) throws IOException;
}

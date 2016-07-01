/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.IOException;
import java.util.List;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;

/**
 * A common interface which must be used by all GeoServer plugins.
 * This will allow GeoServerExtensions to retrieve all the available configurators and
 * let GeoServer to be aware of their extra-configuration files (usually properties
 * files customized and specific for each plugin - see control-flow, monitoring and so on...).
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public interface GeoServerPluginConfigurator {
    
    public List<Resource> getFileLocations() throws IOException;
    
    public void saveConfiguration(GeoServerResourceLoader resourceLoader) throws IOException;
    
    public void loadConfiguration(GeoServerResourceLoader resourceLoader) throws IOException;

}

/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore.cache;

import java.io.File;
import javax.servlet.ServletContext;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.web.context.ServletContextAware;

/**
 * Resource Caches that uses GeoServer Data Directory.
 *
 * @author NielsCharlier
 */
public class DataDirectoryResourceCache extends SimpleResourceCache implements ServletContextAware {

    public DataDirectoryResourceCache() {}

    @Override
    public void setServletContext(ServletContext servletContext) {
        String data = GeoServerResourceLoader.lookupGeoServerDataDirectory(servletContext);
        if (data != null) {
            this.base = new File(data);
        } else {
            throw new IllegalStateException("Unable to determine data directory");
        }
    }
}

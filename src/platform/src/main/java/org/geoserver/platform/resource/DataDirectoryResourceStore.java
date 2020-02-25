/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.Serializable;
import javax.servlet.ServletContext;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.web.context.ServletContextAware;

/**
 * ResourceStore using ServletContext to look up data directory.
 *
 * @see GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)
 * @author Jody Garnett (Boundless)
 */
public class DataDirectoryResourceStore extends FileSystemResourceStore
        implements ServletContextAware, Serializable {

    private static final long serialVersionUID = 5014766223630555410L;

    public DataDirectoryResourceStore() {
        // base directory obtained from servlet context
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        String data = GeoServerResourceLoader.lookupGeoServerDataDirectory(servletContext);
        if (data != null) {
            this.baseDirectory = new File(data);
        } else {
            throw new IllegalStateException("Unable to determine data directory");
        }
    }
}

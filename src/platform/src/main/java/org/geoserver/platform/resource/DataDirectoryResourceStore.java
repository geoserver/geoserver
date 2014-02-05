package org.geoserver.platform.resource;

import java.io.File;

import javax.servlet.ServletContext;

import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.web.context.ServletContextAware;

public class DataDirectoryResourceStore extends FileSystemResourceStore implements ServletContextAware {
    
    public DataDirectoryResourceStore(){
    }
    
    @Override
    public void setServletContext(ServletContext servletContext) {
        String data = GeoServerResourceLoader.lookupGeoServerDataDirectory( servletContext );
        if( data != null ){
            this.baseDirectory = new File( data );
        }
        else {
            throw new IllegalStateException("Unable to determine data directory");
        }
    }
}

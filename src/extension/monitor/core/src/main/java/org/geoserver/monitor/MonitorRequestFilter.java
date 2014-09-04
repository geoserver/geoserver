/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.util.AntPathMatcher;

import static org.geoserver.monitor.MonitorFilter.LOGGER;

public class MonitorRequestFilter {

    FileWatcher<List<Filter>> watcher;
    List<Filter> filters;
    
    public MonitorRequestFilter() {
        filters = new ArrayList();
    }
    
    public MonitorRequestFilter(GeoServerResourceLoader loader) throws IOException {
        //loader.findOrCreateDirectory("monitoring");
        
        File configFile = loader.find("monitoring", "filter.properties");
        if (configFile == null) {
            configFile = loader.createFile("monitoring", "filter.properties");
            loader.copyFromClassPath("filter.properties", configFile, getClass());
        }
        
        watcher = new FileWatcher<List<Filter>>(configFile) {
            @Override
            protected List<Filter> parseFileContents(InputStream in) throws IOException {
                List<Filter> filters = new ArrayList();
            
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                String line = null;
                while ((line = r.readLine()) != null) {
                    filters.add(new Filter(line));
                }
            
                return filters;
            }
        };
    }
    
    public boolean filter(HttpServletRequest req) throws IOException {
        if (watcher != null && watcher.isModified()) {
            synchronized (this) {
                if (watcher.isModified()) {
                    filters = watcher.read();
                }
            }
        }
         
        String path = req.getServletPath() + req.getPathInfo();
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Testing " + path + " for monitor filtering");
        }
        for (Filter f : filters) {
            if (f.matches(path)) {
                return true;
            }
        }
        
        return false;
    }
    
    static class Filter {
        
        AntPathMatcher matcher = new AntPathMatcher();
        String pattern;
     
        Filter(String pattern) {
            this.pattern = pattern;
        }
        
        boolean matches(String path) {
            return matcher.match(pattern, path);
        }
    }
}

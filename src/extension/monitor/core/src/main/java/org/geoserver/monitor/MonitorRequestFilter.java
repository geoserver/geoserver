/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static org.geoserver.monitor.MonitorFilter.LOGGER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.util.IOUtils;
import org.springframework.util.AntPathMatcher;

public class MonitorRequestFilter {

    FileWatcher<List<Filter>> watcher;
    List<Filter> filters;

    public MonitorRequestFilter() {
        filters = new ArrayList<Filter>();
    }

    public MonitorRequestFilter(GeoServerResourceLoader loader) throws IOException {
        Resource configFile = loader.get(Paths.path("monitoring", "filter.properties"));
        if (configFile.getType() == Type.UNDEFINED) {
            IOUtils.copy(getClass().getResourceAsStream("filter.properties"), configFile.out());
        }
        filters = new ArrayList<Filter>();
        watcher = new FilterPropertyFileWatcher(configFile);
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
        if (filters != null) {
            for (Filter f : filters) {
                if (f.matches(path)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** FileWatcher used to parse List<Filter> from text file. */
    private final class FilterPropertyFileWatcher extends FileWatcher<List<Filter>> {

        private FilterPropertyFileWatcher(Resource resource) {
            super(resource);
        }

        @Override
        protected List<Filter> parseFileContents(InputStream in) throws IOException {
            List<Filter> filters = new ArrayList<Filter>();

            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = r.readLine()) != null) {
                filters.add(new Filter(line));
            }

            return filters;
        }
    }

    /** Match path contents based on AntPathMatcher pattern */
    static class Filter {

        AntPathMatcher matcher = new AntPathMatcher();
        String pattern;

        /**
         * Filter based on request path.
         *
         * @param pattern AntPathMatcher pattern
         */
        Filter(String pattern) {
            this.pattern = pattern;
        }

        /**
         * Request path match.
         *
         * @param path Request path
         * @return Request path match
         */
        boolean matches(String path) {
            return matcher.match(pattern, path);
        }
    }
}

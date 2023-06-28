/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.data.ows.URLChecker;
import org.geotools.data.ows.URLCheckers;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

/**
 * Checks the provided URL is contained in data directory global styles folder, or a workspace
 * styles folder.
 */
public class StyleURLChecker implements URLChecker, DisposableBean {

    static final Logger LOGGER = Logging.getLogger(StyleURLChecker.class);

    private final GeoServerDataDirectory dd;

    public StyleURLChecker(GeoServerDataDirectory dd) {
        this.dd = dd;
        URLCheckers.register(this);
    }

    @Override
    public String getName() {
        return "DataDirectoryStyles";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean confirm(String location) {
        try {
            // strip out eventual file prefixes (the catalog can come up with file: alone)
            if (location.startsWith("file://")) location = location.substring(7);
            else if (location.startsWith("file:")) location = location.substring(5);

            // remove the URI query and fragment before handling as a file path
            location = location.split("\\?", 2)[0].split("#", 2)[0];

            File file = new File(location);

            // is it inside the data directory?
            Path resource = file.getCanonicalFile().toPath();
            Path dataDirectory = dd.getRoot().dir().getCanonicalFile().toPath();
            if (!resource.startsWith(dataDirectory)) return false;

            // get the path inside the data dir
            Path relative = dataDirectory.relativize(resource);

            // is it inside the global styles directory?
            if (relative.getNameCount() > 1 && relative.getName(0).toString().equals("styles"))
                return true;

            // is it inside a workspace then?
            if (relative.getNameCount() > 3
                    && relative.getName(0).toString().equals("workspaces")
                    && relative.getName(2).toString().equals("styles")) return true;

        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Unexpected error checking resource location", e);
        }

        return false;
    }

    @Override
    public void destroy() throws Exception {
        // necessary, otherwise the SPI will hold a reference to the bean across restarts/tests
        URLCheckers.deregister(this);
    }
}

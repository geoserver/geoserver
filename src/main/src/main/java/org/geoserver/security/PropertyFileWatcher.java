/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.LinkedProperties;

/**
 * A simple class to support reloadable property files. Watches last modified date on the specified file, and allows to
 * read a Properties out of it.
 *
 * @author Andrea Aime
 */
public class PropertyFileWatcher extends FileWatcher<Properties> {
    public PropertyFileWatcher(Resource resource) {
        super(resource);
    }

    /**
     * Read properties from file.
     *
     * @return properties from file, or null if file does not exist yet
     */
    public Properties getProperties() throws IOException {
        return read();
    }

    @Override
    protected Properties parseFileContents(InputStream in) throws IOException {
        Properties p = new LinkedProperties();
        p.load(in);
        return p;
    }

    public boolean isStale() {
        return isModified();
    }
}

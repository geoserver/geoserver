/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.geoserver.platform.FileWatcher;


/**
 * A simple class to support reloadable property files. Watches last modified
 * date on the specified file, and allows to read a Properties out of it.
 *
 * @author Andrea Aime
 *
 */
public class PropertyFileWatcher extends FileWatcher<Properties> {
    
    public PropertyFileWatcher(File file) {
        super(file);
    }

    public Properties getProperties() throws IOException {
        return read();
    }

    @Override
    protected Properties parseFileContents(InputStream in) throws IOException {
        Properties p = new Properties();
        p.load(in);
        return p;
    }
    
    public boolean isStale() {
        return isModified();
    }
}

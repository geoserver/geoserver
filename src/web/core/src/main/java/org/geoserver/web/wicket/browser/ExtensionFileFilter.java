/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;

/**
 * Returns only visible files whose name ends with the specified extension
 *
 * @author Andrea Aime
 */
@SuppressWarnings("serial")
public class ExtensionFileFilter implements FileFilter, Serializable {
    String[] extensions;

    /**
     * Builds a file filter for the specified extension
     *
     * @param extensions an extension, e.g., ".txt"
     */
    public ExtensionFileFilter(String... extensions) {
        this.extensions = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            this.extensions[i] = extensions[i].toUpperCase();
        }
    }

    public boolean accept(File pathname) {
        if (pathname.isFile()) {
            String name = pathname.getName().toUpperCase();
            for (String extension : extensions) {
                if (name.endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
        if (!pathname.isDirectory()) {
            return false;
        }
        if (pathname.isHidden()) {
            return false;
        }
        return true;
    }
}

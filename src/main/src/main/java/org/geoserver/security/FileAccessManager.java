/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.File;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.impl.DefaultFileAccessManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Provides the GUI, REST API and catalog checks with directives on what parts of the file system the current user can
 * access.
 */
public interface FileAccessManager {
    /**
     * Returns the file system roots available for the current user (or <code>null</code> if there are no restrictions)
     */
    public List<File> getAvailableRoots();

    /**
     * Returns the sandbox root directory, if there is one, or <code>null</code> if there is none (i.e., the user can
     * access the whole file system). This is used by the REST API to automatically prepend the sandbox root to the
     * uploaded file paths.
     */
    public File getSandbox();

    /**
     * Checks if the specified file is accessible in the context of the current request
     *
     * @param file the file to check
     */
    public boolean checkAccess(File file);

    /**
     * Looks up the {@link FileAccessManager} to use, preferring a custom implementation if available, otherwise falling
     * back on the default one. Mimics the behavior in {@link org.geoserver.security.SecureCatalogImpl}
     */
    public static FileAccessManager lookupFileAccessManager() {
        List<FileAccessManager> managers = GeoServerExtensions.extensions(FileAccessManager.class);
        if (managers.isEmpty()) throw new RuntimeException("Unexpected, no FileAdminAccessManager found");

        FileAccessManager manager = null;
        for (FileAccessManager resourceAccessManager : managers) {
            if (!DefaultFileAccessManager.class.equals(resourceAccessManager.getClass())) {
                manager = resourceAccessManager;
                break;
            }
        }

        // no custom manager found?
        if (manager == null) manager = managers.get(0);

        return manager;
    }

    /** Returns the current user authentication */
    default Authentication user() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}

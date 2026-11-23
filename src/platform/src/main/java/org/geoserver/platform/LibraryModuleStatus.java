/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;

/** Module status reporting the version of a library the module depends on, such as a JDBC driver. */
public class LibraryModuleStatus extends ModuleStatusImpl {

    public LibraryModuleStatus(String module, String name, String library, String libraryClass) {
        super(module, name);
        try {
            Version version = GeoTools.getVersion(Class.forName(libraryClass));
            setMessage(library + " version: " + (version == null ? "unknown" : version));
        } catch (ClassNotFoundException e) {
            setAvailable(false);
            setMessage(library + " not found");
        }
    }
}

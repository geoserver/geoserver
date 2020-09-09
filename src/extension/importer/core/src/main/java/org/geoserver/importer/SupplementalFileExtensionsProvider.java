/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.Set;

/**
 * A Class reporting a Set of file extensions for a given base extension. Additional implementations
 * may return additional file extensions (i.e. getting them from a Datadir or external file
 * definition).
 */
public interface SupplementalFileExtensionsProvider {

    /**
     * Return the set of supplemental file extensions available for the given base input extension
     */
    Set<String> getExtensions(String baseExtension);

    /** Check if this provider can handle the specified base input extension */
    boolean canHandle(String baseExtension);
}

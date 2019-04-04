/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Represents special capabilities that may not be supported by all catalog implementations.
 * Normally this capabilities should be defined at the catalog facade level but each catalog
 * implementation is free to provide is own set of capabilities.
 */
public final class CatalogCapabilities {

    private boolean supportsIsolatedWorkspaces = false;

    /**
     * If this method returns TRUE it means that isolated workspaces are supported.
     *
     * @return TRUE or FALSE depending on if isolated workspaces are supported or not
     */
    public boolean supportsIsolatedWorkspaces() {
        return supportsIsolatedWorkspaces;
    }

    /**
     * Specifies if isolated workspaces are supported or not.
     *
     * @param supportsIsolatedWorkspaces TRUE or FALSE, specifying if isolated workspaces are
     *     supported
     */
    public void setIsolatedWorkspacesSupport(boolean supportsIsolatedWorkspaces) {
        this.supportsIsolatedWorkspaces = supportsIsolatedWorkspaces;
    }
}

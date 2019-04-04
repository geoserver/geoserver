/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * A container of grouping for store objects.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface WorkspaceInfo extends CatalogInfo {

    /** The unique name of the workspace. */
    String getName();

    /** Sets the name of the workspace. */
    void setName(String name);

    /**
     * A persistent map of metadata.
     *
     * <p>Data in this map is intended to be persisted. Common case of use is to have services
     * associate various bits of data with a particular workspace.
     */
    MetadataMap getMetadata();

    default boolean isIsolated() {
        return false;
    }

    default void setIsolated(boolean isolated) {
        // nothing is done
    }
}

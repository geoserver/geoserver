/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Base interface for all catalog objects.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface CatalogInfo extends Info {

    /** Accepts a visitor. */
    void accept(CatalogVisitor visitor);

    /**
     * A flag indicating if the catalog object is enabled or not.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Sets the enabled flag for the catalog object.
     */
    default void setEnabled(boolean enabled) {}

    /**
     * Returns true if the catalog object existence should be advertised (true by default, unless
     * otherwise set).
     */
    default boolean isAdvertised() {
        return true;
    }

    /**
     * Set to true if the catalog object should be advertised, false otherwise.
     */
    default void setAdvertised(boolean advertised) {}
}

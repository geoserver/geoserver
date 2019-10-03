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
}

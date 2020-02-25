/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event;

import org.geoserver.catalog.CatalogInfo;

/**
 * Event for the removal of an object to the catalog.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface CatalogRemoveEvent extends CatalogEvent {

    /** the object that was removed. */
    CatalogInfo getSource();
}

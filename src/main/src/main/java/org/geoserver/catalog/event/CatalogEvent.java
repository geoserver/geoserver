/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event;

import org.geoserver.catalog.CatalogInfo;

/**
 * Catalog event.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface CatalogEvent {

    /**
     * The source of the event.
     */
    CatalogInfo getSource();

}

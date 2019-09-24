/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event;

import org.geoserver.catalog.CatalogInfo;

/** @author ImranR */
public interface CatalogBeforeAddEvent extends CatalogEvent {
    /** the object that is going to be added. */
    CatalogInfo getSource();
}

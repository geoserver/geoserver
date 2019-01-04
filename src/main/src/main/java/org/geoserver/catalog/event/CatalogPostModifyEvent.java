/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event;

import java.util.List;

/**
 * Event for the modification of an object in the catalog.
 *
 * <p>The {@link #getSource()} method returns the object modified. For access to the object before
 * it has been modified, see {@link CatalogModifyEvent}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface CatalogPostModifyEvent extends CatalogEvent {

    /** The names of the properties that were modified. */
    List<String> getPropertyNames();

    /** The old values of the properties that were modified. */
    List<Object> getOldValues();

    /** The new values of the properties that were modified. */
    List<Object> getNewValues();
}

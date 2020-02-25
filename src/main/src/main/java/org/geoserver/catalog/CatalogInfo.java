/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.Date;

/**
 * Base interface for all catalog objects.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface CatalogInfo extends Info {

    public static final String TIME_CREATED = "creationTime";
    public static final String TIME_MODIFIED = "modificationTime";

    /** Accepts a visitor. */
    void accept(CatalogVisitor visitor);

    /** default implementation for returning date of modification */
    default Date getDateModified() {
        return null;
    }

    /** default implementation for returning date of creation */
    default Date getDateCreated() {
        return null;
    }

    public default void setDateCreated(Date dateCreated) {
        // do nothing
    }

    /** @param dateModified the dateModified to set */
    default void setDateModified(Date dateModified) {
        // do nothing
    }
}

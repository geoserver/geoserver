/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;

/**
 * Interface implemented by all catalog and configuration objects.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface Info extends Serializable {

    /** Returns the identifier for the object. */
    String getId();
}

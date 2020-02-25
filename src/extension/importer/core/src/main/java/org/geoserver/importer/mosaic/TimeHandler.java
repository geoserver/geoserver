/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.mosaic;

import java.util.Date;
import java.util.Map;

/**
 * Handles timestamps for granules in a mosaic.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class TimeHandler implements java.io.Serializable {

    /** Initializes the handler with any properties. */
    public void init(Map<String, Object> properties) {}

    /** Extracts a timestamp from a mosaic granule. */
    public abstract Date computeTimestamp(Granule g);
}

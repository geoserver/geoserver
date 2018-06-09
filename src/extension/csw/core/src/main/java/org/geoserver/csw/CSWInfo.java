/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw;

import org.geoserver.config.ServiceInfo;

/**
 * CSW configuration
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface CSWInfo extends ServiceInfo {

    /**
     * Get the flag that determines the encoding of the CSW schemaLocation.
     *
     * <p>True if the CSW schemaLocation should refer to the canonical location, false if the CSW
     * schemaLocation should refer to a copy served by GeoServer.
     */
    boolean isCanonicalSchemaLocation();

    /**
     * Set the flag that determines the encoding of the CSW schemaLocation. True if the CSW
     * schemaLocation should refer to the canonical location, false if the CSW schemaLocation should
     * refer to a copy served by GeoServer.
     */
    void setCanonicalSchemaLocation(boolean canonicalSchemaLocation);
}

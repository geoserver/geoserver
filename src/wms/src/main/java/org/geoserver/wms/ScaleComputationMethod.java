/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

/**
 * Represents the different algorithms used to compute the scale denominator
 *
 * @author Andrea Aime - GeoSolutions
 */
public enum ScaleComputationMethod {
    /** The OGC mandated way, for interoperability */
    OGC,
    /**
     * Accurate, but not interoperable, not working against ill setup requests, but ideal for prints
     */
    Accurate
}

/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import org.geoserver.platform.Operation;

/**
 * Marks output format implementations as Complex Features compatible.
 *
 * @author Andrea Aime, Fernando Mino - GeoSolutions.
 */
public interface ComplexFeatureAwareFormat {

    /**
     * Returns true if the response supports complex features, or an attempt to cast down to simple
     * features should be made.
     */
    default boolean supportsComplexFeatures(Object value, Operation operation) {
        return true;
    }
}

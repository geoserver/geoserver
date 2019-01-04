/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import org.locationtech.jts.geom.Geometry;

/**
 * Estimates the size of a geometry
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GeometrySizeEstimator implements ObjectSizeEstimator {

    @Override
    public long getSizeOf(Object object) {
        if (object instanceof Geometry) {
            Geometry g = (Geometry) object;
            // super-lenient, just assuming double storage and no object overhead
            return 2 * g.getNumPoints() * 8;
        }

        return ObjectSizeEstimator.UNKNOWN_SIZE;
    }
}

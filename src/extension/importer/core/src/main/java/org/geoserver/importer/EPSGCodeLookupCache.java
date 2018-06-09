/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import org.geotools.referencing.CRS;
import org.geotools.util.SoftValueHashMap;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Caches expensive EPSG code lookups
 *
 * @author Andrea Aime - GeoSolutions
 */
class EPSGCodeLookupCache {

    /** Marker for failed lookups */
    static final Integer FAILED_LOOKUP = Integer.MIN_VALUE;

    /** The lookup cache */
    SoftValueHashMap<CoordinateReferenceSystem, Integer> cache =
            new SoftValueHashMap<CoordinateReferenceSystem, Integer>(100);

    public Integer lookupEPSGCode(CoordinateReferenceSystem crs) throws FactoryException {
        Integer code = cache.get(crs);
        if (code == null) {
            code = CRS.lookupEpsgCode(crs, true);
            if (code == null) {
                cache.put(crs, FAILED_LOOKUP);
            } else {
                cache.put(crs, code);
            }
        }

        if (FAILED_LOOKUP.equals(code)) {
            code = null;
        }

        return code;
    }
}

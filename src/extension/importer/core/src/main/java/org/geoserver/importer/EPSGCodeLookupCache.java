/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.geotools.util.SoftValueHashMap;

/**
 * Caches expensive EPSG code lookups
 *
 * @author Andrea Aime - GeoSolutions
 */
class EPSGCodeLookupCache {

    /** Marker for failed lookups */
    static final String FAILED_LOOKUP = "NOT_FOUND";

    /** The lookup cache */
    SoftValueHashMap<CoordinateReferenceSystem, String> cache = new SoftValueHashMap<>(100);

    public String lookupIdentifier(CoordinateReferenceSystem crs) throws FactoryException {
        String code = cache.get(crs);
        if (code == null) {
            code = CRS.lookupIdentifier(crs, true);
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

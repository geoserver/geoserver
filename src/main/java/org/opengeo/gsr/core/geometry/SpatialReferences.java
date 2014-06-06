/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package org.opengeo.gsr.core.geometry;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public final class SpatialReferences {
    private SpatialReferences() {
        throw new RuntimeException("No need to instantiate SpatialReferences, it has only static methods.");
    }
    
    public static SpatialReference fromCRS(CoordinateReferenceSystem crs) throws FactoryException {
        Integer epsgCode = CRS.lookupEpsgCode(crs, false);
        if (null != epsgCode) {
            return new SpatialReferenceWKID(epsgCode);
        } else {
            return new SpatialReferenceWKT(crs.toWKT());
        }
    }
}

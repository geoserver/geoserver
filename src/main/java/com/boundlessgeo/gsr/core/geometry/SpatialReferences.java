/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package com.boundlessgeo.gsr.core.geometry;

import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.util.GenericName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public final class SpatialReferences {
    private SpatialReferences() {
        throw new RuntimeException("No need to instantiate SpatialReferences, it has only static methods.");
    }

    public static SpatialReference fromCRS(CoordinateReferenceSystem crs) throws FactoryException {
        String epsgCode = CRS.lookupIdentifier(Citations.ESRI, crs, false);
        if (null == epsgCode) {
            epsgCode = CRS.lookupIdentifier(Citations.EPSG, crs, false);
        }
        if (null != epsgCode) {
            //strip off EPSG
            String code = epsgCode.substring(epsgCode.lastIndexOf(GenericName.DEFAULT_SEPARATOR) + 1);
            SpatialReferenceWKID sr = new SpatialReferenceWKID(Integer.parseInt(code));
            Integer latestWkid = latestWkid(crs);
            if (latestWkid != null) {
                sr.setLatestWkid(latestWkid);
            }
            return sr;
        } else {
            return new SpatialReferenceWKT(crs.toWKT());
        }
    }

    public static Integer latestWkid(CoordinateReferenceSystem crs) throws FactoryException {
        if (!CRS.isTransformationRequired(CRS.decode("EPSG:3857"), crs)) {
            return 3857;
        }
        if (!CRS.isTransformationRequired(CRS.decode("EPSG:4326"), crs)) {
            return 4326;
        }
        return null;
    }
}

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package com.boundlessgeo.gsr.translate.geometry;

import com.boundlessgeo.gsr.Utils;
import com.boundlessgeo.gsr.model.geometry.SpatialReference;
import com.boundlessgeo.gsr.model.geometry.SpatialReferenceWKID;
import com.boundlessgeo.gsr.model.geometry.SpatialReferenceWKT;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.util.GenericName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public final class SpatialReferences {

    public static int DEFAULT_WKID = 4326;

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
            /* TODO: Re-renable this once BSE is on GS 2.13.2 or newer
            Integer latestWkid = latestWkid(crs);
            if (latestWkid != null) {
                sr.setLatestWkid(latestWkid);
            }
            */
            return sr;
        } else {
            return new SpatialReferenceWKT(crs.toWKT());
        }
    }

    public static CoordinateReferenceSystem fromSpatialReference(SpatialReference sr) throws FactoryException {
        if (sr instanceof SpatialReferenceWKID) {
            return Utils.parseSpatialReference(String.valueOf(((SpatialReferenceWKID) sr).getWkid()));
        } else if (sr instanceof SpatialReferenceWKT) {
            return CRS.parseWKT(((SpatialReferenceWKT) sr).getWkt());
        } else {
            throw new IllegalArgumentException("Unknown SpatialReference class: "+ sr.getClass());
        }
    }

    /* TODO: Re-renable this once BSE is on GS 2.13.2 or newer
    public static Integer latestWkid(CoordinateReferenceSystem crs) throws FactoryException {
        if (!CRS.isTransformationRequired(CRS.decode("EPSG:3857"), crs)) {
            return 3857;
        }
        if (!CRS.isTransformationRequired(CRS.decode("EPSG:4326"), crs)) {
            return 4326;
        }
        return null;
    }
    */
}

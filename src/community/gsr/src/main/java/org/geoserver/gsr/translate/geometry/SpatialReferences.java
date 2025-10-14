/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.geometry;

import org.geoserver.gsr.Utils;
import org.geoserver.gsr.model.geometry.SpatialReference;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKID;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKT;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.util.GenericName;

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
            // strip off EPSG
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
        if (sr instanceof SpatialReferenceWKID iD) {
            return Utils.parseSpatialReference(String.valueOf(iD.getWkid()));
        } else if (sr instanceof SpatialReferenceWKT kT) {
            return CRS.parseWKT(kT.getWkt());
        } else {
            throw new IllegalArgumentException("Unknown SpatialReference class: " + sr.getClass());
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

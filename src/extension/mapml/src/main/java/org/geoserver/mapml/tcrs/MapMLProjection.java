/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import org.geoserver.mapml.xml.ProjType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;

/** Wrap the Projection. In case of a Built-in Projection, it will contain the ProjType enum. */
public class MapMLProjection {

    private boolean isBuiltIn;

    private ProjType projType;

    private String projection;

    public MapMLProjection(ProjType projType) {
        isBuiltIn = true;
        this.projType = projType;
        projection = projType.value();
    }

    public MapMLProjection(String proj) throws FactoryException {
        for (ProjType v : ProjType.values()) {
            if (v.name().equalsIgnoreCase(proj.toUpperCase())) {
                projType = v;
                isBuiltIn = true;
                break;
            }
        }
        int epsg = getEpsgCode(proj);
        for (ProjType c : ProjType.values()) {
            if (c.epsgCode == (epsg)) {
                projType = c;
                isBuiltIn = true;
                break;
            }
        }

        if (!isBuiltIn) {
            projection = proj;
            TiledCRS tiledCRS = getTiledCRS();
            if (tiledCRS == null) {
                throw new IllegalArgumentException(
                        "The following projection is not supported: " + proj);
            }
        }
    }

    private static int getEpsgCode(String codeWithPrefix) throws FactoryException {
        CoordinateReferenceSystem coordinateReferenceSystem = CRS.decode(codeWithPrefix, false);
        return CRS.lookupEpsgCode(coordinateReferenceSystem, true);
    }

    public boolean isBuiltIn() {
        return isBuiltIn;
    }

    public String getProjection() {
        return projection;
    }

    public String value() {
        return isBuiltIn ? projType.value() : projection;
    }

    public ProjType unwrap() {
        return projType;
    }

    public TiledCRS getTiledCRS() {
        return TiledCRSConstants.lookupTCRS(value());
    }

    public String getCRSCode() {
        if (isBuiltIn) {
            return projType.getCRSCode();
        }
        TiledCRSParams tcrs = TiledCRSConstants.lookupTCRSParams(value());
        return tcrs.getCode();
    }

    public CoordinateReferenceSystem getCRS() {
        return getTiledCRS().getCRS();
    }
}

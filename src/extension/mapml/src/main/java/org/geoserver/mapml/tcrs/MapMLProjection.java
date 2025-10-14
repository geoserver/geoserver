/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.mapml.xml.ProjType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;

/** Wrap the Projection. In case of a Built-in Projection, it will contain the ProjType enum. */
public class MapMLProjection {

    Set<String> BUILT_IN_PROJECTION = new HashSet<>(Arrays.asList(
            "EPSG:4326",
            "EPSG:3857",
            "EPSG:3978",
            "EPSG:5936",
            "MAPML:OSMTILE",
            "MAPML:APSTILE",
            "MAPML:CBMTILE",
            "MAPML:WGS84"));

    private boolean isBuiltIn;

    private ProjType projType;

    private String projection;

    public MapMLProjection(ProjType projType) {
        isBuiltIn = true;
        this.projType = projType;
        projection = projType.value();
    }

    public MapMLProjection(String proj) throws FactoryException {
        String projUC = proj.toUpperCase();
        for (ProjType v : ProjType.values()) {
            if (v.name().equalsIgnoreCase(projUC)) {
                projType = v;
                isBuiltIn = true;
                break;
            }
        }
        int epsg = getEpsgCode(proj);

        if (BUILT_IN_PROJECTION.contains(proj) || !projUC.startsWith("MAPML")) {
            for (ProjType c : ProjType.values()) {
                if (c.epsgCode == (epsg)) {
                    projType = c;
                    isBuiltIn = true;
                    break;
                }
            }
        }

        if (!isBuiltIn) {
            projection = proj;
            TiledCRS tiledCRS = getTiledCRS();
            if (tiledCRS == null) {
                throw new IllegalArgumentException("The following projection is not supported: " + proj);
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
        String id = value();
        String tcrsCode = null;
        TiledCRSParams tcrs = TiledCRSConstants.lookupTCRSParams(id);
        if (tcrs == null && id.startsWith("MAPML:")) {
            id = id.substring(6);
            // Fallback on id without MAPML prefix
            tcrs = TiledCRSConstants.lookupTCRSParams(id);
        }
        if (tcrs != null) {
            tcrsCode = tcrs.getCode();
        }
        return tcrsCode;
    }

    public CoordinateReferenceSystem getCRS() {
        return getTiledCRS().getCRS();
    }
}

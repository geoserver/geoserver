/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.util;

import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Class grouping methods to handle geometry union and intersection needed to merge limit together
 * when dealing with layer groups.
 */
public class GeomHelper {

    private static final Logger LOGGER = Logging.getLogger(GeomHelper.class);

    /**
     * Parse a wkt string to a Geometry object.
     *
     * @param wkt the WKT to be parsed, can be null.
     * @return the geometry corresponding to the wkt or null if none is passed.
     */
    public static Geometry parseWKT(String wkt) {
        Geometry result = null;
        if (wkt != null) {
            WKTReader wktReader = new WKTReader();
            try {
                if (wkt.contains("SRID")) {
                    String[] areaParts = wkt.split(";");
                    result = wktReader.read(areaParts[1]);
                    int srid = Integer.valueOf(areaParts[0].split("=")[1]);
                    result.setSRID(srid);
                } else {
                    result = wktReader.read(wkt);
                    result.setSRID(4326);
                }
            } catch (ParseException e) {
                throw new RuntimeException("Failed to unmarshal the area wkt", e);
            }
        }
        return result;
    }

    /**
     * Reproject and intersects two geometries.
     *
     * @param first the first geometry to merge.
     * @param second the other geometry as a WKT.
     * @param lessRestrictive if true when one of the geometry is null, null will be returned.
     *     Otherwise the other geometry will be returned.
     * @return the result of intersection.
     */
    public static Geometry reprojectAndUnion(
            Geometry first, Geometry second, boolean lessRestrictive) {
        BiFunction<Geometry, Geometry, Geometry> union = (g1, g2) -> g1.union(g2);
        if (lessRestrictive) return reprojectAndApplyOpFavourNull(first, second, union);
        else return reprojectAndApplyOperation(first, second, union);
    }

    /**
     * Reproject and intersects two geometries.
     *
     * @param first the first geometry to merge.
     * @param second the other geometry as a WKT.
     * @param favourNull if true when one of the geometry is null, null will be returned. Otherwise
     *     the other geometry will be returned.
     * @return the result of intersection.
     */
    public static Geometry reprojectAndIntersect(
            Geometry first, Geometry second, boolean favourNull) {
        BiFunction<Geometry, Geometry, Geometry> intersection = (g1, g2) -> g1.intersection(g2);
        if (favourNull) return reprojectAndApplyOpFavourNull(first, second, intersection);
        else return reprojectAndApplyOperation(first, second, intersection);
    }

    /**
     * Reproject and intersects two geometries. If one of the geoms is null, the other will be
     * returned.
     *
     * @return the result of intersection.
     */
    public static Geometry reprojectAndIntersect(Geometry g1, Geometry g2) {
        return reprojectAndIntersect(g1, g2, false);
    }

    /**
     * Reproject and intersects two geometries. If one of the two parameters is null returns null.
     *
     * @param first the first geometry to merge.
     * @param second the other geometry.
     * @param operation a bifunction with the operation to apply to the geometries.
     * @return the result of intersection.
     */
    public static Geometry reprojectAndApplyOpFavourNull(
            Geometry first, Geometry second, BiFunction<Geometry, Geometry, Geometry> operation) {
        if (first == null || second == null) return null;
        return reprojectAndApplyOperation(first, second, operation);
    }

    /**
     * Reproject and intersects two geometries.
     *
     * @param first the first geometry to merge.
     * @param second the other geometry as a WKT.
     * @param operation a BiFunction accepting Geometry parameters and returning a Geometry.
     * @return the result of intersection.
     */
    public static Geometry reprojectAndApplyOperation(
            Geometry first, Geometry second, BiFunction<Geometry, Geometry, Geometry> operation) {
        if (first == null) return second;
        if (second == null) return first;
        if (first.getSRID() != second.getSRID()) {
            try {
                CoordinateReferenceSystem target = CRS.decode("EPSG:" + first.getSRID(), true);
                CoordinateReferenceSystem source = CRS.decode("EPSG:" + second.getSRID(), true);
                MathTransform transformation = CRS.findMathTransform(source, target);
                second = JTS.transform(second, transformation);
                second.setSRID(first.getSRID());
            } catch (FactoryException | TransformException e) {
                throw new RuntimeException(
                        "Unable to intersect allowed areas: error during transformation from "
                                + second.getSRID()
                                + " to "
                                + first.getSRID());
            }
        }
        Geometry result = operation.apply(first, second);
        result.setSRID(first.getSRID());
        return result;
    }

    /**
     * Reproject a geometry to target CRS.
     *
     * @param geometry the geometry.
     * @param targetCRS the target CRS.
     * @return the reprojected geometry.
     */
    public static Geometry reprojectGeometry(
            Geometry geometry, CoordinateReferenceSystem targetCRS) {
        if (geometry == null) return null;

        try {
            CoordinateReferenceSystem geomCrs = CRS.decode("EPSG:" + geometry.getSRID(), true);
            if ((targetCRS != null) && !CRS.equalsIgnoreMetadata(geomCrs, targetCRS)) {
                MathTransform mt = CRS.findMathTransform(geomCrs, targetCRS, true);
                geometry = JTS.transform(geometry, mt);
                Integer srid = CRS.lookupEpsgCode(targetCRS, false);
                geometry.setSRID(srid);
            }
            return geometry;
        } catch (FactoryException | TransformException e) {
            LOGGER.log(Level.SEVERE, "Error while reprojecting geometry: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve CRS from a CatalogInfo object.
     *
     * @param info from which retrieve the crs.
     * @return the crs or null.
     */
    public static CoordinateReferenceSystem getCRSFromInfo(CatalogInfo info) {
        CoordinateReferenceSystem crs = null;
        if (info instanceof LayerInfo) {
            crs = ((LayerInfo) info).getResource().getCRS();
        } else if (info instanceof ResourceInfo) {
            crs = ((ResourceInfo) info).getCRS();
        } else if (info instanceof LayerGroupInfo) {
            crs = ((LayerGroupInfo) info).getBounds().getCoordinateReferenceSystem();
        } else {
            throw new RuntimeException(
                    "Cannot retrieve CRS from info " + info.getClass().getSimpleName());
        }
        return crs;
    }
}

/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

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
 * Class grouping methods to handle areas union and intersection needed to merge limit together when
 * dealing with layer groups.
 */
public class GeoFenceAreaHelper {

    private static final Logger LOGGER = Logging.getLogger(ContainerLimitResolver.class);

    /**
     * Parse a wkt string to a Geometry object.
     *
     * @param allowedArea the allowed area, can be null.
     * @return the geometry corresponding to the wkt or null if none is passed.
     */
    Geometry parseAllowedArea(String allowedArea) {
        Geometry result = null;
        if (allowedArea != null) {
            WKTReader wktReader = new WKTReader();
            try {
                if (allowedArea.indexOf("SRID") != -1) {
                    String[] allowedAreaParts = allowedArea.split(";");
                    result = wktReader.read(allowedAreaParts[1]);
                    int srid = Integer.valueOf(allowedAreaParts[0].split("=")[1]);
                    result.setSRID(srid);
                } else {
                    result = wktReader.read(allowedArea);
                    result.setSRID(4326);
                }
            } catch (ParseException e) {
                throw new RuntimeException("Failed to unmarshal the restricted area wkt", e);
            }
        }
        return result;
    }

    /**
     * Reproject if needed and perform union of two geometries.
     *
     * @param first the first geometry to merge.
     * @param second the other geometry to merge.
     * @param lessRestrictive if set to true if one of the two geometries is null, null will be
     *     returned. If false the other geometry will be returned.
     * @return the result of union.
     */
    Geometry reprojectAndUnion(Geometry first, Geometry second, boolean lessRestrictive) {
        if (lessRestrictive) return reprojectAndUnionLessRestrictive(first, second);
        else return reprojectAndUnion(first, second);
    }

    /**
     * Reproject and union two geometries. If one of the geometries is null returns null.
     *
     * @param first the first geometry to merge.
     * @param second the other geometry to merge.
     * @return the result of union.
     */
    Geometry reprojectAndUnionLessRestrictive(Geometry first, Geometry second) {
        if (first == null || second == null) return null;

        Geometry result = reprojectAndUnion(first, second);
        return result;
    }

    /**
     * Reproject and union two geometries. If one of the two geometries is null, returns the other
     * one.
     *
     * @param first the first geometry to merge.
     * @param second the other geometry to merge.
     * @return the result of union.
     */
    Geometry reprojectAndUnion(Geometry first, Geometry second) {
        if (first == null) return second;
        if (second == null) return first;
        if (first.getSRID() != second.getSRID()) {
            try {
                CoordinateReferenceSystem target = CRS.decode("EPSG:" + first.getSRID());
                CoordinateReferenceSystem source = CRS.decode("EPSG:" + second.getSRID());
                MathTransform transformation = CRS.findMathTransform(source, target);
                second = JTS.transform(second, transformation);
                second.setSRID(first.getSRID());
            } catch (FactoryException e) {
                throw new RuntimeException(
                        "Unable to merge allowed areas: can't reproject from "
                                + second.getSRID()
                                + " to "
                                + first.getSRID());
            } catch (TransformException e) {
                throw new RuntimeException(
                        "Unable to merge allowed areas: error during transformation from "
                                + second.getSRID()
                                + " to "
                                + first.getSRID());
            }
        }
        Geometry result = first.union(second);
        result.setSRID(first.getSRID());
        return result;
    }

    /**
     * Reproject and intersects two geometries. If one of the two parameters is null returns null.
     *
     * @param first the first geometry to merge.
     * @param secondWKT the other geometry as a WKT.
     * @return the result of intersection.
     */
    Geometry reprojectAndIntersectLessRestrictive(Geometry first, String secondWKT) {
        if (first == null || secondWKT == null) return null;
        Geometry second = parseAllowedArea(secondWKT);
        return reprojectAndIntersect(first, second);
    }

    /**
     * Reproject and intersects two geometries. If one of the two parameters is null returns the
     * other.
     *
     * @param first the first geometry to merge.
     * @param secondWKT the other geometry as a WKT.
     * @return the result of intersection.
     */
    Geometry reprojectAndIntersect(Geometry first, String secondWKT) {
        Geometry second = parseAllowedArea(secondWKT);
        return reprojectAndIntersect(first, second);
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
    Geometry reprojectAndIntersect(Geometry first, String second, boolean lessRestrictive) {
        if (lessRestrictive) return reprojectAndIntersectLessRestrictive(first, second);
        else return reprojectAndIntersect(first, second);
    }

    /**
     * Reproject and intersects two geometries.
     *
     * @param first the first geometry to merge.
     * @param second the other geometry as a WKT.
     * @return the result of intersection.
     */
    Geometry reprojectAndIntersect(Geometry first, Geometry second) {
        if (first == null) return second;
        if (second == null) return first;
        if (first.getSRID() != second.getSRID()) {
            try {
                CoordinateReferenceSystem target = CRS.decode("EPSG:" + first.getSRID());
                CoordinateReferenceSystem source = CRS.decode("EPSG:" + second.getSRID());
                MathTransform transformation = CRS.findMathTransform(source, target);
                second = JTS.transform(second, transformation);
                second.setSRID(first.getSRID());
            } catch (FactoryException e) {
                throw new RuntimeException(
                        "Unable to interserct allowed areas: can't reproject from "
                                + second.getSRID()
                                + " to "
                                + first.getSRID());
            } catch (TransformException e) {
                throw new RuntimeException(
                        "Unable to intersect allowed areas: error during transformation from "
                                + second.getSRID()
                                + " to "
                                + first.getSRID());
            }
        }
        Geometry result = first.intersection(second);
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
    Geometry reprojectGeometry(Geometry geometry, CoordinateReferenceSystem targetCRS) {
        try {
            CoordinateReferenceSystem geomCrs = CRS.decode("EPSG:" + geometry.getSRID());
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
    CoordinateReferenceSystem getCrsFromInfo(CatalogInfo info) {
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

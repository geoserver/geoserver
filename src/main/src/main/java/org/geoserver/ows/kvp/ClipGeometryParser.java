/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import static org.geoserver.platform.ServiceException.INVALID_PARAMETER_VALUE;

import java.util.regex.Pattern;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.platform.ServiceException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.gml2.SrsSyntax;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;

/**
 * Utility class to parse the CLIP vendor parameter used by WMS and WCS. It's not a KVP parser because the request
 * objects do not always have a "clip" property (WCS would require a "clip" property in the EMF models)
 */
public class ClipGeometryParser {

    private static final Pattern SRID_REGEX = Pattern.compile("SRID=[0-9].*");

    private static final WKTReader2 reader = new WKTReader2();

    /**
     * Reads a geometry from a WKT string, and transforms it to the target CRS if needed. In case the targetCRS is
     * flipped (north-east axis order), the geometry is transformed to east-north order. When available, the SRID is
     * parsed from the WKT string and used to set the geometry's CRS, otherwise the targetCRS is used. If both are
     * present, reprojection from the geometry's CRS to the targetCRS is performed.
     */
    public static synchronized Geometry readGeometry(final String wkt, CoordinateReferenceSystem targetCRS)
            throws Exception {
        String[] wktContents = wkt.split(";");
        Geometry geom = getGeometry(wktContents[wktContents.length - 1]);
        if (!(geom.getClass().isAssignableFrom(Polygon.class) || geom.getClass().isAssignableFrom(MultiPolygon.class)))
            throw new ServiceException("Clip must be a polygon or multipolygon", INVALID_PARAMETER_VALUE, "clip");
        // parse SRID if passed, looking for a pattern srid=4326:Polygon(...)
        CoordinateReferenceSystem geomCRS = null;
        if (wktContents.length == 2
                && SRID_REGEX.matcher(wktContents[0].toUpperCase()).matches()) {
            String sridString = wktContents[0].split("=")[1];
            // force xy. Forcing EPSG in this case is legit, as EWKT does not advertise an authority
            geomCRS = CRS.decode("EPSG:" + sridString, true);
            geom.setUserData(geomCRS);
        }

        if (targetCRS != null) {
            // geometry is used for clipping, internal handling is all in east/north order
            if (CRS.getAxisOrder(targetCRS) == CRS.AxisOrder.NORTH_EAST) {
                String id = ResourcePool.lookupIdentifier(targetCRS, false);
                targetCRS = CRS.decode(SrsSyntax.AUTH_CODE.getSRS(id));
            }
            if (geomCRS != null) {
                if (CRS.isTransformationRequired(targetCRS, geomCRS)) {
                    MathTransform transform = CRS.findMathTransform(geomCRS, targetCRS);
                    geom = JTS.transform(geom, transform);
                    geom.setUserData(targetCRS);
                }
            } else {
                geom.setUserData(targetCRS);
            }
        }

        return geom;
    }

    private static Geometry getGeometry(String wkt) {
        try {
            return reader.read(wkt);
        } catch (ParseException e) {
            throw new ServiceException("Invalid WKT syntax", INVALID_PARAMETER_VALUE, "clip");
        }
    }
}

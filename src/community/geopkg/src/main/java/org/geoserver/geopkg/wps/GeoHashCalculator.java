/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import ch.hsr.geohash.GeoHash;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Calculates GeoHashes for a geometry, in a spirit similar to PostGIS ST_GeoHash, but with
 * different results due to a different resolution calculation
 */
class GeoHashCalculator {

    /** Default calculator, uses not math transform */
    public static GeoHashCalculator DEFAULT = new GeoHashCalculator();

    private static final int MAX_PRECISION = 12;
    MathTransform mt;

    protected GeoHashCalculator(MathTransform mt) {
        this.mt = mt;
    }

    protected GeoHashCalculator() {}

    public static GeoHashCalculator get(CoordinateReferenceSystem crs) throws FactoryException {
        if (crs != null) {
            MathTransform mt = CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84);
            if (!mt.isIdentity()) {
                return new GeoHashCalculator(mt);
            }
        }
        return DEFAULT;
    }

    public String compute(Geometry geom) throws TransformException {
        if (mt != null) {
            geom = JTS.transform(geom, mt);
        }

        Point centroid = geom.getCentroid();
        int precision = getPrecision(geom);
        return GeoHash.geoHashStringWithCharacterPrecision(
                centroid.getY(), centroid.getX(), precision);
    }

    /**
     * Precision detection is just based on the size, so it's a bit different than the PostGIS
     * algorithm, but tolerant to geometries simmetrical to the Greenwitch meridian
     */
    private int getPrecision(Geometry geom) {
        Envelope envelope = geom.getEnvelopeInternal();
        double width = envelope.getWidth();
        double height = envelope.getHeight();
        int precision = getPrecision(height, 180);
        precision += getPrecision(width, 360);

        return Math.min(Math.max(precision / 5, 1), MAX_PRECISION);
    }

    private int getPrecision(double side, double reference) {
        int precision = 0;
        while (reference > side) {
            reference /= 2;
            precision++;
        }
        return precision;
    }
}

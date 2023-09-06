/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.NoninvertibleTransformException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;

/*
 * The Projection class supplies projection/unprojection transformations for known projections
 */

/** @author prushforth */
public class Projection {

    protected CoordinateReferenceSystem crs;
    protected CoordinateReferenceSystem baseCRS;
    protected MathTransform toProjected;
    protected MathTransform toLatLng;

    /** @param code */
    public Projection(String code) {
        try {
            this.crs = CRS.decode(code);
            this.baseCRS =
                    CRS.getProjectedCRS(crs) != null
                            ? CRS.getProjectedCRS(crs).getBaseCRS()
                            : this.crs;
            this.toProjected = CRS.findMathTransform(this.baseCRS, crs);
            this.toLatLng = this.toProjected.inverse();
        } catch (FactoryException | NoninvertibleTransformException ex) {
            Logger.getLogger(Projection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param latlng
     * @return
     * @throws MismatchedDimensionException - MismatchedDimensionException
     * @throws TransformException - TransformException
     */
    public Point project(LatLng latlng) throws MismatchedDimensionException, TransformException {
        if (toProjected.isIdentity()) {
            return new Point(latlng.lng, latlng.lat);
        }
        Position2D projected = new Position2D(crs);
        toProjected.transform(new Position2D(this.baseCRS, latlng.lat, latlng.lng), projected);
        return new Point(projected.x, projected.y);
    }

    /**
     * @param p
     * @return
     * @throws MismatchedDimensionException - MismatchedDimensionException
     * @throws TransformException - TransformException
     */
    public LatLng unproject(Point p) throws MismatchedDimensionException, TransformException {
        if (toLatLng.isIdentity()) {
            return new LatLng(p.y, p.x);
        }
        Position2D unprojected = new Position2D(this.baseCRS);
        toLatLng.transform(new Position2D(this.crs, p.x, p.y), unprojected);
        return new LatLng(unprojected.getOrdinate(0), unprojected.getOrdinate(1));
    }

    /** @return */
    public CoordinateReferenceSystem getCRS() {
        return this.crs;
    }
}

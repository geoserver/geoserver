/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

/*
 * The Projection class supplies projection/unprojection transformations for known projections
 */
public class Projection {

    protected CoordinateReferenceSystem crs;
    protected CoordinateReferenceSystem baseCRS;
    protected MathTransform toProjected;
    protected MathTransform toLatLng;

    public Projection(String code) {
        try {
            this.crs = CRS.decode(code);
            this.baseCRS =
                    CRS.getProjectedCRS(crs) != null
                            ? CRS.getProjectedCRS(crs).getBaseCRS()
                            : this.crs;
            this.toProjected = CRS.findMathTransform(this.baseCRS, crs);
            this.toLatLng = this.toProjected.inverse();
        } catch (FactoryException ex) {
            Logger.getLogger(Projection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(Projection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Point project(LatLng latlng) throws MismatchedDimensionException, TransformException {
        if (toProjected.isIdentity()) {
            return new Point(latlng.lng, latlng.lat);
        }
        DirectPosition2D projected = new DirectPosition2D(crs);
        toProjected.transform(
                new DirectPosition2D(this.baseCRS, latlng.lat, latlng.lng), projected);
        return new Point(projected.x, projected.y);
    }

    public LatLng unproject(Point p) throws MismatchedDimensionException, TransformException {
        if (toLatLng.isIdentity()) {
            return new LatLng(p.y, p.x);
        }
        DirectPosition2D unprojected = new DirectPosition2D(this.baseCRS);
        toLatLng.transform(new DirectPosition2D(this.crs, p.x, p.y), unprojected);
        return new LatLng(unprojected.getOrdinate(0), unprojected.getOrdinate(1));
    }

    public CoordinateReferenceSystem getCRS() {
        return this.crs;
    }
}

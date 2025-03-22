/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Point;

class DistanceSlopeCalculator {

    private GeodeticCalculator gc;
    private Point current;
    private Point previous;
    private final CoordinateReferenceSystem projection;
    private double distance;
    private double slope;

    public DistanceSlopeCalculator(CoordinateReferenceSystem projection) {
        this.projection = projection;
        if (projection instanceof GeographicCRS) gc = new GeodeticCalculator(projection);
    }

    public void next(Point next, double altitude) throws TransformException {
        if (current != null) previous = current;
        current = next;

        if (previous != null) {
            if (gc != null) {
                gc.setStartingPosition(JTS.toDirectPosition(previous.getCoordinate(), projection));
                gc.setDestinationPosition(JTS.toDirectPosition(current.getCoordinate(), projection));
                distance = gc.getOrthodromicDistance();
            } else {
                distance = previous.distance(current);
            }

            // calculate slope percentage
            slope = altitude * 100 / distance;
        }
    }

    public double getDistance() {
        return distance;
    }

    public double getSlope() {
        return slope;
    }

    public CoordinateReferenceSystem getProjection() {
        return projection;
    }
}

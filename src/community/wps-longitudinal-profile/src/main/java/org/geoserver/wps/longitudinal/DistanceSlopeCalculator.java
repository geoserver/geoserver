/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.geoserver.wps.WPSException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import si.uom.SI;

class DistanceSlopeCalculator {

    private GeodeticCalculator gc;
    private Point current;
    private Point previous;
    private double previousAltitude;
    private double currentAltitude;
    private final CoordinateReferenceSystem projection;
    private final boolean projectedDistance;
    private double distance;
    private double slope;
    private double unitsPerMeter = 1;
    private double metersPerAltitudeUnit = 1;

    /**
     * Measures points in the coverage CRS, reporting distances in units of {@code projection}. When
     * {@code projectedDistance} is set, measures points in {@code projection} on its plane instead, failing with a
     * {@link WPSException} if it is not a projected CRS.
     */
    public DistanceSlopeCalculator(
            GridCoverage2D coverage,
            int altitudeIndex,
            CoordinateReferenceSystem projection,
            boolean projectedDistance) {
        this.projection = projection;
        this.projectedDistance = projectedDistance;
        // measure in the coverage CRS so that the target projection cannot stretch the distances
        CoordinateReferenceSystem sourceCRS = coverage.getCoordinateReferenceSystem2D();
        if (projectedDistance) {
            // a Euclidean distance in degrees is not a distance
            if (!(projection instanceof ProjectedCRS))
                throw new WPSException(
                        "Projected distances need a projected targetProjection, got " + projection.getName());
            metersPerAltitudeUnit = getMetersPerAltitudeUnit(coverage, altitudeIndex);
        } else if (sourceCRS instanceof GeographicCRS || sourceCRS instanceof ProjectedCRS) {
            gc = new GeodeticCalculator(sourceCRS);
            // the geodesic run is in meters, the altitudes must be too
            metersPerAltitudeUnit = getMetersPerAltitudeUnit(coverage, altitudeIndex);
        }
        // report in the projected CRS unit (e.g. feet), degrees are not a ground unit so keep meters
        if (projection instanceof ProjectedCRS) unitsPerMeter = getUnitsPerMeter(projection);
    }

    /** Returns how many CRS axis units make a meter, 1 when the axis has no unit. */
    static double getUnitsPerMeter(CoordinateReferenceSystem crs) {
        Unit<?> unit = crs.getCoordinateSystem().getAxis(0).getUnit();
        if (unit == null) return 1;
        if (!unit.isCompatible(SI.METRE))
            throw new WPSException("Cannot measure distances, axis unit is not a length in " + crs.getName());
        return SI.METRE.getConverterTo(unit.asType(Length.class)).convert(1d);
    }

    /** Returns the meters in an altitude unit: the band unit when a length, else the projected CRS one, else 1. */
    static double getMetersPerAltitudeUnit(GridCoverage2D coverage, int altitudeIndex) {
        Unit<?> unit = coverage.getSampleDimension(altitudeIndex).getUnits();
        if (unit != null && unit.isCompatible(SI.METRE))
            return unit.asType(Length.class).getConverterTo(SI.METRE).convert(1d);
        // like GDAL, without a vertical unit assume the horizontal one, meters for degrees
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
        return crs instanceof ProjectedCRS ? 1 / getUnitsPerMeter(crs) : 1;
    }

    /** Sets the calculator destination, failing with a {@link WPSException} when the CRS cannot place the point. */
    static void setDestination(GeodeticCalculator gc, Coordinate c) {
        CoordinateReferenceSystem crs = gc.getCoordinateReferenceSystem();
        try {
            gc.setDestinationPosition(JTS.toDirectPosition(c, crs));
        } catch (IllegalArgumentException | TransformException e) {
            // typically a geometry without CRS whose coordinates are not in the coverage one
            throw new WPSException("Cannot measure distances, point " + c + " is not valid in " + crs.getName(), e);
        }
    }

    public void next(Point next, double altitude) throws TransformException {
        if (current != null) {
            previous = current;
            previousAltitude = currentAltitude;
        }
        current = next;
        currentAltitude = altitude;

        if (gc != null) {
            // the previous destination becomes the start, so each point is projected to geographic once
            if (previous != null) gc.setStartingGeographicPoint(gc.getDestinationGeographicPoint());
            setDestination(gc, current.getCoordinate());
        }

        if (previous != null) {
            // slope compares run and altitudes in the same unit, before conversion to the reporting unit
            double run = gc != null ? gc.getOrthodromicDistance() : previous.distance(current);
            distance = gc != null ? run * unitsPerMeter : run;
            // the planar run is in projection units, bring it to meters like the altitudes
            if (projectedDistance) run /= unitsPerMeter;

            // calculate slope percentage, zero on coincident points (e.g. lon spread at a pole) rather than NaN
            slope = run == 0 ? 0 : (currentAltitude - previousAltitude) * metersPerAltitudeUnit * 100 / run;
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

    /** Returns true when {@link #next} expects points in {@link #getProjection()}, false for coverage CRS ones. */
    public boolean isProjectedDistance() {
        return projectedDistance;
    }
}

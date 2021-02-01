/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

/** Wraps the geometry of DGGS zones crossing the dateline TODO: handle the pole case too */
public class ZoneWrapper {

    private static final double EPS_AREA = 1e-12;

    public enum DatelineLocation {
        /** Not crossing the dateline */
        NotCrossing,
        /** Crossing the dateline, majority of points in the west empisphere */
        West,
        /** Crossing the dateline, majority of points in the east empisphere */
        East
    };

    /**
     * Wraps a dateline crossing polygon so that its longitudes are all packed on one side. Will not
     * modify other geometries.
     *
     * @param cs The coordinate sequence representing the polygon outer shell.
     * @return The update coordinate sequence (might be the same as the input CS)
     */
    public static CoordinateSequence wrap(CoordinateSequence cs) {
        DatelineLocation location = isDatelineCrossing(cs);

        if (location != DatelineLocation.NotCrossing) {
            int size = cs.size();
            for (int i = 0; i < size; i++) {
                double lng = cs.getOrdinate(i, 0);
                double offset = getOffset(location, lng);
                if (offset != 0) cs.setOrdinate(i, 0, lng + offset);
            }
        }

        return cs;
    }

    public static CoordinateSequence includePole(
            GeometryFactory gf, CoordinateSequence cs, boolean north) {
        List<Coordinate> coordinates = new ArrayList<>(Arrays.asList(cs.toCoordinateArray()));
        Collections.sort(coordinates, (o1, o2) -> (int) Math.signum(o1.x - o2.x));
        Coordinate low = coordinates.get(0);
        Coordinate high = coordinates.get(coordinates.size() - 1);
        // the polygon is polar, needs to go from dateline to dateline
        if (low.x > -180 || high.x < 180) {
            if (low.x > -180) {
                Coordinate shifted = new Coordinate(high.x - 360, high.y);
                LineSegment missingPortion = new LineSegment(shifted, low);
                LineSegment dateline =
                        new LineSegment(new Coordinate(-180, -90), new Coordinate(-180, 90));
                Coordinate intersection = missingPortion.intersection(dateline);
                coordinates.add(0, intersection);
                if (high.x < 180) {
                    coordinates.add(coordinates.size(), new Coordinate(180, intersection.y));
                }
            } else {
                // we know the first point touches the dateline at -180,
                // so just need to roll and add
                coordinates.add(coordinates.size() - 1, new Coordinate(180, low.y));
            }
        }

        // close the polygon around the poe
        double poleLatitude = north ? 90 : -90;
        int cl = coordinates.size();
        coordinates.add(cl, new Coordinate(180, poleLatitude));
        coordinates.add(cl + 1, new Coordinate(-180, poleLatitude));
        coordinates.add(cl + 2, new Coordinate(-180, coordinates.get(0).y));

        return gf.getCoordinateSequenceFactory()
                .create(coordinates.toArray(new Coordinate[coordinates.size()]));
    }

    public static CoordinateSequence getUnionSequence(
            GeometryFactory gf, CoordinateSequence cs, Polygon polarPoly) {
        Polygon poly = gf.createPolygon(cs);
        CoordinateSequence csUnion;
        if (poly.getArea() < EPS_AREA) {
            csUnion = polarPoly.getExteriorRing().getCoordinateSequence();
        } else {
            Geometry union = union(polarPoly, poly);
            csUnion = ((Polygon) union.getGeometryN(0)).getExteriorRing().getCoordinateSequence();
        }
        return gf.getCoordinateSequenceFactory().create(csUnion);
    }

    public static Geometry union(Polygon polarPoly, Polygon poly) {
        Geometry result = null;
        try {
            result = polarPoly.union(poly);
        } catch (Exception e1) {
            // try a precision reduction approach, starting from mm and scaling up to km
            double precision = 1e-3 / 100000; // 1 degree roughly 100km
            // from mm to km
            for (int i = 0; i < 6; i++) {
                GeometryPrecisionReducer reducer =
                        new GeometryPrecisionReducer(new PrecisionModel(1 / precision));
                Geometry reducedPolarPoly = reducer.reduce(polarPoly);
                Geometry reducedPoly = reducer.reduce(poly);
                try {
                    result = reducedPolarPoly.union(reducedPoly);
                    break;
                } catch (Exception e3) {
                    precision *= 10;
                }
            }

            if (result == null) {
                throw new RuntimeException(
                        "Failed to union geometries, even with precision reduction");
            }
        }

        return result;
    }

    /**
     * Checks if the geometry crosses the dateline or not
     *
     * @param cs The coordinate sequence backing the geometry
     * @return The relative position of the geometry to the dateline
     */
    public static DatelineLocation isDatelineCrossing(CoordinateSequence cs) {
        int eastCount = 0;
        int westCount = 0;
        boolean datelineCrossing = false;

        int size = cs.size();
        double prevLng = cs.getX(0);
        // the cs represents a ring, avoid considering the start points twice
        for (int i = 0; i < size - 1; i++) {
            double lng = cs.getX(i);
            if (lng - prevLng > 180 || lng - prevLng < -180) {
                datelineCrossing = true;
            }
            if (lng < 0) {
                westCount++;
            } else {
                eastCount++;
            }
            prevLng = lng;
        }

        if (datelineCrossing)
            return eastCount > westCount ? DatelineLocation.East : DatelineLocation.West;
        else return DatelineLocation.NotCrossing;
    }

    private static double getOffset(DatelineLocation location, double lng) {
        double offset = 0;
        if (location == DatelineLocation.East && lng < 0) offset = 360;
        if (location == DatelineLocation.West && lng > 0) offset = -360;
        return offset;
    }

    interface AggregatePointSelector {
        public Coordinate select(Coordinate curr, Coordinate selected);
    }
}

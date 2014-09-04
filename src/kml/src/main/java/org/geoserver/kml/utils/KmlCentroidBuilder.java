/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

/**
 * A KML specific geometry centroid extractor
 */
public class KmlCentroidBuilder {

    /**
     * Returns the centroid of the geometry, handling a geometry collection.
     * <p>
     * In the case of a collection a multi point containing the centroid of each geometry in the
     * collection is calculated. The first point in the multi point is returned as the controid.
     * </p>
     */
    public Coordinate geometryCentroid(Geometry g) {
        if (g instanceof GeometryCollection) {
            g = selectRepresentativeGeometry((GeometryCollection) g);
        }

        if (g == null) {
            return null;
        } else if (g instanceof Point) {
            // simple case
            return g.getCoordinate();
        } else if (g instanceof LineString) {
            // make sure the point we return is actually on the line
            LineString line = (LineString) g;
            LengthIndexedLine lil = new LengthIndexedLine(line);
            return lil.extractPoint(line.getLength() / 2.0);
        } else {
            // return the actual centroid
            return g.getCentroid().getCoordinate();
        }
    }

    /**
     * Selects a representative geometry from the collection (the one covering the biggest area)
     * 
     * @param g
     * @return
     */
    private Geometry selectRepresentativeGeometry(GeometryCollection g) {
        GeometryCollection gc = (GeometryCollection) g;

        if (gc.isEmpty()) {
            return null;
        }

        // check for case of single geometry or multipoint
        Geometry first = gc.getGeometryN(0);
        if (gc.getNumGeometries() == 1 || g instanceof MultiPoint) {
            return first;
        } else {
            // get the geometry with the largest bbox
            double maxAreaSoFar = first.getEnvelope().getArea();
            Geometry geometryToReturn = first;

            for (int t = 0; t < gc.getNumGeometries(); t++) {
                Geometry curr = gc.getGeometryN(t);
                double area = curr.getEnvelope().getArea();
                if (area > maxAreaSoFar) {
                    maxAreaSoFar = area;
                    geometryToReturn = curr;
                }
            }

            return geometryToReturn;
        }
    }

}

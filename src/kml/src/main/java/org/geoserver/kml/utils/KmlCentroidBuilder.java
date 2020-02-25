/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.geometry.jts.GeometryClipper;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.linearref.LengthIndexedLine;

/** A KML specific geometry centroid extractor */
public class KmlCentroidBuilder {

    static Logger LOG = Logging.getLogger(KmlCentroidBuilder.class);

    /**
     * Returns the centroid of the geometry, handling a geometry collection.
     *
     * <p>In the case of a collection a multi point containing the centroid of each geometry in the
     * collection is calculated. The first point in the multi point is returned as the controid.
     */
    public Coordinate geometryCentroid(Geometry g) {
        return geometryCentroid(g, null, null);
    }

    /**
     * Returns the centroid of the geometry, handling a geometry collection.
     *
     * <p>In the case of a collection a multi point containing the centroid of each geometry in the
     * collection is calculated. The first point in the multi point is returned as the controid.
     *
     * <p>The <tt>opts</tt> parameter is used to provide additional options controlling how the
     * centroid is computed.
     *
     * @param g The geometry to compute the centroid.
     * @param bbox The request bbox, used to potentially clip the geometry before computting the
     *     centroid.
     * @param opts The centroid options controlling whether clipping/sampling/etc... are used.
     */
    public Coordinate geometryCentroid(Geometry g, Envelope bbox, KmlCentroidOptions opts) {
        if (opts == null) {
            opts = KmlCentroidOptions.DEFAULT;
        }

        // clip?
        if (opts.isClip()) {
            if (bbox != null) {
                g = clipGeometry(g, bbox);
            } else {
                LOG.warning("Clip option specified for kml centroids, but no bbox available");
            }
        }

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
        } else if (g instanceof Polygon) {
            if (opts.isContain()) {
                try {
                    Point p =
                            RendererUtilities.sampleForInternalPoint(
                                    (Polygon) g, null, null, null, -1, opts.getSamples());
                    if (p != null && !p.isEmpty()) {
                        return p.getCoordinate();
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Unable to calculate central point for polygon", e);
                }
            }
        }

        // return the actual centroid
        return g.getCentroid().getCoordinate();
    }

    /** Selects a representative geometry from the collection (the one covering the biggest area) */
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

    private Geometry clipGeometry(Geometry g, Envelope bbox) {
        return new GeometryClipper(bbox).clipSafe(g, true, 0);
    }
}

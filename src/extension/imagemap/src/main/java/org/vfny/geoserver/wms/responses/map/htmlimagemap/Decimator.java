/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * (c) 2004-2008 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * A modified version of Decimator from Geotools renderer.lite.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.geotools.geometry.jts.LiteCoordinateSequence;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Accepts geometries and collapses all the vertices that will be rendered to the same pixel.
 *
 * @author jeichar
 */
public final class Decimator {

    private double spanx = -1;

    private double spany = -1;

    /**
     * djb - noticed that the old way of finding out the decimation is based on the (0,0) location
     * of the image. This is often wildly unrepresentitive of the scale of the entire map.
     *
     * <p>A better thing to do is to decimate this on a per-shape basis (and use the shape's
     * center). Another option would be to sample the image at different locations (say 9) and
     * choose the smallest spanx/spany you find.
     *
     * <p>Also, if the xform is an affine Xform, you can be a bit more aggressive in the decimation.
     * If its not an affine xform (ie. its actually doing a CRS xform), you may find this is a bit
     * too aggressive due to any number of mathematical issues.
     *
     * <p>This is just a simple method that uses the centre of the given rectangle instead of (0,0).
     *
     * <p>NOTE: this could need more work based on CRS, but the rectangle is in pixels so it should
     * be fairly immune to all but crazy projections.
     */
    public Decimator(MathTransform screenToWorld, Rectangle paintArea) {
        if (screenToWorld != null) {
            double[] original =
                    new double[] {
                        paintArea.x + paintArea.width / 2.0,
                        paintArea.y + paintArea.height / 2.0,
                        paintArea.x + paintArea.width / 2.0 + 1,
                        paintArea.y + paintArea.height / 2.0 + 1,
                    };
            double[] coords = new double[4];
            try {
                screenToWorld.transform(original, 0, coords, 0, 2);
            } catch (TransformException e) {
                return;
            }
            this.spanx = Math.abs(coords[0] - coords[2]) * 0.8;
            // 0.8 is just so you dont decimate "too much". magic
            // number.
            this.spany = Math.abs(coords[1] - coords[3]) * 0.8;
        } else {
            this.spanx = 1;
            this.spany = 1;
        }
    }

    public final void decimateTransformGeneralize(Geometry geometry, MathTransform transform)
            throws TransformException {
        if (geometry instanceof GeometryCollection) {
            GeometryCollection collection = (GeometryCollection) geometry;
            final int length = collection.getNumGeometries();
            for (int i = 0; i < length; i++) {
                decimateTransformGeneralize(collection.getGeometryN(i), transform);
            }
        } else if (geometry instanceof Point) {
            LiteCoordinateSequence seq =
                    (LiteCoordinateSequence) ((Point) geometry).getCoordinateSequence();
            decimateTransformGeneralize(seq, transform);
        } else if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            decimateTransformGeneralize(polygon.getExteriorRing(), transform);
            final int length = polygon.getNumInteriorRing();
            for (int i = 0; i < length; i++) {
                decimateTransformGeneralize(polygon.getInteriorRingN(i), transform);
            }
        } else if (geometry instanceof LineString) {
            LiteCoordinateSequence seq =
                    (LiteCoordinateSequence) ((LineString) geometry).getCoordinateSequence();
            decimateTransformGeneralize(seq, transform);
        }
    }

    private void forceClosed(Coordinate[] coords) {
        if (!coords[0].equals2D(coords[coords.length - 1])) coords[coords.length - 1] = coords[0];
    }

    /** decimates JTS geometries. */
    public final Geometry decimate(Geometry geom) {
        GeometryFactory gFac = new GeometryFactory(geom.getPrecisionModel(), geom.getSRID());
        if (spanx == -1) return geom;
        if (geom instanceof MultiPoint) {
            // TODO check geometry and if its bbox is too small turn it into a 1
            // point geom
            return geom;
        }
        if (geom instanceof GeometryCollection) {
            // TODO check geometry and if its bbox is too small turn it into a
            // 1-2 point geom
            // takes a bit of work because the geometry will need to be
            // recreated.
            GeometryCollection collection = (GeometryCollection) geom;
            Geometry[] result = new Geometry[collection.getDimension()];
            final int numGeometries = collection.getNumGeometries();
            for (int i = 0; i < numGeometries; i++) {
                result[i] = decimate(collection.getGeometryN(i));
            }
            return gFac.createGeometryCollection(result);

        } else if (geom instanceof LineString) {
            LineString line = (LineString) geom;
            CoordinateSequence seq = (CoordinateSequence) line.getCoordinateSequence();
            LiteCoordinateSequence lseq = new LiteCoordinateSequence(seq.toCoordinateArray());

            if (decimateOnEnvelope(line, lseq)) {
                if (lseq.size() >= 2) return gFac.createLineString(lseq);
            }
            if (lseq.size() >= 2) return gFac.createLineString(decimate(lseq));
            return null;
        } else if (geom instanceof Polygon) {
            Polygon line = (Polygon) geom;
            Coordinate[] exterior = decimate(line.getExteriorRing()).getCoordinates();
            forceClosed(exterior);
            if (exterior.length > 3) {
                LinearRing ring = gFac.createLinearRing(exterior);

                final int numRings = line.getNumInteriorRing();
                List<LinearRing> rings = new ArrayList<LinearRing>();

                for (int i = 0; i < numRings; i++) {
                    Coordinate[] interior = decimate(line.getInteriorRingN(i)).getCoordinates();
                    forceClosed(interior);
                    if (interior.length > 3) rings.add(gFac.createLinearRing(interior));
                }
                return gFac.createPolygon(ring, rings.toArray(new LinearRing[] {}));
            }
            return null;
        }
        return geom;
    }

    /** */
    private boolean decimateOnEnvelope(Geometry geom, LiteCoordinateSequence seq) {
        Envelope env = geom.getEnvelopeInternal();
        if (env.getWidth() <= spanx && env.getHeight() <= spany) {
            double[] coords = seq.getArray();
            int dim = seq.getDimension();
            double[] newcoords = new double[dim * 2];
            for (int i = 0; i < dim; i++) {
                newcoords[i] = coords[i];
                newcoords[dim + i] = coords[coords.length - dim + i];
            }
            seq.setArray(coords);
            return true;
        }
        return false;
    }

    /**
     * 1. remove any points that are within the spanx,spany. We ALWAYS keep 1st and last point 2.
     * transform to screen coordinates 3. remove any points that are close (span <1)
     */
    private final void decimateTransformGeneralize(
            LiteCoordinateSequence seq, MathTransform transform) throws TransformException {
        // decimates before XFORM
        int ncoords = seq.size();
        double originalOrds[] = seq.getXYArray(); // 2*#of points

        if (ncoords < 2) {
            if (ncoords == 1) // 1 coordinate -- just xform it
            {
                double[] newCoordsXformed2 = new double[2];
                transform.transform(originalOrds, 0, newCoordsXformed2, 0, 1);
                seq.setArray(newCoordsXformed2);
                return;
            } else return; // ncoords =0
        }

        // unfortunately, we have to keep things in double precion until after
        // the transform or we could move things.
        double[] allCoords = new double[ncoords * 2]; // preallocate -- might
        // not be full (throw
        // away Z)

        allCoords[0] = originalOrds[0]; // allways have 1st one
        allCoords[1] = originalOrds[1];

        int actualCoords = 1;
        double lastX = allCoords[0];
        double lastY = allCoords[1];
        for (int t = 1; t < (ncoords - 1); t++) {
            // see if this one should be added
            double x = originalOrds[t * 2];
            double y = originalOrds[t * 2 + 1];
            if ((Math.abs(x - lastX) > spanx) || (Math.abs(y - lastY)) > spany) {
                allCoords[actualCoords * 2] = x;
                allCoords[actualCoords * 2 + 1] = y;
                lastX = x;
                lastY = y;
                actualCoords++;
            }
        }
        allCoords[actualCoords * 2] = originalOrds[(ncoords - 1) * 2];
        // always have last one
        allCoords[actualCoords * 2 + 1] = originalOrds[(ncoords - 1) * 2 + 1];
        actualCoords++;

        double[] newCoordsXformed;
        // DO THE XFORM
        if ((transform == null) || (transform.isIdentity())) // no actual
        // xform
        {
            newCoordsXformed = allCoords;
        } else {
            newCoordsXformed = new double[actualCoords * 2];
            transform.transform(allCoords, 0, newCoordsXformed, 0, actualCoords);
        }

        // GENERALIZE -- we should be in screen space so spanx=spany=1.0

        // unfortunately, we have to keep things in double precion until after
        // the transform or we could move things.
        double[] finalCoords = new double[ncoords * 2]; // preallocate -- might
        // not be full (throw
        // away Z)

        finalCoords[0] = newCoordsXformed[0]; // allways have 1st one
        finalCoords[1] = newCoordsXformed[1];

        int actualCoordsGen = 1;
        lastX = newCoordsXformed[0];
        lastY = newCoordsXformed[1];

        for (int t = 1; t < (actualCoords - 1); t++) {
            // see if this one should be added
            double x = newCoordsXformed[t * 2];
            double y = newCoordsXformed[t * 2 + 1];
            if ((Math.abs(x - lastX) > 0.75) || (Math.abs(y - lastY)) > 0.75) // 0.75
            // instead of 1 just because it tends to look nicer for slightly
            // more work. magic number.
            {
                finalCoords[actualCoordsGen * 2] = x;
                finalCoords[actualCoordsGen * 2 + 1] = y;
                lastX = x;
                lastY = y;
                actualCoordsGen++;
            }
        }
        finalCoords[actualCoordsGen * 2] = newCoordsXformed[(actualCoords - 1) * 2];
        // always have last one
        finalCoords[actualCoordsGen * 2 + 1] = newCoordsXformed[(actualCoords - 1) * 2 + 1];
        actualCoordsGen++;

        // stick back in
        double[] seqDouble = new double[2 * actualCoordsGen];
        System.arraycopy(finalCoords, 0, seqDouble, 0, actualCoordsGen * 2);
        seq.setArray(seqDouble);
    }

    private CoordinateSequence decimate(LiteCoordinateSequence seq) {
        double[] coords = seq.getArray();
        int numDoubles = coords.length;
        int dim = seq.getDimension();
        int readDoubles = 0;
        double prevx, currx, prevy, curry, diffx, diffy;
        for (int currentDoubles = 0; currentDoubles < numDoubles; currentDoubles += dim) {
            if (currentDoubles >= dim && currentDoubles < numDoubles - 1) {
                prevx = coords[readDoubles - dim];
                currx = coords[currentDoubles];
                diffx = Math.abs(prevx - currx);
                prevy = coords[readDoubles - dim + 1];
                curry = coords[currentDoubles + 1];
                diffy = Math.abs(prevy - curry);
                if (diffx > spanx || diffy > spany) {
                    readDoubles = copyCoordinate(coords, dim, readDoubles, currentDoubles);
                }
            } else {
                readDoubles = copyCoordinate(coords, dim, readDoubles, currentDoubles);
            }
        }
        double[] newCoords = new double[readDoubles];
        System.arraycopy(coords, 0, newCoords, 0, readDoubles);
        seq.setArray(newCoords);
        return seq;
    }

    /** */
    private int copyCoordinate(
            double[] coords, int dimension, int readDoubles, int currentDoubles) {
        for (int i = 0; i < dimension; i++) {
            coords[readDoubles + i] = coords[currentDoubles + i];
        }
        readDoubles += dimension;
        return readDoubles;
    }
}

/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import net.sf.json.JSONException;
import org.geoserver.wfs.json.RoundingUtil;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;

/** This class write geometric types to Json using a JsonGenerator */
public class GeometryHandler {

    private CRS.AxisOrder axisOrder = CRS.AxisOrder.NORTH_EAST;

    private int numDecimals = 6;

    private boolean encodedMeasures = false;

    public GeometryHandler() {}

    public GeometryHandler(CRS.AxisOrder axisOrder, int numDecimals, boolean encodedMeasures) {
        this.axisOrder = axisOrder;
        this.numDecimals = numDecimals;
        this.encodedMeasures = encodedMeasures;
    }

    public void writeGeometry(JsonGenerator generator, Geometry geom) throws IOException {
        if (geom instanceof Point) {
            writePoint(generator, geom);
        } else if (geom instanceof LineString) {
            writeLinestring(generator, geom);
        } else if (geom instanceof Polygon) {
            writePolygon(generator, geom);
        } else if (geom instanceof MultiPoint) {
            writeMultiPoint(generator, geom);
        } else if (geom instanceof MultiLineString) {
            writeMultiLineString(generator, geom);
        } else if (geom instanceof MultiPolygon) {
            writeMultiPolygon(generator, geom);
        } else if (geom instanceof GeometryCollection) {
            GeometryCollection coll = (GeometryCollection) geom;
            for (int i = 0; i < coll.getNumGeometries(); i++) {
                writeGeometry(generator, coll.getGeometryN(i));
            }
        }
    }

    public void writePoint(JsonGenerator generator, Geometry point) throws IOException {
        writeCoordinates(generator, (Point) point);
    }

    public void writeLinestring(JsonGenerator generator, Geometry linestring) throws IOException {
        writeCoordinates(generator, ((LineString) linestring).getCoordinateSequence());
    }

    public void writePolygon(JsonGenerator generator, Geometry polygon) throws IOException {
        writePolygonCoordinates(generator, (Polygon) polygon);
    }

    public void writeMultiPoint(JsonGenerator generator, Geometry multiPoint) throws IOException {
        MultiPoint points = (MultiPoint) multiPoint;
        generator.writeStartArray();
        for (int i = 0, n = points.getNumGeometries(); i < n; i++) {
            writeCoordinates(generator, ((Point) points.getGeometryN(i)));
        }
        generator.writeEndArray();
    }

    public void writeMultiLineString(JsonGenerator generator, Geometry multiLineString)
            throws IOException {
        org.locationtech.jts.geom.MultiLineString lines = (MultiLineString) multiLineString;
        generator.writeStartArray();
        for (int i = 0, n = lines.getNumGeometries(); i < n; i++) {
            LineString lineString = (org.locationtech.jts.geom.LineString) lines.getGeometryN(i);
            writeCoordinates(generator, lineString.getCoordinateSequence());
        }
        generator.writeEndArray();
    }

    public void writeMultiPolygon(JsonGenerator generator, Geometry multiPolygon)
            throws IOException {
        MultiPolygon polygons = (MultiPolygon) multiPolygon;
        generator.writeStartArray();
        for (int i = 0, n = polygons.getNumGeometries(); i < n; i++) {
            writePolygonCoordinates(generator, (Polygon) polygons.getGeometryN(i));
        }
        generator.writeEndArray();
    }

    private void writePolygonCoordinates(
            JsonGenerator generator, org.locationtech.jts.geom.Polygon geometry)
            throws JSONException, IOException {
        generator.writeStartArray();
        writeCoordinates(generator, geometry.getExteriorRing().getCoordinateSequence());

        for (int i = 0, ii = geometry.getNumInteriorRing(); i < ii; i++) {
            writeCoordinates(generator, geometry.getInteriorRingN(i).getCoordinateSequence());
        }

        generator.writeEndArray();
    }

    private void writeCoordinates(JsonGenerator generator, Point point) throws IOException {
        CoordinateSequence coordinates = point.getCoordinateSequence();
        // let's see if we need to encode measures, NaN values will not be encoded
        double m = encodedMeasures ? coordinates.getM(0) : Double.NaN;
        writeCoordinate(
                generator, coordinates.getX(0), coordinates.getY(0), coordinates.getZ(0), m);
    }

    private void writeCoordinates(JsonGenerator generator, CoordinateSequence coordinates)
            throws IOException {
        generator.writeStartArray();
        // each coordinate will be encoded has an array of ordinates
        for (int i = 0; i < coordinates.size(); i++) {
            // let's see if we need to encode measures, NaN values will not be encoded
            double m = encodedMeasures ? coordinates.getM(i) : Double.NaN;
            ;
            // encode the coordinate ordinates to the JSON output
            writeCoordinate(
                    generator, coordinates.getX(i), coordinates.getY(i), coordinates.getZ(i), m);
        }
        generator.writeEndArray();
    }

    private void writeCoordinate(JsonGenerator generator, double x, double y, double z, double m)
            throws IOException {
        generator.writeStartArray();
        // adjust the order of X and Y ordinates if needed
        if (axisOrder == CRS.AxisOrder.NORTH_EAST) {
            // encode latitude first and then longitude
            if (!Double.isNaN(y)) { // for 1d linear referencing cases
                writeRounded(generator, y);
            }
            writeRounded(generator, x);
        } else {
            // encode longitude first and then latitude
            writeRounded(generator, x);
            if (!Double.isNaN(y)) { // for 1d linear referencing cases
                writeRounded(generator, y);
            }
        }
        // if Z value is not available but we have a measure, we set Z value to zero
        z = Double.isNaN(z) && !Double.isNaN(m) ? 0 : z;
        // encode Z value if available
        if (!Double.isNaN(z)) {
            writeRounded(generator, z);
        }
        // encode M value if available
        if (!Double.isNaN(m)) {
            writeRounded(generator, m);
        }
        generator.writeEndArray();
    }

    private void writeRounded(JsonGenerator generator, double value) throws IOException {
        generator.writeNumber(RoundingUtil.round(value, numDecimals));
    }
}
